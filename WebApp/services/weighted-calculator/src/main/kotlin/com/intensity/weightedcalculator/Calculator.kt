package com.intensity.weightedcalculator

import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.SlotScore
import com.intensity.core.calculateChargeTime
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.time.ZonedDateTime

fun calculate(
    electricity: Electricity,
    weights: Weights,
    start: ZonedDateTime,
    end: ZonedDateTime,
    time: Long
) =
    electricity
        .windowed(start, end)
        .validate()
        .normalize()
        .bestChargeTime(weights, time)

fun Result<Electricity, Failed>.normalize() =
    this.map { it.normalize() }

fun Electricity.normalize(): Electricity {
    val normalizedPrices = normalize(data.map { it.price })
    val normalizedIntensities = normalize(data.map { it.intensity })
    return Electricity(data.mapIndexed { index, slot ->
        slot.copy(
            price = normalizedPrices[index],
            intensity = normalizedIntensities[index]
        )
    })
}

fun normalize(values: List<BigDecimal>): List<BigDecimal> {
    val max = values.max()
    return values.map { it.divide(max, 5, HALF_UP) }
}

private fun Result<Electricity, Failed>.bestChargeTime(weights: Weights, time: Long) =
    this.flatMap { electricity ->
        val weightedCalculation = SlotScore { it.price * weights.price + it.intensity * weights.intensity }
        calculateChargeTime(electricity.data, time, weightedCalculation)
    }

data class Weights(val price: BigDecimal, val intensity: BigDecimal)
