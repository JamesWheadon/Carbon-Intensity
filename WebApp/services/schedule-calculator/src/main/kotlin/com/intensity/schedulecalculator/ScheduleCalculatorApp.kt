package com.intensity.schedulecalculator

import dev.forkhandles.result4k.valueOrNull
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind
import java.math.BigDecimal
import java.time.ZonedDateTime

fun schedulerApp() = "/schedule" bind POST to { request ->
    val scheduleRequest = scheduleRequestLens(request)
    val time = scheduleRequest.time
    val weights = scheduleRequest.weights()
    val electricity = scheduleRequest.electricity()
    val chargeTime = calculate(electricity, weights, time).valueOrNull()!!
    Response(OK).with(chargeTimeLens of chargeTime)
}

data class ScheduleRequest(
    val time: Long,
    val priceWeight: Double,
    val intensityWeight: Double,
    val electricityData: List<HalfHourElectricityData>
) {
    fun weights() = Weights(
        BigDecimal(priceWeight.toString()),
        BigDecimal(intensityWeight.toString())
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
            BigDecimal(price.toString()),
            BigDecimal(intensity.toString())
        )
    }
}

val scheduleRequestLens = Jackson.autoBody<ScheduleRequest>().toLens()
val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()
