package com.intensity

import com.fasterxml.jackson.annotation.JsonProperty
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
        ).valueOrNull()!!

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
        ).valueOrNull()!!

        assertThat(prices.results.map(HalfHourPrices::wholesalePrice), equalTo(listOf(23.4, 26.0, 24.3)))
    }

    @Test
    fun `can get electricity prices at a certain time`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            "2023-03-28T01:00Z",
            "2023-03-28T04:59Z"
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
            equalTo(Failure("Incorrect Octopus product code"))
        )
    }

    @Test
    fun `handles no product existing`() {
        val prices = octopus.prices(
            "AGILE-FLEX",
            "E-1R-AGILE-FLEX-22-11-25-C",
            "2023-03-28T01:00Z",
            "2023-03-28T04:59Z"
        )

        assertThat(
            prices,
            equalTo(Failure("Incorrect Octopus product code"))
        )
    }

    @Test
    fun `handles no tariff existing`() {
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX",
            "2023-03-28T01:00Z",
            "2023-03-28T04:59Z"
        )

        assertThat(
            prices,
            equalTo(Failure("Incorrect Octopus tariff code"))
        )
    }

    @Test
    fun `handles invalid time periods`() {
        assertThat(
            octopus.prices(
                "AGILE-FLEX-22-11-25",
                "E-1R-AGILE-FLEX-22-11-25-C",
                "2023-03-28T01:00Z",
                "2023-03-27T04:59Z"
            ),
            equalTo(Failure("Invalid request"))
        )
        assertThat(
            octopus.prices(
                "AGILE-FLEX-22-11-25",
                "E-1R-AGILE-FLEX-22-11-25-C",
                "2023-03-28T01:00Z",
                "2023-03-27T04:59Z"
            ),
            equalTo(Failure("Invalid request"))
        )
        assertThat(
            octopus.prices(
                "AGILE-FLEX-22-11-25",
                "E-1R-AGILE-FLEX-22-11-25-C",
                "2023-03-28T01:00Z",
                "2023-03-27T0459Z"
            ),
            equalTo(Failure("Invalid request"))
        )
    }
}

interface Octopus {
    fun prices(productCode: String, tariffCode: String, periodFrom: String, periodTo: String): Result<Prices, String>
    fun products(): Result<Products, String>
    fun product(productCode: String): Result<ProductDetails, String>
}

class OctopusCloud(val httpHandler: HttpHandler) : Octopus {
    override fun prices(
        productCode: String,
        tariffCode: String,
        periodFrom: String,
        periodTo: String
    ): Result<Prices, String> {
        val response = httpHandler(
            Request(
                GET,
                "/$productCode/electricity-tariffs/$tariffCode/standard-unit-rates/?period_from=$periodFrom&period_to=$periodTo"
            )
        )
        return when (response.status) {
            OK -> Success(pricesLens(response))
            BAD_REQUEST -> Failure("Invalid request")
            NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure("Failure communicating with Octopus")
        }
    }

    override fun products(): Result<Products, String> {
        val response = httpHandler(Request(GET, "/"))
        return when (response.status) {
            OK -> Success(productsLens(response))
            else -> Failure("Failure communicating with Octopus")
        }
    }

    override fun product(productCode: String): Result<ProductDetails, String> {
        val response = httpHandler(
            Request(GET, "/$productCode/")
        )
        return when (response.status) {
            OK -> Success(productDetailsLens(response))
            NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure("Failure communicating with Octopus")
        }
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

        assertThat(products, equalTo(Failure("Failure communicating with Octopus")))
    }

    @Test
    fun `handles failure getting product details`() {
        fakeOctopus.fail()
        val productDetails = octopus.product("AGILE")

        assertThat(productDetails, equalTo(Failure("Failure communicating with Octopus")))
    }

    @Test
    fun `handles failure getting tariff prices`() {
        fakeOctopus.fail()
        val prices = octopus.prices(
            "AGILE-FLEX-22-11-25",
            "E-1R-AGILE-FLEX-22-11-25-C",
            "2023-03-28T01:00Z",
            "2023-03-28T04:59Z"
        )

        assertThat(prices, equalTo(Failure("Failure communicating with Octopus")))
    }
}

@Disabled
class OctopusTest : OctopusContractTest() {
    override val octopus = OctopusCloud(octopusClient())
}

fun octopusClient() = ClientFilters.SetBaseUriFrom(Uri.of("https://api.octopus.energy/v1/products"))
    .then(JavaHttpClient())

class FakeOctopus : HttpHandler {
    private val products = mutableSetOf<String>()
    private val tariffCodePrices = mutableMapOf<Pair<String, String>, List<Double>>()
    private var fail = false

    val routes = routes(
        "/" bind GET to {
            Response(OK).body(
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
        "/{productCode}" bind GET to { request ->
            val productCode = request.path("productCode")
            if (products.contains(productCode)) {
                Response(OK).body(
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
                Response(NOT_FOUND).body("""{"detail":"No EnergyProduct matches the given query."}""")
            }
        },
        "/{productCode}/electricity-tariffs/{tariffCode}/standard-unit-rates" bind GET to { request ->
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
                    Response(BAD_REQUEST).body(errors.joinToString(",", "{", "}"))
                } else if (periodTo.isBefore(periodFrom)) {
                    Response(BAD_REQUEST).body("""{"period_from":["Must not be greater than `period_to`."]}""")
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
            true -> Response(INTERNAL_SERVER_ERROR)
            false -> routes(request)
        }
    }
}

data class Prices(val results: List<HalfHourPrices>)
data class HalfHourPrices(
    @JsonProperty("value_exc_vat") val wholesalePrice: Double,
    @JsonProperty("value_inc_vat") val retailPrice: Double,
    @JsonProperty("valid_from") val from: Instant,
    @JsonProperty("valid_to") val to: Instant
)

data class Products(val results: List<Product>)
data class Product(
    val code: String,
    @JsonProperty("display_name") val name: String,
    val brand: String
)

data class ProductDetails(@JsonProperty("single_register_electricity_tariffs") val tariffs: Map<String, TariffDetails>)
data class TariffDetails(@JsonProperty("direct_debit_monthly") val monthly: TariffFees)
data class TariffFees(val code: String)

data class OctopusError(val detail: String) {
    fun toFailure() = Failure(
        when (detail) {
            "No ElectricityTariff matches the given query." -> "Incorrect Octopus tariff code"
            else -> "Incorrect Octopus product code"
        }
    )
}

val pricesLens = Jackson.autoBody<Prices>().toLens()
val productsLens = Jackson.autoBody<Products>().toLens()
val productDetailsLens = Jackson.autoBody<ProductDetails>().toLens()
val octopusErrorLens = Jackson.autoBody<OctopusError>().toLens()
