package com.intensity

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import java.time.Instant

interface NationalGrid {
    fun fortyEightHourIntensity(time: Instant): NationalGridData
}

class NationalGridCloud(val httpHandler: HttpHandler) : NationalGrid {
    override fun fortyEightHourIntensity(time: Instant): NationalGridData {
        val dateIntensity = httpHandler(Request(Method.GET, "/intensity/$time/fw48h"))
        val intensityData = nationalGridDataLens(dateIntensity)
        return NationalGridData(intensityData.data.drop(1))
    }
}

fun nationalGridClient() = ClientFilters.SetHostFrom(Uri.of("https://api.carbonintensity.org.uk"))
    .then(JavaHttpClient())

data class NationalGridData(val data: List<HalfHourData>)
data class HalfHourData(val from: Instant, val to: Instant, val intensity: Intensity)
data class Intensity(val forecast: Int, val actual: Int?, val index: String)

val nationalGridDataLens = NationalGridJackson.autoBody<NationalGridData>().toLens()
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
