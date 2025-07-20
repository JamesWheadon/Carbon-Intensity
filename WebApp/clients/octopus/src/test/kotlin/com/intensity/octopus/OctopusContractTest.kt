package com.intensity.octopus

import com.intensity.coretest.containsEntries
import com.intensity.coretest.isSuccess
import com.intensity.observability.Observability
import com.intensity.observability.TestObservability
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.valueOrNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

abstract class OctopusContractTest {
    abstract val octopus: Octopus

    @Test
    fun `can get electricity prices`() {
        val prices = octopus.prices(
            OctopusProduct("AGILE-FLEX-22-11-25"),
            OctopusTariff("E-1R-AGILE-FLEX-22-11-25-C"),
            ZonedDateTime.of(2023, 3, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 26, 1, 29, 0, 0, ZoneId.of("UTC"))
        ).valueOrNull()!!

        assertThat(prices.results.map(PriceData::wholesalePrice), equalTo(listOf(23.4, 26.0, 24.3)))
        assertThat(
            prices.results.first().from,
            equalTo(ZonedDateTime.of(2023, 3, 26, 1, 0, 0, 0, ZoneId.ofOffset("", ZoneOffset.UTC)))
        )
    }

    @Test
    fun `can get electricity prices for a certain tariff`() {
        val prices = octopus.prices(
            OctopusProduct("AGILE-FLEX-22-11-25"),
            OctopusTariff("E-1R-AGILE-FLEX-22-11-25-B"),
            ZonedDateTime.of(2023, 3, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 26, 1, 29, 0, 0, ZoneId.of("UTC"))
        ).valueOrNull()!!

        assertThat(prices.results.map(PriceData::wholesalePrice), equalTo(listOf(23.4, 26.0, 24.3)))
    }

    @Test
    fun `can get electricity prices at a certain time`() {
        val prices = octopus.prices(
            OctopusProduct("AGILE-FLEX-22-11-25"),
            OctopusTariff("E-1R-AGILE-FLEX-22-11-25-C"),
            ZonedDateTime.of(2023, 3, 28, 1, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 28, 4, 59, 0, 0, ZoneId.of("UTC"))
        ).valueOrNull()!!

        assertThat(
            prices.results.map(PriceData::wholesalePrice),
            equalTo(listOf(22.0, 22.16, 18.38, 19.84, 16.6, 19.79, 18.0, 22.2))
        )
        assertThat(
            prices.results.last().from,
            equalTo(ZonedDateTime.of(2023, 3, 28, 1, 0, 0, 0, ZoneId.ofOffset("", ZoneOffset.UTC)))
        )
        assertThat(
            prices.results.first().to,
            equalTo(ZonedDateTime.of(2023, 3, 28, 5, 0, 0, 0, ZoneId.ofOffset("", ZoneOffset.UTC)))
        )
    }

    @Test
    fun `can get the existing products`() {
        val products = octopus.products()

        assertThat(products, isSuccess())
    }

    @Test
    fun `can get product information`() {
        val productDetails = octopus.product(OctopusProduct("AGILE-FLEX-22-11-25")).valueOrNull()!!

        assertThat(productDetails.tariffs["_A"]!!.monthly.code, equalTo(OctopusTariff("E-1R-AGILE-FLEX-22-11-25-A")))
    }

    @Test
    fun `handles no product details existing`() {
        val productDetails = octopus.product(OctopusProduct("AGILE-FLEX"))

        assertThat(
            productDetails,
            equalTo(Failure(IncorrectOctopusProductCode))
        )
    }

    @Test
    fun `handles no product existing`() {
        val prices = octopus.prices(
            OctopusProduct("AGILE-FLEX"),
            OctopusTariff("E-1R-AGILE-FLEX-22-11-25-C"),
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
            OctopusProduct("AGILE-FLEX-22-11-25"),
            OctopusTariff("E-1R-AGILE-FLEX"),
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
                OctopusProduct("AGILE-FLEX-22-11-25"),
                OctopusTariff("E-1R-AGILE-FLEX-22-11-25-C"),
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
            "E-1R-AGILE-FLEX-22-11-25-C" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            listOf(23.4, 26.0, 24.3)
        )
        fake.setPricesFor(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-B" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            listOf(23.4, 26.0, 24.3)
        )
        fake.setPricesFor(
            "AGILE-FLEX-22-11-25", "E-1R-AGILE-FLEX-22-11-25-C" to ZonedDateTime.parse("2023-03-28T01:00:00Z"),
            listOf(22.0, 22.16, 18.38, 19.84, 16.6, 19.79, 18.0, 22.2)
        )
    }
    private val observability = TestObservability()
    override val octopus = OctopusCloud(fakeOctopus, observability.observability("octopus-test"))

    @Test
    fun `handles failure getting products`() {
        fakeOctopus.fail()
        val products = octopus.products()

        assertThat(products, equalTo(Failure(OctopusCommunicationFailed)))
    }

    @Test
    fun `handles failure getting product details`() {
        fakeOctopus.fail()
        val productDetails = octopus.product(OctopusProduct("AGILE"))

        assertThat(productDetails, equalTo(Failure(OctopusCommunicationFailed)))
    }

    @Test
    fun `handles failure getting tariff prices`() {
        fakeOctopus.fail()
        val prices = octopus.prices(
            OctopusProduct("AGILE-FLEX-22-11-25"),
            OctopusTariff("E-1R-AGILE-FLEX-22-11-25-C"),
            ZonedDateTime.of(2023, 3, 28, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 28, 4, 59, 0, 0, ZoneId.of("UTC"))
        )

        assertThat(prices, equalTo(Failure(OctopusCommunicationFailed)))
    }

    @Test
    fun `creates a span with data about the request to get octopus products`() {
        octopus.products()

        assertThat(observability.spans().map { it.name }, equalTo(listOf("Fetch Octopus Products")))
        val fetchSpan = observability.spans().first { it.name == "Fetch Octopus Products" }
        assertThat(
            fetchSpan.attributes,
            containsEntries(
                listOf(
                    "service.name" to "octopus-test",
                    "http.response.status_code" to 200L,
                    "http.path" to "/",
                    "http.target" to "Octopus"
                )
            )
        )
    }

    @Test
    fun `creates a span with data about the request to get an octopus product`() {
        octopus.product(OctopusProduct("AGILE-FLEX-22-11-25"))

        assertThat(observability.spans().map { it.name }, equalTo(listOf("Fetch Octopus Product")))
        val fetchSpan = observability.spans().first { it.name == "Fetch Octopus Product" }
        assertThat(
            fetchSpan.attributes,
            containsEntries(
                listOf(
                    "service.name" to "octopus-test",
                    "http.response.status_code" to 200L,
                    "http.path" to "/AGILE-FLEX-22-11-25/",
                    "http.target" to "Octopus"
                )
            )
        )
    }

    @Test
    fun `creates a span with data about the request to get an octopus tariff prices`() {
        octopus.prices(
            OctopusProduct("AGILE-FLEX-22-11-25"),
            OctopusTariff("E-1R-AGILE-FLEX-22-11-25-C"),
            ZonedDateTime.of(2023, 3, 26, 0, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2023, 3, 26, 1, 29, 0, 0, ZoneId.of("UTC"))
        )

        assertThat(observability.spans().map { it.name }, equalTo(listOf("Fetch Octopus Tariff Prices")))
        val fetchSpan = observability.spans().first { it.name == "Fetch Octopus Tariff Prices" }
        assertThat(
            fetchSpan.attributes,
            containsEntries(
                listOf(
                    "service.name" to "octopus-test",
                    "http.response.status_code" to 200L,
                    "http.path" to "/AGILE-FLEX-22-11-25/electricity-tariffs/E-1R-AGILE-FLEX-22-11-25-C/standard-unit-rates/",
                    "http.target" to "Octopus"
                )
            )
        )
    }
}

@Disabled
class OctopusTest : OctopusContractTest() {
    override val octopus = OctopusCloud(octopusClient(), Observability.noOp())
}
