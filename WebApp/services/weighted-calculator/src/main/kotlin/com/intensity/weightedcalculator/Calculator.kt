package com.intensity.weightedcalculator

import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.HalfHourElectricity
import com.intensity.core.SlotScore
import com.intensity.core.calculateChargeTime
import com.intensity.core.timeChunked
import com.intensity.core.validate
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

fun calculate(electricity: Electricity, weights: Weights, time: Long) =
    electricity.validate()
        .normalize()
        .timeChunked(time)
        .bestChargeTime(weights, time)

fun Result<Electricity, Failed>.normalize() =
    this.map { it.normalize() }

fun Electricity.normalize(): Electricity {
    val normalizedPrices = normalize(slots.map { it.price })
    val normalizedIntensities = normalize(slots.map { it.intensity })
    return Electricity(slots.mapIndexed { index, slot ->
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

private fun Result<Electricity, Failed>.timeChunked(time: Long) = this.flatMap { it.timeChunked(time) }

private fun Result<List<List<HalfHourElectricity>>, Failed>.bestChargeTime(weights: Weights, time: Long) =
    this.map { timeChunks ->
        val weightedCalculation = SlotScore { it.price * weights.price + it.intensity * weights.intensity }
        calculateChargeTime(timeChunks, time, weightedCalculation)
    }

data class Weights(val price: BigDecimal, val intensity: BigDecimal)
