package com.intensity.nationalgrid

import com.intensity.core.ErrorResponse
import com.intensity.core.Failed
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import io.opentelemetry.api.OpenTelemetry
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.format.Jackson
import java.time.ZonedDateTime

interface NationalGrid {
    fun intensity(from: ZonedDateTime, to: ZonedDateTime): Result<NationalGridData, Failed>
}

class NationalGridCloud(private val httpHandler: HttpHandler, private val openTelemetry: OpenTelemetry) : NationalGrid {
    override fun intensity(from: ZonedDateTime, to: ZonedDateTime): Result<NationalGridData, Failed> {
        val start = from.plusMinutes(30 - from.minute % 30L)
        val end = to.plusMinutes((30 - (to.minute % 30L)) % 30L)
        val tracingFilter = ClientFilters.OpenTelemetryTracing(
            openTelemetry,
            spanName(),
            spanCreationMutator = { spanBuilder -> spanBuilder.setAttribute("target.name", "National Grid") }
        )
        val response = tracingFilter.then(httpHandler)(Request(GET, "/intensity/$start/$end"))
        return when (response.status) {
            OK -> Success(nationalGridDataLens(response))
            else -> Failure(NationalGridFailed)
        }
    }
}

private fun spanName(): (Request) -> String = {
    "Fetch Carbon Intensity"
}

fun nationalGridClient() = ClientFilters.SetHostFrom(Uri.of("https://api.carbonintensity.org.uk"))
    .then(JavaHttpClient())

data class NationalGridData(val data: List<IntensityData>)
data class IntensityData(val from: ZonedDateTime, val to: ZonedDateTime, val intensity: Intensity)
data class Intensity(val forecast: Int, val actual: Int?, val index: String)

val nationalGridDataLens = Jackson.autoBody<NationalGridData>().toLens()

object NationalGridFailed : Failed {
    override fun toErrorResponse() = ErrorResponse("Failed to get intensity data")
}
