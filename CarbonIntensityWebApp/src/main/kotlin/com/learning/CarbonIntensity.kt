package com.learning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main() {
    val server = carbonIntensityServer(9000, PythonScheduler(schedulerClient())).start()

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int, scheduler: Scheduler): Http4kServer {
    return carbonIntensity(scheduler).asServer(SunHttp(port))
}

fun carbonIntensity(scheduler: Scheduler): (Request) -> Response {
    return CatchLensFailure.then(
        routes(
            "/charge-time" bind POST to { request ->
                val chargeDetails = chargeDetailsLens(request)
                val bestChargeTime = scheduler.getBestChargeTime(chargeDetails.startTime)
                Response(OK).with(chargeTimeResponseLens of bestChargeTime.toResponse())
            }
        )
    )
}

const val schedulerPattern: String = "yyyy-MM-dd'T'HH:mm:ss"

object SchedulerJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(
            BiDiMapping(
                { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                { instant -> formatWith(schedulerPattern).format(instant) }
            )
        )
        .done()
        .deactivateDefaultTyping()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)

const val gridPattern = "yyyy-MM-dd'T'HH:mmX"

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

fun formatWith(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    .withZone(ZoneOffset.UTC)

interface Scheduler {
    fun sendIntensities(intensities: Intensities): ErrorResponse?

    fun getBestChargeTime(chargeTime: Instant): ChargeTime
}

class PythonScheduler(val httpHandler: HttpHandler) : Scheduler {
    override fun sendIntensities(intensities: Intensities): ErrorResponse? {
        val response = httpHandler(Request(POST, "/intensities").with(intensitiesLens of intensities))
        return if (response.status == Status.NO_CONTENT) {
            null
        } else {
            errorResponseLens(response)
        }
    }

    override fun getBestChargeTime(chargeTime: Instant): ChargeTime {
        val timestamp = formatWith(schedulerPattern).format(chargeTime)
        return chargeTimeLens(httpHandler(Request(Method.GET, "/charge-time?current=$timestamp")))
    }
}

fun schedulerClient() = ClientFilters.SetHostFrom(Uri.of("http://localhost:8000")).then(JavaHttpClient())

data class ChargeDetails(val startTime: Instant)
data class Intensities(val intensities: List<Int>, val date: Instant)
data class ChargeTime(val chargeTime: Instant?, val error: String?)
data class ChargeTimeResponse(val chargeTime: Instant)
data class ErrorResponse(val error: String)

private fun ChargeTime.toResponse() = ChargeTimeResponse(this.chargeTime!!)

val chargeDetailsLens = SchedulerJackson.autoBody<ChargeDetails>().toLens()
val intensitiesLens = SchedulerJackson.autoBody<Intensities>().toLens()
val chargeTimeLens = SchedulerJackson.autoBody<ChargeTime>().toLens()
val chargeTimeResponseLens = SchedulerJackson.autoBody<ChargeTimeResponse>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
