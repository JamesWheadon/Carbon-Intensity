package com.intensity.octopus

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor
import kotlin.math.min

class FakeOctopus : HttpHandler {
    private val products = mutableSetOf<String>()
    private val tariffCodePrices = mutableMapOf<Pair<String, String>, List<Double>>()
    private var fail = false

    val routes = routes(
        "/" bind Method.GET to {
            Response(Status.OK).body(
                """
                    {
                        "count":1,
                        "next":null,
                        "previous":null,
                        "results":[
                            {
                                "code":"AGILE-24-10-01",
                                "display_name":"Agile Octopus",
                                "brand":"OCTOPUS_ENERGY"
                            }
                        ]
                    }""".trimIndent()
            )
        },
        "/{productCode}" bind Method.GET to { request ->
            val productCode = request.path("productCode")
            if (products.contains(productCode)) {
                Response(Status.OK).body(
                    """
                        {
                            "single_register_electricity_tariffs": {
                                "_A": {
                                    "direct_debit_monthly": {
                                        "code": "E-1R-AGILE-FLEX-22-11-25-A"
                                    }
                                }
                            }
                        }
                    """.trimIndent()
                )
            } else {
                Response(Status.NOT_FOUND).body("""{"detail":"No EnergyProduct matches the given query."}""")
            }
        },
        "/{productCode}/electricity-tariffs/{tariffCode}/standard-unit-rates" bind Method.GET to { request ->
            val productCode = request.path("productCode")!!
            val tariffCode = request.path("tariffCode")!!
            if (products.contains(productCode) && tariffCodePrices.keys.any { it.first == tariffCode }) {
                val errors = mutableListOf<String>()
                val periodFrom = try {
                    Instant.ofEpochSecond(
                        parseTimestamp(request.query("period_from")!!).epochSecond / 1800 * 1800
                    )
                } catch (e: DateTimeParseException) {
                    errors.add(""""period_from":["Enter a valid date/time."]""")
                    Instant.now()
                }
                val periodTo = try {
                    Instant.ofEpochSecond(
                        parseTimestamp(request.query("period_to")!!).epochSecond / 1800 * 1800 + 1800
                    )
                } catch (e: DateTimeParseException) {
                    errors.add(""""period_to":["Enter a valid date/time."]""")
                    Instant.now()
                }
                if (errors.isNotEmpty()) {
                    Response(Status.BAD_REQUEST).body(errors.joinToString(",", "{", "}"))
                } else if (periodTo.isBefore(periodFrom)) {
                    Response(Status.BAD_REQUEST).body("""{"period_from":["Must not be greater than `period_to`."]}""")
                } else {
                    val halfHourPrices = mutableListOf<String>()
                    val halfHourIntervals = ((periodTo.epochSecond - periodFrom.epochSecond) / 1800).toInt()
                    val providedPriceData = tariffCodePrices[tariffCode to periodFrom.toString()]!!.size
                    val count = min(halfHourIntervals, providedPriceData)
                    for (i in 0 until count) {
                        halfHourPrices.add(
                            """{
                                "value_exc_vat":${tariffCodePrices[tariffCode to periodFrom.toString()]!![count - 1 - i]},
                                "value_inc_vat":${tariffCodePrices[tariffCode to periodFrom.toString()]!![count - 1 - i] * 1.05},
                                "valid_from":"${periodFrom.plusSeconds(i * 1800L)}",
                                "valid_to":"${periodFrom.plusSeconds((i + 1) * 1800L)}",
                                "payment_method":null
                            }"""
                        )
                    }
                    Response(Status.OK).body(
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
            } else if (products.contains(productCode)) {
                Response(Status.NOT_FOUND).body("""{"detail":"No ElectricityTariff matches the given query."}""")
            } else {
                Response(Status.NOT_FOUND).body("""{"detail":"No EnergyProduct matches the given query."}""")
            }
        }
    )

    private fun parseTimestamp(timestamp: String): Instant =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
            .parse(timestamp) { temporal: TemporalAccessor? -> Instant.from(temporal) }

    fun setPricesFor(productCode: String, tariffCodeAtTime: Pair<String, String>, prices: List<Double>) {
        products.add(productCode)
        tariffCodePrices[tariffCodeAtTime] = prices
    }

    fun fail() {
        fail = true
    }

    override fun invoke(request: Request): Response {
        return when (fail) {
            true -> Response(Status.INTERNAL_SERVER_ERROR)
            false -> routes(request)
        }
    }
}
