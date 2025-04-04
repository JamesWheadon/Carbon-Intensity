package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.calculateChargeTime
import com.intensity.core.validate
import dev.forkhandles.result4k.flatMap
import java.math.BigDecimal

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal, time: Long) =
    electricity
        .validate()
        .flatMap {
            val data = electricity.data.filter { dataPoint -> dataPoint.intensity <= intensityLimit }
            calculateChargeTime(data, time) { it.price }
        }

fun underPriceLimit(electricity: Electricity, priceLimit: BigDecimal, time: Long) =
    electricity
        .validate()
        .flatMap {
            val data = electricity.data.filter { dataPoint -> dataPoint.price <= priceLimit }
            calculateChargeTime(data, time) { it.intensity }
        }
