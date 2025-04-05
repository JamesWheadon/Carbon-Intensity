package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.chargeTimeLens
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import java.math.BigDecimal

interface LimitCalculator {
    fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): ChargeTime?
    fun priceLimit(electricity: Electricity, limit: BigDecimal, time: Long): ChargeTime?
}

class LimitCalculatorCloud(val httpHandler: HttpHandler) : LimitCalculator {
    override fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): ChargeTime? {
        val response = httpHandler(
            Request(
                Method.POST,
                "/calculate/intensity/$limit"
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
            chargeTimeLens(response)
        } else {
            null
        }
    }

    override fun priceLimit(electricity: Electricity, limit: BigDecimal, time: Long): ChargeTime? {
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
            chargeTimeLens(response)
        } else {
            null
        }
    }
}