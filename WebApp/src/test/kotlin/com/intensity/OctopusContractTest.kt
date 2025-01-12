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
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Instant

abstract class OctopusContractTest {
    abstract val octopus: Octopus

    @Test
    fun `can get electricity prices`() {
        val prices = octopus.prices()

        assertThat(prices.results.size, equalTo(3))
    }
}

interface Octopus {
    fun prices(): Prices
}

class OctopusCloud(val httpHandler: HttpHandler) : Octopus {
    override fun prices(): Prices {
        return pricesLens(
            httpHandler(
                Request(
                    GET,
                    "/AGILE-FLEX-22-11-25/electricity-tariffs/E-1R-AGILE-FLEX-22-11-25-C/standard-unit-rates/?period_from=2023-03-26T00:00Z&period_to=2023-03-26T01:29Z"
                )
            )
        )
    }
}

class FakeOctopusTest : OctopusContractTest() {
    override val octopus =
        OctopusCloud(
            FakeOctopus()
        )
}

class FakeOctopus : HttpHandler {
    val routes = routes(
        "/{tariff}/electricity-tariffs/{tariff}/standard-unit-rates" bind GET to {
            Response(OK).body(
                """
                {
                    "count":3,
                    "next":null,
                    "previous":null,
                    "results":
                    [
                        {
                            "value_exc_vat":23.4,
                            "value_inc_vat":24.57,
                            "valid_from":"2023-03-26T01:00:00Z",
                            "valid_to":"2023-03-26T01:30:00Z",
                            "payment_method":null
                        },
                        {
                            "value_exc_vat":26.0,
                            "value_inc_vat":27.3,
                            "valid_from":"2023-03-26T00:30:00Z",
                            "valid_to":"2023-03-26T01:00:00Z",
                            "payment_method":null
                        },
                        {
                            "value_exc_vat":24.3,
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
