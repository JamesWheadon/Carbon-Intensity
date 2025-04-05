package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.chargeTimeLens
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with

interface WeightsCalculator {
    fun chargeTime(electricity: Electricity, weights: Weights, time: Long): Result<ChargeTime, Failed>
}

class WeightsCalculatorCloud(val httpHandler: HttpHandler) : WeightsCalculator {
    override fun chargeTime(electricity: Electricity, weights: Weights, time: Long): Result<ChargeTime, Failed> {
        val response = httpHandler(
            Request(
                Method.POST,
                "/calculate"
            ).with(
                ScheduleRequest.lens of ScheduleRequest(
                    time,
                    electricity,
                    priceWeight = weights.priceWeight,
                    intensityWeight = weights.intensityWeight
                )
            )
        )
        return if (response.status == OK) {
            Success(chargeTimeLens(response))
        } else {
            Failure(UnableToCalculateChargeTime)
        }
    }
}