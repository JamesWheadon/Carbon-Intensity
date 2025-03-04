package com.intensity.limitcalculator

import java.math.BigDecimal
import java.time.ZonedDateTime

fun underIntensityLimit(electricity: Electricity, intensityLimit: BigDecimal) =
    electricity.slots.filter { halfHour -> halfHour.intensity <= intensityLimit }

data class Electricity(val slots: List<HalfHourElectricity>)
data class HalfHourElectricity(
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    val price: BigDecimal,
    val intensity: BigDecimal
)
