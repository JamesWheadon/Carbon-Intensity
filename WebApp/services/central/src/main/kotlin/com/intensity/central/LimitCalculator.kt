package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.chargeTimeLens
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import java.math.BigDecimal

interface LimitCalculator {
    fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed>
    fun priceLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed>
}

class LimitCalculatorCloud(val httpHandler: HttpHandler, private val openTelemetry: OpenTelemetry = OpenTelemetry.noop()) : LimitCalculator {
    override fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed> {
        val headers = mutableMapOf<String, String>()

        val setter = TextMapSetter<MutableMap<String, String>> { carrier, key, value ->
            carrier?.set(key, value)
        }

        val propagator = GlobalOpenTelemetry.getPropagators().textMapPropagator
        propagator.inject(Context.current(), headers, setter)
        val response = httpHandler(
            Request(
                Method.POST,
                "/calculate/intensity/$limit"
            ).headers(headers.map { it.key to it.value }).with(
                ScheduleRequest.lens of ScheduleRequest(
                    time,
                    electricity,
                    electricity.data.first().from,
                    electricity.data.last().to
                )
            )
        )
        return if (response.status == Status.OK) {
            Success(chargeTimeLens(response))
        } else {
            Failure(UnableToCalculateChargeTime)
        }
    }

    override fun priceLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed> {
        val response = httpHandler(
            Request(
                Method.POST,
                "/calculate/price/$limit"
            ).with(
                ScheduleRequest.lens of ScheduleRequest(
                    time,
                    electricity,
                    electricity.data.first().from,
                    electricity.data.last().to
                )
            )
        )
        return if (response.status == Status.OK) {
            Success(chargeTimeLens(response))
        } else {
            Failure(UnableToCalculateChargeTime)
        }
    }
}