package com.intensity.weightedcalculator

import com.intensity.core.Electricity
import com.intensity.core.NoChargeTimePossible
import com.intensity.core.chargeTimeLens
import com.intensity.core.errorResponseLens
import com.intensity.core.handleLensFailures
import dev.forkhandles.result4k.fold
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind

fun weightedCalculatorApp() = handleLensFailures()
    .then(weightedCalculatorRoute())

private fun weightedCalculatorRoute() = "/calculate" bind POST to { request ->
    val scheduleRequest = scheduleRequestLens(request)
    calculate(scheduleRequest.electricity, scheduleRequest.weights(), scheduleRequest.time)
        .fold(
            { chargeTime -> Response(OK).with(chargeTimeLens of chargeTime) },
            { failed ->
                val status = when (failed) {
                    NoChargeTimePossible -> NOT_FOUND
                    else -> BAD_REQUEST
                }
                Response(status).with(errorResponseLens of failed.toErrorResponse())
            }
        )
}

data class ScheduleRequest(
    val time: Long,
    val priceWeight: Double,
    val intensityWeight: Double,
    val electricity: Electricity
) {
    fun weights() = Weights(
        priceWeight.toBigDecimal(),
        intensityWeight.toBigDecimal()
    )
}

val scheduleRequestLens = Jackson.autoBody<ScheduleRequest>().toLens()
