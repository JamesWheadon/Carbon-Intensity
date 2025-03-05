package com.intensity.core

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
