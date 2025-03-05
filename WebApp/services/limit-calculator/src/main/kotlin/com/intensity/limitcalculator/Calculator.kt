package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.timeChunked
import com.intensity.core.validate
import dev.forkhandles.result4k.map
import java.math.BigDecimal

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal) =
    electricity.validate().map {
        it.copy(slots = electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit })
            .timeChunked()
    }
