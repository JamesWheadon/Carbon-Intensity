package com.intensity.central

import com.intensity.core.ErrorResponse
import com.intensity.core.Failed
import com.intensity.core.errorResponseLens
import com.intensity.octopus.Octopus
import com.intensity.octopus.pricesLens
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.fold
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.partition
import dev.forkhandles.result4k.valueOrNull
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import java.time.ZonedDateTime

fun octopusProducts(
    octopus: Octopus
) = "tariffs" bind GET to {
    octopus.products().flatMap { products ->
        val tariffsAndFailures = products.results.map { product ->
            octopus.product(product.code).map {
                OctopusProduct(product.code, it.tariffs.values.map { tariff -> tariff.monthly.code })
            }
        }.partition()
        if (tariffsAndFailures.first.isNotEmpty()) {
            Success(tariffsAndFailures.first)
        } else {
            Failure(NoOctopusProducts)
        }
    }.fold(
        { products ->
            Response(OK).with(octopusProductsLens of OctopusProducts(products))
        },
        { failed ->
            val status = when (failed) {
                is NoOctopusProducts -> NOT_FOUND
                else -> INTERNAL_SERVER_ERROR
            }
            Response(status).with(errorResponseLens of failed.toErrorResponse())
        }
    )
}

fun octopusPrices(
    octopus: Octopus
) = "tariffs/{productCode}/{tariffCode}" bind GET to { request ->
    val prices = octopus.prices(
        request.path("productCode")!!,
        request.path("tariffCode")!!,
        ZonedDateTime.now(),
        request.query("end")?.let { ZonedDateTime.parse(it) } ?: ZonedDateTime.now().plusDays(2)
    )
    Response(OK).with(pricesLens of prices.valueOrNull()!!)
}

val octopusProductsLens = Jackson.autoBody<OctopusProducts>().toLens()

data class OctopusProducts(val products: List<OctopusProduct>)
data class OctopusProduct(val name: String, val tariffs: List<String>)

object NoOctopusProducts : Failed {
    override fun toErrorResponse() = ErrorResponse("No Octopus products")
}
