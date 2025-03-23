package com.intensity.core

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.format.Jackson
import java.math.BigDecimal
import java.time.Duration
import java.time.ZonedDateTime

fun interface SlotScore {
    fun getSlotScore(slot: HalfHourElectricity): BigDecimal
}

data class ChargeTime(val from: ZonedDateTime, val to: ZonedDateTime)

val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()

fun calculateChargeTime(
    data: List<HalfHourElectricity>,
    time: Long,
    slotScore: SlotScore
): Result<ChargeTime, Failed> {
    val timeChunks = getTimeChunks(data).filter { chunk ->
        Duration.between(chunk.first().from, chunk.last().to).toMinutes() >= time
    }
    if (timeChunks.isEmpty()) {
        return Failure(NoChargeTimePossible)
    }
    var bestStartTime = timeChunks.first().first().from
    var bestScore: BigDecimal? = null
    timeChunks.forEach { chunk ->
        val scored = chunk.map { Triple(it.from, it.to, slotScore.getSlotScore(it)) }
        scored.forEachIndexed { index, window ->
            val following = scored.subList(index, scored.size).takeWhile { it.first < window.first.plusMinutes(time) }
            val preceding = scored.subList(0, index + 1).takeLastWhile { it.second > window.second.minusMinutes(time) }
            if (following.sumOf { Duration.between(it.first, it.second).toMinutes() } >= time) {
                val endTime = window.first.plusMinutes(time)
                val followingScore = following.sumOf {
                    if (!endTime.isBefore(it.second)) {
                        it.third * Duration.between(it.first, it.second).toMinutes().toBigDecimal()
                    } else {
                        it.third * Duration.between(it.first, endTime).toMinutes().toBigDecimal()
                    }
                }
                if (bestScore == null || followingScore < bestScore) {
                    bestScore = followingScore
                    bestStartTime = window.first
                }
            }
            if (preceding.sumOf { Duration.between(it.first, it.second).toMinutes() } >= time) {
                val startTime = window.second.minusMinutes(time)
                val precedingScore = preceding.sumOf {
                    if (!startTime.isAfter(it.first)) {
                        it.third * Duration.between(it.first, it.second).toMinutes().toBigDecimal()
                    } else {
                        it.third * Duration.between(startTime, it.second).toMinutes().toBigDecimal()
                    }
                }
                if (bestScore == null || precedingScore < bestScore) {
                    bestScore = precedingScore
                    bestStartTime = startTime
                }
            }
        }
    }
    return Success(ChargeTime(bestStartTime, bestStartTime.plusMinutes(time)))
}

private fun getTimeChunks(data: List<HalfHourElectricity>) =
    data.sortedBy { it.from }
        .fold<HalfHourElectricity, MutableList<MutableList<HalfHourElectricity>>>(mutableListOf()) { acc, slot ->
            if (acc.isEmpty() || acc.last().last().to != slot.from) {
                acc.add(mutableListOf(slot))
            } else {
                acc.last().add(slot)
            }
            acc
        }

object NoChargeTimePossible : Failed {
    override fun toErrorResponse() = ErrorResponse("No charge time possible")
}
