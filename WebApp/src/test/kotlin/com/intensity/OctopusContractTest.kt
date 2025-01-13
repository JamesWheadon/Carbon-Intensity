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
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import kotlin.math.min

abstract class OctopusContractTest {
    abstract val octopus: Octopus

    @Test
    fun `can get electricity prices`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            "2023-03-26T00:00Z",
            "2023-03-26T01:29Z"
        )

        assertThat(prices.results.map(HalfHourPrices::wholesalePrice), equalTo(listOf(23.4, 26.0, 24.3)))
        assertThat(prices.results.first().from, equalTo(Instant.parse("2023-03-26T01:00:00Z")))
    }

    @Test
    fun `can get electricity prices for a certain tariff`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-B",
            "2023-03-26T00:00Z",
            "2023-03-26T01:29Z"
        )

        assertThat(prices.results.map(HalfHourPrices::wholesalePrice), equalTo(listOf(27.2, 26.7, 26.9)))
    }

    @Test
    fun `can get electricity prices at a certain time`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            "2023-03-28T01:00Z",
            "2023-03-28T04:59Z"
        )

        assertThat(
            prices.results.map(HalfHourPrices::wholesalePrice),
            equalTo(listOf(15.9, 17.8, 20.5, 13.6, 15.0, 24.7, 27.1, 13.4))
        )
        assertThat(prices.results.last().from, equalTo(Instant.parse("2023-03-28T01:00:00Z")))
        assertThat(prices.results.first().to, equalTo(Instant.parse("2023-03-28T05:00:00Z")))
    }
}

interface Octopus {
    fun prices(productCode: String, tariffCode: String, periodFrom: String, periodTo: String): Prices
}

class OctopusCloud(val httpHandler: HttpHandler) : Octopus {
    override fun prices(productCode: String, tariffCode: String, periodFrom: String, periodTo: String): Prices {
        return pricesLens(
            httpHandler(
                Request(
                    GET,
                    "/$productCode/electricity-tariffs/$tariffCode/standard-unit-rates/?period_from=$periodFrom&period_to=$periodTo"
                )
            )
        )
    }
}

class FakeOctopusTest : OctopusContractTest() {
    private val fakeOctopus = FakeOctopus().also { fake ->
        fake.setPricesFor("E-1R-AGILE-FLEX-22-11-25-C" to "2023-03-26T00:00:00Z", listOf(23.4, 26.0, 24.3))
        fake.setPricesFor("E-1R-AGILE-FLEX-22-11-25-B" to "2023-03-26T00:00:00Z", listOf(27.2, 26.7, 26.9))
        fake.setPricesFor(
            "E-1R-AGILE-FLEX-22-11-25-C" to "2023-03-28T01:00:00Z",
            listOf(15.9, 17.8, 20.5, 13.6, 15.0, 24.7, 27.1, 13.4)
        )
    }

    override val octopus =
        OctopusCloud(
            fakeOctopus
        )
}

class FakeOctopus : HttpHandler {
    private val tariffCodePrices = mutableMapOf<Pair<String, String>, List<Double>>()

    val routes = routes(
        "/{productCode}/electricity-tariffs/{tariffCode}/standard-unit-rates" bind GET to { request ->
            val tariffCode = request.path("tariffCode")!!

            val periodFrom =
                Instant.ofEpochSecond(
                    parseTimestamp(request.query("period_from")!!).epochSecond / 1800 * 1800
                )
            val periodTo =
                Instant.ofEpochSecond(
                    parseTimestamp(request.query("period_to")!!).epochSecond / 1800 * 1800 + 1800
                )

            val halfHourPrices = mutableListOf<String>()
            val halfHourIntervals = ((periodTo.epochSecond - periodFrom.epochSecond) / 1800).toInt()
            val providedPriceData = tariffCodePrices[tariffCode to periodFrom.toString()]!!.size
            val count = min(halfHourIntervals, providedPriceData)
            for (i in 0 until count) {
                halfHourPrices.add(
                    """{
                            "value_exc_vat":${tariffCodePrices[tariffCode to periodFrom.toString()]!![count - 1 - i]},
                            "value_inc_vat":24.57,
                            "valid_from":"${periodFrom.plusSeconds(i * 1800L)}",
                            "valid_to":"${periodFrom.plusSeconds((i + 1) * 1800L)}",
                            "payment_method":null
                        }"""
                )
            }
            Response(OK).body(
                """
                    {
                        "count":${count},
                        "next":null,
                        "previous":null,
                        "results":
                        ${halfHourPrices.reversed().joinToString(",", "[", "]")}
                    }""".trimIndent()
            )
        }
    )

    private fun parseTimestamp(timestamp: String): Instant =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
            .parse(timestamp) { temporal: TemporalAccessor? -> Instant.from(temporal) }

    fun setPricesFor(tariffCodeAtTime: Pair<String, String>, prices: List<Double>) {
        tariffCodePrices[tariffCodeAtTime] = prices
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
