package com.intensity.central

import com.intensity.core.SchedulerJackson
import com.intensity.core.errorResponseLens
import com.intensity.nationalgrid.NationalGrid
import com.intensity.openapi.ContractSchema
import dev.forkhandles.result4k.fold
import org.http4k.contract.ContractRoute
import org.http4k.contract.Tag
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.meta
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun intensities(
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
            ZonedDateTime.parse("2024-09-30T00:00:00Z")
        )
    )
} bindContract POST to { _: Request ->
    val startOfDay = LocalDate.now().atStartOfDay().atZone(ZoneId.of("UTC").normalized())
    nationalGrid.fortyEightHourIntensity(startOfDay)
        .fold(
            { intensitiesForecast ->
                Response(OK).with(
                    intensitiesResponseLens of IntensitiesResponse(
                        intensitiesForecast.data.map { it.intensity.forecast },
                        startOfDay
                    )
                )
            },
            { failed ->
                Response(NOT_FOUND).with(errorResponseLens of failed.toErrorResponse())
            }
        )
}

data class IntensitiesResponse(val intensities: List<Int>, val date: ZonedDateTime) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "intensities" to FieldMetadata("minItems" to 48, "maxItems" to 48),
            "date" to FieldMetadata("format" to "date-time")
        )
}

val intensitiesResponseLens = SchedulerJackson.autoBody<IntensitiesResponse>().toLens()
