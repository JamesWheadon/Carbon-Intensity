package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.calculateChargeTime
import dev.forkhandles.result4k.flatMap
import java.math.BigDecimal
import java.time.ZonedDateTime

fun underIntensityLimit(
    electricity: Electricity,
    intensityLimit: BigDecimal,
    start: ZonedDateTime,
    end: ZonedDateTime,
    time: Long
) =
    electricity
        .windowed(start, end)
        .validate()
        .flatMap { windowedElectricity ->
            val data = windowedElectricity.data.filter { dataPoint -> dataPoint.intensity <= intensityLimit }
            calculateChargeTime(data, time) { it.price }
        }

fun underPriceLimit(
    electricity: Electricity,
    priceLimit: BigDecimal,
    start: ZonedDateTime,
    end: ZonedDateTime,
    time: Long
) =
    electricity
        .windowed(start, end)
        .validate()
        .flatMap { windowedElectricity ->
            val data = windowedElectricity.data.filter { dataPoint -> dataPoint.price <= priceLimit }
            calculateChargeTime(data, time) { it.intensity }
        }
