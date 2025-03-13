package com.intensity.octopus

import com.fasterxml.jackson.annotation.JsonProperty
import com.intensity.core.ErrorResponse
import com.intensity.core.Failed
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.Jackson
import java.time.Instant

interface Octopus {
    fun prices(productCode: String, tariffCode: String, periodFrom: String, periodTo: String): Result<Prices, Failed>
    fun products(): Result<Products, Failed>
    fun product(productCode: String): Result<ProductDetails, Failed>
}

class OctopusCloud(val httpHandler: HttpHandler) : Octopus {
    override fun prices(
        productCode: String,
        tariffCode: String,
        periodFrom: String,
        periodTo: String
    ): Result<Prices, Failed> {
        val response = httpHandler(
            Request(
                Method.GET,
                "/$productCode/electricity-tariffs/$tariffCode/standard-unit-rates/?period_from=$periodFrom&period_to=$periodTo"
            )
        )
        return when (response.status) {
            Status.OK -> Success(pricesLens(response))
            Status.BAD_REQUEST -> Failure(InvalidRequestFailed)
            Status.NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure(OctopusCommunicationFailed)
        }
    }

    override fun products(): Result<Products, Failed> {
        val response = httpHandler(Request(Method.GET, "/"))
        return when (response.status) {
            Status.OK -> Success(productsLens(response))
            else -> Failure(OctopusCommunicationFailed)
        }
    }

    override fun product(productCode: String): Result<ProductDetails, Failed> {
        val response = httpHandler(
            Request(Method.GET, "/$productCode/")
        )
        return when (response.status) {
            Status.OK -> Success(productDetailsLens(response))
            Status.NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure(OctopusCommunicationFailed)
        }
    }
}

fun octopusClient() = ClientFilters.SetBaseUriFrom(Uri.of("https://api.octopus.energy/v1/products"))
    .then(JavaHttpClient())

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
