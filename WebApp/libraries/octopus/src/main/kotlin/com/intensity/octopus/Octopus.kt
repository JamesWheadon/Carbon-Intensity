@file:Suppress("MayBeConstant")

package com.intensity.octopus

import com.fasterxml.jackson.annotation.JsonProperty
import com.intensity.core.ErrorResponse
import com.intensity.core.Failed
import com.intensity.observability.Observability
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.Jackson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Octopus(private val httpHandler: HttpHandler, private val observability: Observability) {
    companion object {
        val pathSegment = "octopus"

        fun client() = ClientFilters.SetBaseUriFrom(Uri.of("https://api.octopus.energy/v1/products"))
            .then(JavaHttpClient())
    }

    fun products(): Result<Products, Failed> {
        val response = observability.outboundHttp("Fetch Octopus Products", "Octopus").then(httpHandler)(Request(GET, "_://octopus/"))
        return when (response.status) {
            OK -> Success(productsLens(response))
            else -> Failure(OctopusCommunicationFailed)
        }
    }

    fun product(product: OctopusProduct): Result<ProductDetails, Failed> {
        val response = observability.outboundHttp("Fetch Octopus Product", "Octopus").then(httpHandler)(
            Request(GET, "_://octopus/${product.code}/")
        )
        return when (response.status) {
            OK -> Success(productDetailsLens(response))
            NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure(OctopusCommunicationFailed)
        }
    }

    fun prices(
        product: OctopusProduct,
        tariff: OctopusTariff,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): Result<Prices, Failed> {
        val periodFrom = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        val periodTo = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        val response = observability.outboundHttp("Fetch Octopus Tariff Prices", "Octopus").then(httpHandler)(
            Request(
                GET,
                "_://octopus/${product.code}/electricity-tariffs/${tariff.code}/standard-unit-rates/?period_from=$periodFrom&period_to=$periodTo"
            )
        )
        return when (response.status) {
            OK -> Success(pricesLens(response))
            BAD_REQUEST -> Failure(InvalidRequestFailed)
            NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure(OctopusCommunicationFailed)
        }
    }
}

@JvmInline
value class OctopusProduct(val code: String)

@JvmInline
value class OctopusTariff(val code: String)

data class Prices(val results: List<PriceData>)
data class PriceData(
    @JsonProperty("value_exc_vat") val wholesalePrice: Double,
    @JsonProperty("value_inc_vat") val retailPrice: Double,
    @JsonProperty("valid_from") val from: ZonedDateTime,
    @JsonProperty("valid_to") val to: ZonedDateTime
)

data class Products(val results: List<Product>)
data class Product(
    val code: OctopusProduct
)

data class ProductDetails(@JsonProperty("single_register_electricity_tariffs") val tariffs: Map<String, TariffDetails>)
data class TariffDetails(@JsonProperty("direct_debit_monthly") val monthly: TariffFees)
data class TariffFees(val code: OctopusTariff)
data class OctopusError(val detail: String) {
    fun toFailure() = Failure(
        when (detail) {
            "No ElectricityTariff matches the given query." -> IncorrectOctopusTariffCode
            else -> IncorrectOctopusProductCode
        }
    )
}

val pricesLens = Jackson.autoBody<Prices>().toLens()
val productsLens = Jackson.autoBody<Products>().toLens()
val productDetailsLens = Jackson.autoBody<ProductDetails>().toLens()
val octopusErrorLens = Jackson.autoBody<OctopusError>().toLens()

object OctopusCommunicationFailed : Failed {
    override fun toErrorResponse() = ErrorResponse("Failure communicating with Octopus")
}

object IncorrectOctopusProductCode : Failed {
    override fun toErrorResponse() = ErrorResponse("Incorrect Octopus product code")
}

object IncorrectOctopusTariffCode : Failed {
    override fun toErrorResponse() = ErrorResponse("Incorrect Octopus tariff code")
}

object InvalidRequestFailed : Failed {
    override fun toErrorResponse() = ErrorResponse("Invalid request")
}
