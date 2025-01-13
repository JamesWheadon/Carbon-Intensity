package com.intensity

import com.fasterxml.jackson.annotation.JsonProperty
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Instant

abstract class OctopusContractTest {
    abstract val octopus: Octopus

    @Test
    fun `can get electricity prices`() {
        val prices = octopus.prices("AGILE-FLEX-22-11-25", "E-1R-AGILE-FLEX-22-11-25-C")

        assertThat(prices.results.first().wholesalePrice, equalTo(23.4))
    }

    @Test
    fun `can get electricity prices for a certain tariff`() {
        val prices = octopus.prices("AGILE-FLEX-22-11-25", "E-1R-AGILE-FLEX-22-11-25-B")

        assertThat(prices.results.first().wholesalePrice, equalTo(27.2))
    }
}

interface Octopus {
    fun prices(productCode: String, tariffCode: String): Prices
}

class OctopusCloud(val httpHandler: HttpHandler) : Octopus {
    override fun prices(productCode: String, tariffCode: String): Prices {
        return pricesLens(
            httpHandler(
                Request(
                    GET,
                    "/$productCode/electricity-tariffs/$tariffCode/standard-unit-rates/?period_from=2023-03-26T00:00Z&period_to=2023-03-26T01:29Z"
                )
            )
        )
    }
}

class FakeOctopusTest : OctopusContractTest() {
    private val fakeOctopus = FakeOctopus().also { fake ->
        fake.setPricesForTariffCode("E-1R-AGILE-FLEX-22-11-25-C", listOf(23.4, 26.0, 24.3))
        fake.setPricesForTariffCode("E-1R-AGILE-FLEX-22-11-25-B", listOf(27.2, 26.7, 26.9))
    }

    override val octopus =
        OctopusCloud(
            fakeOctopus
        )
}

class FakeOctopus : HttpHandler {
    private val tariffCodePrices = mutableMapOf<String, List<Double>>()

    val routes = routes(
        "/{productCode}/electricity-tariffs/{tariffCode}/standard-unit-rates" bind GET to { request ->
            val tariffCode = request.path("tariffCode")!!
            Response(OK).body(
                """
                {
                    "count":3,
                    "next":null,
                    "previous":null,
                    "results":
                    [
                        {
                            "value_exc_vat":${tariffCodePrices[tariffCode]?.get(0) ?: 0.0},
                            "value_inc_vat":24.57,
                            "valid_from":"2023-03-26T01:00:00Z",
                            "valid_to":"2023-03-26T01:30:00Z",
                            "payment_method":null
                        },
                        {
                            "value_exc_vat":${tariffCodePrices[tariffCode]?.get(1) ?: 0.0},
                            "value_inc_vat":27.3,
                            "valid_from":"2023-03-26T00:30:00Z",
                            "valid_to":"2023-03-26T01:00:00Z",
                            "payment_method":null
                        },
                        {
                            "value_exc_vat":${tariffCodePrices[tariffCode]?.get(2) ?: 0.0},
                            "value_inc_vat":25.515,
                            "valid_from":"2023-03-26T00:00:00Z",
                            "valid_to":"2023-03-26T00:30:00Z",
                            "payment_method":null
                        }
                    ]
                }""".trimIndent()
            )
        }
    )

    fun setPricesForTariffCode(tariffCode: String, prices: List<Double>) {
        tariffCodePrices[tariffCode] = prices
    }

    override fun invoke(request: Request) = routes(request)
}

data class Prices(val results: List<HalfHourPrices>)
data class HalfHourPrices(
    @JsonProperty("value_exc_vat") val wholesalePrice: Double,
    @JsonProperty("value_inc_vat") val retailPrice: Double,
    @JsonProperty("valid_from") val from: Instant,
    @JsonProperty("valid_to") val to: Instant
)

val pricesLens = Jackson.autoBody<Prices>().toLens()
