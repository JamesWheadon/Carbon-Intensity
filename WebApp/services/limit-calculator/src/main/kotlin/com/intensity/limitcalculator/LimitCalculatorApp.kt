package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.chargeTimeLens
import com.intensity.core.errorResponseLens
import com.intensity.core.handleLensFailures
import dev.forkhandles.result4k.fold
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Path
import org.http4k.lens.bigDecimal
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.ZonedDateTime

fun limitCalculatorApp() = handleLensFailures()
    .then(limitRoutes())

private fun limitRoutes() = routes(
    "/calculate/intensity/{limit}" bind POST to { request ->
        val scheduleRequest = scheduleRequestLens(request)
        val intensityLimit = limitLens(request)
        underIntensityLimit(scheduleRequest.electricity, intensityLimit, scheduleRequest.time)
            .fold(
                { chargeTime -> Response(OK).with(chargeTimeLens of chargeTime) },
                { failed -> Response(BAD_REQUEST).with(errorResponseLens of failed.toErrorResponse()) }
            )
    },
    "/calculate/price/{limit}" bind POST to { request ->
        val scheduleRequest = scheduleRequestLens(request)
        val priceLimit = limitLens(request)
        underPriceLimit(scheduleRequest.electricity, priceLimit, scheduleRequest.time)
            .fold(
                { chargeTime -> Response(OK).with(chargeTimeLens of chargeTime) },
                { failed -> Response(BAD_REQUEST).with(errorResponseLens of failed.toErrorResponse()) }
            )
    }
)

data class ScheduleRequest(
    val time: Long,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val electricity: Electricity
)

val scheduleRequestLens = Jackson.autoBody<ScheduleRequest>().toLens()
val limitLens = Path.bigDecimal().of("limit")
