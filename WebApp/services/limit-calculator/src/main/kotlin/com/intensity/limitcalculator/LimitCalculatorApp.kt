package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.HalfHourElectricity
import com.intensity.core.chargeTimeLens
import dev.forkhandles.result4k.valueOrNull
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.math.BigDecimal
import java.time.ZonedDateTime

fun limitCalculatorApp() =
    routes(
        "/calculate/intensity/{limit}" bind Method.POST to { request ->
            val scheduleRequest = scheduleRequestLens(request)
            val intensityLimit = BigDecimal(request.path("limit"))
            val chargeTime =
                underIntensityLimit(scheduleRequest.electricity(), intensityLimit, scheduleRequest.time).valueOrNull()!!
            Response(OK).with(chargeTimeLens of chargeTime)
        },
        "/calculate/price/{limit}" bind Method.POST to { request ->
            val scheduleRequest = scheduleRequestLens(request)
            val priceLimit = BigDecimal(request.path("limit"))
            val chargeTime =
                underPriceLimit(scheduleRequest.electricity(), priceLimit, scheduleRequest.time).valueOrNull()!!
            Response(OK).with(chargeTimeLens of chargeTime)
        }
    )

data class ScheduleRequest(
    val time: Long,
    val electricityData: List<HalfHourElectricityData>
) {
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
