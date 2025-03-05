package com.intensity.limitcalculator

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.timeChunked
import com.intensity.core.validate
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import java.math.BigDecimal

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal, time: Long) =
    electricity.validate().flatMap {
        it.copy(slots = electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit })
            .timeChunked(time)
    }.map {
        val best = it.flatten().minBy { it.price }
        ChargeTime(best.from, best.to)
    }
