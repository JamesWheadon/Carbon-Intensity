package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.timeChunked
import com.intensity.core.validate
import dev.forkhandles.result4k.flatMap
import java.math.BigDecimal

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal, time: Long) =
    electricity.validate().flatMap {
        it.copy(slots = electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit })
            .timeChunked(time)
    }
