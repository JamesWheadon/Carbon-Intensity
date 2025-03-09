package com.intensity.weightedcalculator

import com.intensity.core.Electricity
import com.intensity.core.ErrorResponse
import com.intensity.core.HalfHourElectricity
import com.intensity.core.chargeTimeLens
import com.intensity.core.errorResponseLens
import dev.forkhandles.result4k.fold
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.Jackson
import org.http4k.routing.bind
import java.time.ZonedDateTime

fun weightedCalculatorApp() = CatchLensFailure { _ -> handleLensFailure() }
    .then(weightedCalculatorRoute())

private fun weightedCalculatorRoute() = "/calculate" bind POST to { request ->
    val scheduleRequest = scheduleRequestLens(request)
    calculate(scheduleRequest.electricity(), scheduleRequest.weights(), scheduleRequest.time)
        .fold(
            { chargeTime -> Response(OK).with(chargeTimeLens of chargeTime) },
            { failed -> Response(BAD_REQUEST).with(errorResponseLens of failed.toErrorResponse()) }
        )
}

fun handleLensFailure() = Response(BAD_REQUEST).with(errorResponseLens of ErrorResponse("Invalid Request"))

data class ScheduleRequest(
    val time: Long,
    val priceWeight: Double,
    val intensityWeight: Double,
    val electricityData: List<HalfHourElectricityData>
) {
    fun weights() = Weights(
        priceWeight.toBigDecimal(),
        intensityWeight.toBigDecimal()
    )

    fun electricity() = Electricity(
        electricityData.map { it.toHalfHourElectricity() }
    )
}

data class HalfHourElectricityData(
    val startTime: String,
    val price: Double,
    val intensity: Double
) {
    fun toHalfHourElectricity(): HalfHourElectricity {
        val start = ZonedDateTime.parse(startTime)
        return HalfHourElectricity(
            start,
            start.plusMinutes(30),
            price.toBigDecimal(),
            intensity.toBigDecimal()
        )
    }
}

val scheduleRequestLens = Jackson.autoBody<ScheduleRequest>().toLens()
