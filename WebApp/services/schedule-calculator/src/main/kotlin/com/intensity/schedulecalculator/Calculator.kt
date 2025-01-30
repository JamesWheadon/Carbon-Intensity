package com.intensity.schedulecalculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

fun calculate(electricity: Electricity, weights: Weights, time: Long): ChargeTime {
    val normalizedElectricity = normalize(electricity)
    val slotScores = normalizedElectricity.slots.map { slot ->
        Triple(slot.from, slot.to, slot.price * weights.price + slot.intensity * weights.intensity)
    }
    val slotSpan = (time.toInt() + 29) / 30
    val best = slotScores.windowed(slotSpan).minBy { span -> span.sumOf { it.third } }
    return ChargeTime(best.first().first, best.first().first.plusMinutes(time))
}

fun normalize(electricity: Electricity): Electricity {
    val normalizedPrices = normalize(electricity.slots.map { it.price })
    val normalizedIntensities = normalize(electricity.slots.map { it.intensity })
    return Electricity(electricity.slots.mapIndexed { index, slot ->
        slot.copy(
            price = normalizedPrices[index],
            intensity = normalizedIntensities[index]
        )
    })
}

fun normalize(values: List<BigDecimal>): List<BigDecimal> {
    val max = values.max()
    return values.map { it.divide(max, 5, RoundingMode.HALF_UP) }
}

data class Electricity(val slots: List<HalfHourElectricity>)
data class HalfHourElectricity(
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    val price: BigDecimal,
    val intensity: BigDecimal
)
data class Weights(val price: BigDecimal, val intensity: BigDecimal)
data class ChargeTime(val from: ZonedDateTime, val to: ZonedDateTime)
