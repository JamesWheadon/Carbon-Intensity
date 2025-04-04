package com.intensity.octopus

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.min

class FakeOctopus : HttpHandler {
    private val products = mutableSetOf<String>()
    private val errorProducts = mutableSetOf<String>()
    private val tariffCodePrices = mutableMapOf<Pair<String, ZonedDateTime>, List<Double>>()
    private var fail = false

    val routes = routes(
        "/" bind GET to {
            Response(OK).body(
                """
                    {
                        "count":${products.size},
                        "next":null,
                        "previous":null,
                        "results":${
                    products.joinToString(",", "[", "]") {
                        """{
                        "code":"$it",
                        "display_name":"Agile Octopus",
                        "brand":"OCTOPUS_ENERGY"
                    }"""
                    }
                }
                    }""".trimIndent()
            )
        },
        "/{productCode}" bind GET to { request ->
            val productCode = request.path("productCode")
            if (products.contains(productCode) && !errorProducts.contains(productCode)) {
                Response(OK).body(
                    """
                        {
                            "single_register_electricity_tariffs": {
                                "_A": {
                                    "direct_debit_monthly": {
                                        "code": "E-1R-$productCode-A"
                                    }
                                }
                            }
                        }
                    """.trimIndent()
                )
            } else {
                Response(NOT_FOUND).body("""{"detail":"No EnergyProduct matches the given query."}""")
            }
        },
        "/{productCode}/electricity-tariffs/{tariffCode}/standard-unit-rates" bind GET to { request ->
            val productCode = request.path("productCode")!!
            val tariffCode = request.path("tariffCode")!!
            if (products.contains(productCode) && tariffCodePrices.keys.any { it.first == tariffCode }) {
                val errors = mutableListOf<String>()
                var periodFrom = try {
                    parseTimestamp(request.query("period_from")!!)
                } catch (e: DateTimeParseException) {
                    errors.add(""""period_from":["Enter a valid date/time."]""")
                    ZonedDateTime.now()
                }
                var periodTo = try {
                    parseTimestamp(request.query("period_to")!!)
                } catch (e: DateTimeParseException) {
                    errors.add(""""period_to":["Enter a valid date/time."]""")
                    ZonedDateTime.now()
                }
                if (errors.isNotEmpty()) {
                    Response(BAD_REQUEST).body(errors.joinToString(",", "{", "}"))
                } else if (periodTo.isBefore(periodFrom)) {
                    Response(BAD_REQUEST).body("""{"period_from":["Must not be greater than `period_to`."]}""")
                } else {
                    periodFrom = periodFrom.truncatedTo(ChronoUnit.MINUTES).minusMinutes(periodFrom.minute % 30L)
                    periodTo = periodTo.truncatedTo(ChronoUnit.MINUTES).plusMinutes(30 - periodTo.minute % 30L)
                    val halfHourPrices = mutableListOf<String>()
                    val halfHourIntervals = ((periodTo.toEpochSecond() - periodFrom.toEpochSecond()) / 1800).toInt()
                    val providedPriceData = tariffCodePrices[tariffCode to periodFrom]!!
                    val count = min(halfHourIntervals, providedPriceData.size)
                    for (i in 0 until count) {
                        halfHourPrices.add(
                            """{
                                "value_exc_vat":${providedPriceData[count - 1 - i]},
                                "value_inc_vat":${providedPriceData[count - 1 - i] * 1.05},
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
            } else if (products.contains(productCode)) {
                Response(NOT_FOUND).body("""{"detail":"No ElectricityTariff matches the given query."}""")
            } else {
                Response(NOT_FOUND).body("""{"detail":"No EnergyProduct matches the given query."}""")
            }
        }
    )

    private fun parseTimestamp(timestamp: String) = ZonedDateTime.parse(timestamp)

    fun setPricesFor(productCode: String, tariffCodeAtTime: Pair<String, ZonedDateTime>, prices: List<Double>) {
        products.add(productCode)
        tariffCodePrices[tariffCodeAtTime] = prices
    }

    fun fail() {
        fail = true
    }

    fun incorrectOctopusProductCode(productCode: String) {
        products.add(productCode)
        errorProducts.add(productCode)
    }

    override fun invoke(request: Request): Response {
        return when (fail) {
            true -> Response(INTERNAL_SERVER_ERROR)
            false -> routes(request)
        }
    }
}
