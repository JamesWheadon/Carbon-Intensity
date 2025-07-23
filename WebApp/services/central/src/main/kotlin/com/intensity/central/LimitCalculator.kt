package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.chargeTimeLens
import com.intensity.observability.Observability
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import java.math.BigDecimal

interface LimitCalculator {
    fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed>
    fun priceLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed>
}

class LimitCalculatorCloud(private val httpHandler: HttpHandler, private val observability: Observability) : LimitCalculator {
    override fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): Result<ChargeTime, Failed> {
        val response = observability.outboundHttp("Intensity limit", "Limit")
            .then(httpHandler)(
            Request(POST, "_://limit/calculate/intensity/$limit").with(
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
        val response = observability.outboundHttp("Price limit", "Limit")
            .then(httpHandler)(Request(POST, "_://limit/calculate/price/$limit").with(
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