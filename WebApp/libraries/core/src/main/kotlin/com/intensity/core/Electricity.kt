package com.intensity.core

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import java.math.BigDecimal
import java.time.ZonedDateTime

data class Electricity(val slots: List<HalfHourElectricity>)
data class HalfHourElectricity(
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    val price: BigDecimal,
    val intensity: BigDecimal
)

fun Electricity.timeChunked(): MutableList<MutableList<HalfHourElectricity>> =
    slots.sortedBy { it.from }
        .fold(mutableListOf()) { acc, slot ->
            if (acc.isEmpty() || acc.last().last().to != slot.from) {
                acc.add(mutableListOf(slot))
            } else {
                acc.last().add(slot)
            }
            acc
        }

fun Electricity.validate(): Result<Electricity, Failed> =
    if (this.slots.sortedBy { it.from }.windowed(2).any { it.last().from.isBefore(it.first().to) }) {
        Failure(OverlappingData)
    } else {
        Success(this)
    }

object OverlappingData : Failed {
    override fun toErrorResponse() = ErrorResponse("Overlapping data windows")
}