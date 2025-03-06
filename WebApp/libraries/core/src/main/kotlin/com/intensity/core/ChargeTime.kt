package com.intensity.core

import org.http4k.format.Jackson
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

fun interface SlotScore {
    fun getSlotScore(slot: HalfHourElectricity): BigDecimal
}

data class ChargeTime(val from: ZonedDateTime, val to: ZonedDateTime)

val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()

fun calculateChargeTime(
    timeChunks: List<List<HalfHourElectricity>>,
    time: Long,
    slotScore: SlotScore
): ChargeTime {
    val dataSlotsSpanned = (time.toInt() + 29) / 30
    val slotFractionToExclude = slotFractionToExclude(time)
    var bestStartTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
    var bestScore = BigDecimal("1000")
    timeChunks.forEach { chunk ->
        chunk.map { Triple(it.from, it.to, slotScore.getSlotScore(it)) }
            .windowed(dataSlotsSpanned)
            .forEach { window ->
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
    return ChargeTime(bestStartTime, bestStartTime.plusMinutes(time))
}

private fun slotFractionToExclude(time: Long): BigDecimal {
    val minutesLessThanFullSlot = (30 - (time.toInt() % 30)) % 30
    return BigDecimal(minutesLessThanFullSlot).divide(BigDecimal("30"), 5, RoundingMode.HALF_UP)
}
