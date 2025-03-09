package com.intensity.central

import com.intensity.nationalgrid.NationalGrid
import com.intensity.openapi.ContractSchema
import com.intensity.scheduler.Intensities
import com.intensity.scheduler.Scheduler
import com.intensity.scheduler.SchedulerIntensitiesData
import com.intensity.scheduler.SchedulerJackson
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.valueOrNull
import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.meta
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun intensities(
    scheduler: Scheduler,
    nationalGrid: NationalGrid
): ContractRoute = "intensities" meta {
    summary = "Intensity data for the current 48 hour period"
    description =
        "Retrieves the intensity data for the current 48 hour period used in the model"
    tags += Tag("Data")
    consumes += APPLICATION_JSON
    produces += APPLICATION_JSON
    returning(
        OK,
        intensitiesResponseLens to IntensitiesResponse(
            List(48) { 100 },
            Instant.parse("2024-09-30T00:00:00Z")
        )
    )
} bindContract POST to { _: Request ->
    val intensitiesData = scheduler.getIntensitiesData()
    val startOfDay = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()
    val intensitiesForecast = if (intensitiesData.forCurrentDay(startOfDay)) {
        intensitiesData.valueOrNull()!!.intensities
    } else {
        updateSchedulerIntensities(nationalGrid, startOfDay, scheduler)
    }
    Response(OK).with(
        intensitiesResponseLens of IntensitiesResponse(
            intensitiesForecast,
            startOfDay
        )
    )
}

private fun Result<SchedulerIntensitiesData, String>.forCurrentDay(
    startOfDay: Instant?
) = valueOrNull() != null && valueOrNull()!!.date == startOfDay

private fun updateSchedulerIntensities(
    nationalGrid: NationalGrid,
    startOfDay: Instant,
    scheduler: Scheduler
): List<Int> {
    val gridData = nationalGrid.fortyEightHourIntensity(startOfDay)
    val intensitiesForecast = gridData.data.map { halfHourSlot -> halfHourSlot.intensity.forecast }
    scheduler.sendIntensities(Intensities(intensitiesForecast, startOfDay))
    return intensitiesForecast
}

data class IntensitiesResponse(val intensities: List<Int>, val date: Instant) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "intensities" to FieldMetadata("minItems" to 48, "maxItems" to 48),
            "date" to FieldMetadata("format" to "date-time")
        )
}

val intensitiesResponseLens = SchedulerJackson.autoBody<IntensitiesResponse>().toLens()
