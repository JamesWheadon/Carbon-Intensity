package com.intensity.nationalgrid

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intensity.core.ErrorResponse
import com.intensity.core.Failed
import com.intensity.core.formatWith
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import java.time.Instant

interface NationalGrid {
    fun fortyEightHourIntensity(time: Instant): Result<NationalGridData, Failed>
}

class NationalGridCloud(val httpHandler: HttpHandler) : NationalGrid {
    override fun fortyEightHourIntensity(time: Instant): Result<NationalGridData, Failed> {
        val response = httpHandler(Request(Method.GET, "/intensity/$time/fw48h"))
        return when (response.status) {
            OK -> Success(NationalGridData(nationalGridDataLens(response).data.drop(1)))
            else -> Failure(NationalGridFailed)
        }
    }
}

fun nationalGridClient() = ClientFilters.SetHostFrom(Uri.of("https://api.carbonintensity.org.uk"))
    .then(JavaHttpClient())

data class NationalGridData(val data: List<HalfHourData>)
data class HalfHourData(val from: Instant, val to: Instant, val intensity: Intensity)
data class Intensity(val forecast: Int, val actual: Int?, val index: String)

val nationalGridDataLens = NationalGridJackson.autoBody<NationalGridData>().toLens()

object NationalGridFailed : Failed {
    override fun toErrorResponse() = ErrorResponse("Failed to get intensity data")
}

private const val gridPattern = "yyyy-MM-dd'T'HH:mmX"

object NationalGridJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(
            BiDiMapping(
                { timestamp -> Instant.from(formatWith(gridPattern).parse(timestamp)) },
                { instant -> formatWith(gridPattern).format(instant) }
            )
        )
        .done()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)
