package com.intensity.limitcalculator

import com.intensity.core.Electricity
import java.math.BigDecimal

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal) =
    electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit }
