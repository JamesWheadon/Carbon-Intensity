package com.intensity.octopus

import com.intensity.coretest.isSuccess
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.valueOrNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

abstract class OctopusContractTest {
    abstract val octopus: Octopus

    @Test
    fun `can get electricity prices`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            ZonedDateTime.of(2023, 3, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 26, 1, 29, 0, 0, ZoneId.of("UTC"))
        ).valueOrNull()!!

        assertThat(prices.results.map(HalfHourPrices::wholesalePrice), equalTo(listOf(23.4, 26.0, 24.3)))
        assertThat(prices.results.first().from, equalTo(Instant.parse("2023-03-26T01:00:00Z")))
    }

    @Test
    fun `can get electricity prices for a certain tariff`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-B",
            ZonedDateTime.of(2023, 3, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 26, 1, 29, 0, 0, ZoneId.of("UTC"))
        ).valueOrNull()!!

        assertThat(prices.results.map(HalfHourPrices::wholesalePrice), equalTo(listOf(23.4, 26.0, 24.3)))
    }

    @Test
    fun `can get electricity prices at a certain time`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            ZonedDateTime.of(2023, 3, 28, 1, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 28, 4, 59, 0, 0, ZoneId.of("UTC"))
        ).valueOrNull()!!

        assertThat(
            prices.results.map(HalfHourPrices::wholesalePrice),
            equalTo(listOf(22.0, 22.16, 18.38, 19.84, 16.6, 19.79, 18.0, 22.2))
        )
        assertThat(prices.results.last().from, equalTo(Instant.parse("2023-03-28T01:00:00Z")))
        assertThat(prices.results.first().to, equalTo(Instant.parse("2023-03-28T05:00:00Z")))
    }

    @Test
    fun `can get the existing products`() {
        val products = octopus.products()

        assertThat(products, isSuccess())
    }

    @Test
    fun `can get product information`() {
        val productDetails = octopus.product("AGILE-FLEX-22-11-25").valueOrNull()!!

        assertThat(productDetails.tariffs["_A"]!!.monthly.code, equalTo("E-1R-AGILE-FLEX-22-11-25-A"))
    }

    @Test
    fun `handles no product details existing`() {
        val productDetails = octopus.product("AGILE-FLEX")

        assertThat(
            productDetails,
            equalTo(Failure(IncorrectOctopusProductCode))
        )
    }

    @Test
    fun `handles no product existing`() {
        val prices = octopus.prices(
            "AGILE-FLEX",
            "E-1R-AGILE-FLEX-22-11-25-C",
            ZonedDateTime.of(2023, 3, 28, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 28, 4, 59, 0, 0, ZoneId.of("UTC"))
        )

        assertThat(
            prices,
            equalTo(Failure(IncorrectOctopusProductCode))
        )
    }

    @Test
    fun `handles no tariff existing`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX",
            ZonedDateTime.of(2023, 3, 28, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 28, 4, 59, 0, 0, ZoneId.of("UTC"))
        )

        assertThat(
            prices,
            equalTo(Failure(IncorrectOctopusTariffCode))
        )
    }

    @Test
    fun `handles invalid time periods`() {
        assertThat(
            octopus.prices(
                "AGILE-FLEX-22-11-25",
                "E-1R-AGILE-FLEX-22-11-25-C",
                ZonedDateTime.of(2023, 3, 28, 0, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2023, 3, 27, 4, 59, 0, 0, ZoneId.of("UTC"))
            ),
            equalTo(Failure(InvalidRequestFailed))
        )
    }
}

class FakeOctopusTest : OctopusContractTest() {
    private val fakeOctopus = FakeOctopus().also { fake ->
        fake.setPricesFor(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C" to "2023-03-26T00:00:00Z",
            listOf(23.4, 26.0, 24.3)
        )
        fake.setPricesFor(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-B" to "2023-03-26T00:00:00Z",
            listOf(23.4, 26.0, 24.3)
        )
        fake.setPricesFor(
            "AGILE-FLEX-22-11-25", "E-1R-AGILE-FLEX-22-11-25-C" to "2023-03-28T01:00:00Z",
            listOf(22.0, 22.16, 18.38, 19.84, 16.6, 19.79, 18.0, 22.2)
        )
    }

    override val octopus =
        OctopusCloud(
            fakeOctopus
        )

    @Test
    fun `handles failure getting products`() {
        fakeOctopus.fail()
        val products = octopus.products()

        assertThat(products, equalTo(Failure(OctopusCommunicationFailed)))
    }

    @Test
    fun `handles failure getting product details`() {
        fakeOctopus.fail()
        val productDetails = octopus.product("AGILE")

        assertThat(productDetails, equalTo(Failure(OctopusCommunicationFailed)))
    }

    @Test
    fun `handles failure getting tariff prices`() {
        fakeOctopus.fail()
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            ZonedDateTime.of(2023, 3, 28, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 28, 4, 59, 0, 0, ZoneId.of("UTC"))
        )

        assertThat(prices, equalTo(Failure(OctopusCommunicationFailed)))
    }
}

@Disabled
class OctopusTest : OctopusContractTest() {
    override val octopus = OctopusCloud(octopusClient())
}
