package com.intensity.core

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import java.math.BigDecimal
import java.time.ZonedDateTime

data class Electricity(val data: List<ElectricityData>) {
    fun windowed(start: ZonedDateTime, end: ZonedDateTime): Electricity {
        return this.copy(data = data.mapNotNull { dataPoint ->
            val pointStart = if (dataPoint.from.isBefore(start)) {
                start
            } else {
                dataPoint.from
            }
            val pointEnd = if (dataPoint.to.isAfter(end)) {
                end
            } else {
                dataPoint.to
            }
            if (!pointStart.isAfter(pointEnd)) {
                dataPoint.copy(from = pointStart, to = pointEnd)
            } else {
                null
            }
        })
    }

    fun validate(): Result<Electricity, Failed> =
        if (this.data.sortedBy { it.from }.windowed(2).any { it.last().from.isBefore(it.first().to) }) {
            Failure(OverlappingData)
        } else {
            Success(this)
        }
}

data class ElectricityData(
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    val price: BigDecimal,
    val intensity: BigDecimal
)

object OverlappingData : Failed {
    override fun toErrorResponse() = ErrorResponse("Overlapping data windows")
}
