package com.intensity.central

import com.intensity.core.Electricity
import com.intensity.core.ElectricityData
import com.intensity.core.errorResponseLens
import com.intensity.nationalgrid.NationalGrid
import com.intensity.openapi.ContractSchema
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.fold
import dev.forkhandles.result4k.map
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
import org.http4k.format.Jackson
import org.http4k.routing.bind
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun intensities(
    nationalGrid: NationalGrid
): ContractRoute = "/intensities" meta {
    summary = "Intensity data for the current 48 hour period"
    description =
        "Retrieves the intensity data for the current 48 hour period used in the model"
    tags += Tag("Data")
    consumes += APPLICATION_JSON
    produces += APPLICATION_JSON
    returning(
        OK,
        IntensitiesResponse.lens to IntensitiesResponse(
            List(48) { 100 },
            ZonedDateTime.parse("2024-09-30T00:00:00Z")
        )
    )
} bindContract POST to { _: Request ->
    val startOfDay = LocalDate.now(ZoneId.of("UTC")).atStartOfDay(ZoneId.of("UTC").normalized())
    nationalGrid.intensity(startOfDay, startOfDay.plusDays(2))
        .fold(
            { intensitiesForecast ->
                Response(OK).with(
                    IntensitiesResponse.lens of IntensitiesResponse(
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

fun intensityChargeTime(
    nationalGrid: NationalGrid,
    weightsCalculator: WeightsCalculator
) = "/intensities/charge-time" bind POST to { request ->
    val calculationData = IntensityCalculationData.lens(request)
    nationalGrid.intensity(calculationData.start, calculationData.end)
        .map { intensities ->
            Electricity(intensities.data.map { data -> ElectricityData(data.from, data.to, data.intensity.forecast.toBigDecimal(), BigDecimal("0")) })
        }.flatMap { electricity ->
            weightsCalculator.chargeTime(electricity, Weights(0.0, 1.0), calculationData.time)
        }.toChargeTimeResponse()
}

data class IntensitiesResponse(val intensities: List<Int>, val date: ZonedDateTime) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "intensities" to FieldMetadata("minItems" to 48, "maxItems" to 48),
            "date" to FieldMetadata("format" to "date-time")
        )

    companion object {
        val lens = Jackson.autoBody<IntensitiesResponse>().toLens()
    }
}

private data class IntensityCalculationData(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val time: Long
) {
    companion object {
        val lens = Jackson.autoBody<IntensityCalculationData>().toLens()
    }
}
