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
            val data = electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit }
            calculateChargeTime(data, time) { it.price }
        }

fun underPriceLimit(electricity: Electricity, priceLimit: BigDecimal, time: Long) =
    electricity
        .validate()
        .flatMap {
            val data = electricity.slots.filter { halfHour -> halfHour.price <= priceLimit }
            calculateChargeTime(data, time) { it.intensity }
        }
