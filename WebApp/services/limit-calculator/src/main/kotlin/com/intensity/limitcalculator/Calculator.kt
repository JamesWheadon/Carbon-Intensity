package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.timeChunked
import java.math.BigDecimal

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal) =
    electricity.copy(slots = electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit })
        .timeChunked()
