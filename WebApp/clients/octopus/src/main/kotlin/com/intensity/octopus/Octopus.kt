package com.intensity.octopus

import com.fasterxml.jackson.annotation.JsonProperty
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
                Method.GET,
                "/$productCode/electricity-tariffs/$tariffCode/standard-unit-rates/?period_from=$periodFrom&period_to=$periodTo"
            )
        )
        return when (response.status) {
            Status.OK -> Success(pricesLens(response))
            Status.BAD_REQUEST -> Failure("Invalid request")
            Status.NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure("Failure communicating with Octopus")
        }
    }

    override fun products(): Result<Products, String> {
        val response = httpHandler(Request(Method.GET, "/"))
        return when (response.status) {
            Status.OK -> Success(productsLens(response))
            else -> Failure("Failure communicating with Octopus")
        }
    }

    override fun product(productCode: String): Result<ProductDetails, String> {
        val response = httpHandler(
            Request(Method.GET, "/$productCode/")
        )
        return when (response.status) {
            Status.OK -> Success(productDetailsLens(response))
            Status.NOT_FOUND -> octopusErrorLens(response).toFailure()
            else -> Failure("Failure communicating with Octopus")
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
            "No ElectricityTariff matches the given query." -> "Incorrect Octopus tariff code"
            else -> "Incorrect Octopus product code"
        }
    )
}

val pricesLens = Jackson.autoBody<Prices>().toLens()
val productsLens = Jackson.autoBody<Products>().toLens()
val productDetailsLens = Jackson.autoBody<ProductDetails>().toLens()
val octopusErrorLens = Jackson.autoBody<OctopusError>().toLens()
