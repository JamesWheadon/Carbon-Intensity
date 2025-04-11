package com.intensity.central

import com.intensity.core.Electricity
import com.intensity.core.ErrorResponse
import com.intensity.core.Failed
import com.intensity.core.errorResponseLens
import com.intensity.octopus.InvalidRequestFailed
import com.intensity.octopus.Octopus
import com.intensity.octopus.OctopusCommunicationFailed
import com.intensity.octopus.OctopusProduct
import com.intensity.octopus.OctopusTariff
import com.intensity.octopus.PriceData
import com.intensity.octopus.Prices
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.fold
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.partition
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.zonedDateTime
import org.http4k.routing.bind
import java.math.BigDecimal
import java.time.ZonedDateTime

fun octopusProducts(
    octopus: Octopus
) = "/tariffs" bind GET to {
    octopus.products().flatMap { products ->
        val tariffsAndFailures = products.results.map { product ->
            octopus.product(product.code).map {
                OctopusProductResponse(product.code, it.tariffs.values.map { tariff -> tariff.monthly.code })
            }
        }.partition()
        if (tariffsAndFailures.first.isNotEmpty()) {
            Success(tariffsAndFailures.first)
        } else {
            Failure(NoOctopusProducts)
        }
    }.fold(
        { products ->
            Response(OK).with(OctopusProducts.lens of OctopusProducts(products))
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
) = "/tariffs/{productCode}/{tariffCode}" bind GET to { request ->
    val start = startTimeLens(request) ?: ZonedDateTime.now()
    val end = endTimeLens(request) ?: start.plusDays(2)
    octopus.prices(
        octopusProductLens(request),
        octopusTariffLens(request),
        start,
        end
    ).fold(
        { prices ->
            Response(OK).with(OctopusPricesResponse.lens of prices.toResponse())
        },
        { failed ->
            val status = when (failed) {
                InvalidRequestFailed -> BAD_REQUEST
                OctopusCommunicationFailed -> INTERNAL_SERVER_ERROR
                else -> NOT_FOUND
            }
            Response(status).with(errorResponseLens of failed.toErrorResponse())
        }
    )
}

fun octopusChargeTimes(
    calculator: Calculator
) = "/octopus/charge-time" bind POST to { request ->
    val calculationData = CalculationData.lens(request)
    calculator.calculate(calculationData).toChargeTimeResponse()
}

private val endTimeLens = Query.zonedDateTime().optional("end")
private val startTimeLens = Query.zonedDateTime().optional("start")
private val octopusProductLens = Path.map { OctopusProduct(it) }.of("productCode")
private val octopusTariffLens = Path.map { OctopusTariff(it) }.of("tariffCode")

private data class OctopusProducts(val products: List<OctopusProductResponse>) {
    companion object {
        val lens = Jackson.autoBody<OctopusProducts>().toLens()
    }
}

private data class OctopusProductResponse(val name: OctopusProduct, val tariffs: List<OctopusTariff>)

private data class OctopusPricesResponse(val prices: List<PricesResponse>) {
    companion object {
        val lens = Jackson.autoBody<OctopusPricesResponse>().toLens()
    }
}

private data class PricesResponse(
    val wholesalePrice: Double,
    val retailPrice: Double,
    val from: ZonedDateTime,
    val to: ZonedDateTime
)

data class CalculationData(
    val product: OctopusProduct,
    val tariff: OctopusTariff,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val time: Long,
    val intensityLimit: BigDecimal? = null,
    val priceLimit: BigDecimal? = null,
    val weights: Weights? = null
) {
    companion object {
        val lens = Jackson.autoBody<CalculationData>().toLens()
    }
}

data class ScheduleRequest(
    val time: Long,
    val electricity: Electricity,
    val start: ZonedDateTime? = null,
    val end: ZonedDateTime? = null,
    val priceWeight: Double? = null,
    val intensityWeight: Double? = null
) {
    companion object {
        val lens = Jackson.autoBody<ScheduleRequest>().toLens()
    }
}

data class Weights(val priceWeight: Double, val intensityWeight: Double)

private fun Prices.toResponse() = OctopusPricesResponse(this.results.map(PriceData::toResponse))
private fun PriceData.toResponse() = PricesResponse(wholesalePrice, retailPrice, from, to)

private object NoOctopusProducts : Failed {
    override fun toErrorResponse() = ErrorResponse("No Octopus products")
}
