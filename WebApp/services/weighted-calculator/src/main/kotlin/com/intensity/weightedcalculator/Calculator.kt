package com.intensity.weightedcalculator

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.HalfHourElectricity
import com.intensity.core.timeChunked
import com.intensity.core.validate
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
        val dataSlotsSpanned = (time.toInt() + 29) / 30
        val slotFractionToExclude = slotFractionToExclude(time)
        var bestStartTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
        var bestScore = BigDecimal("1000")
        timeChunks.forEach { chunk ->
            val slotScores = chunk.map { slot ->
                Triple(slot.from, slot.to, slot.price * weights.price + slot.intensity * weights.intensity)
            }
            slotScores.windowed(dataSlotsSpanned).forEach { window ->
                val baseScore = window.sumOf { it.third }
                val earlyScore = baseScore.minus(window.last().third * slotFractionToExclude)
                val lateScore = baseScore.minus(window.first().third * slotFractionToExclude)
                if (earlyScore < bestScore) {
                    bestScore = earlyScore
                    bestStartTime = window.first().first
                }
                if (lateScore < bestScore) {
                    bestScore = lateScore
                    bestStartTime = window.last().second.minusMinutes(time)
                }
            }
        }
        ChargeTime(bestStartTime, bestStartTime.plusMinutes(time))
    }

private fun slotFractionToExclude(time: Long): BigDecimal {
    val minutesLessThanFullSlot = (30 - (time.toInt() % 30)) % 30
    return BigDecimal(minutesLessThanFullSlot).divide(BigDecimal("30"), 5, HALF_UP)
}

data class Weights(val price: BigDecimal, val intensity: BigDecimal)
