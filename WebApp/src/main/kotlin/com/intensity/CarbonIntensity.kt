package com.intensity

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS

fun main() {
    val server = carbonIntensityServer(
        9000,
        PythonScheduler(schedulerClient()),
        NationalGridCloud(nationalGridClient())
    ).start()
    println("Server started on " + server.port())
}

val corsMiddleware = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)

fun carbonIntensityServer(port: Int, scheduler: Scheduler, nationalGrid: NationalGrid): Http4kServer {
    return carbonIntensity(scheduler, nationalGrid).asServer(SunHttp(port))
}

fun carbonIntensity(scheduler: Scheduler, nationalGrid: NationalGrid): (Request) -> Response {
    return corsMiddleware.then(
        CatchLensFailure.then(
            routes(
                "/charge-time" bind POST to { request ->
                    val chargeDetails = chargeDetailsLens(request)
                    if (chargeDetails.isValid()) {
                        getChargeTime(scheduler, chargeDetails, nationalGrid)
                    } else {
                        Response(BAD_REQUEST).with(errorResponseLens of ErrorResponse("end time must be after start time by at least the charge duration, default 30"))
                    }
                },
                "intensities" bind GET to {
                    val intensitiesData = scheduler.getIntensitiesData()
                    if (intensitiesData.intensities == null) {
                        val currentDate = LocalDate.now()
                        val gridData = nationalGrid.dateIntensity(currentDate)
                        val intensitiesForecast = gridData.data.map { halfHourSlot -> halfHourSlot.intensity.forecast }
                        scheduler.sendIntensities(Intensities(intensitiesForecast, currentDate.atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()))
                        Response(OK).with(intensitiesResponseLens of IntensitiesResponse(intensitiesForecast))
                    } else {
                        Response(OK).with(intensitiesResponseLens of IntensitiesResponse(intensitiesData.intensities))
                    }
                }
            )
        )
    )
}

private fun getChargeTime(
    scheduler: Scheduler,
    chargeDetails: ChargeDetails,
    nationalGrid: NationalGrid
): Response {
    var bestChargeTime = scheduler.getBestChargeTime(chargeDetails)
    if (bestChargeTime.chargeTime == null) {
        updateScheduler(chargeDetails, nationalGrid, scheduler)
        scheduler.trainDuration(chargeDetails.duration ?: 30)
        bestChargeTime = scheduler.getBestChargeTime(chargeDetails)
    }
    return if (bestChargeTime.chargeTime != null) {
        Response(OK).with(chargeTimeResponseLens of bestChargeTime.toResponse())
    } else {
        Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("unable to find charge time"))
    }
}

private fun updateScheduler(
    chargeDetails: ChargeDetails,
    nationalGrid: NationalGrid,
    scheduler: Scheduler
) {
    val dateIntensity = getCarbonIntensitiesForDate(chargeDetails, nationalGrid)
    val intensities = Intensities(
        dateIntensity.data.map { halfHour -> halfHour.intensity.forecast },
        chargeDetails.startTime.truncatedTo(DAYS)
    )
    scheduler.sendIntensities(intensities)
}

private fun getCarbonIntensitiesForDate(
    chargeDetails: ChargeDetails,
    nationalGrid: NationalGrid
): NationalGridData {
    val chargeDate = LocalDateTime.ofInstant(chargeDetails.startTime, ZoneOffset.UTC).toLocalDate()
    val dateIntensity = nationalGrid.dateIntensity(chargeDate)
    return dateIntensity
}

interface Scheduler {
    fun sendIntensities(intensities: Intensities): ErrorResponse?
    fun trainDuration(duration: Int): ErrorResponse?
    fun getBestChargeTime(chargeDetails: ChargeDetails): ChargeTime
    fun getIntensitiesData(): SchedulerIntensitiesData
    fun deleteData()
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

    override fun trainDuration(duration: Int): ErrorResponse? {
        val response = httpHandler(Request(PATCH, "/intensities/train?duration=$duration"))
        return if (response.status == Status.NO_CONTENT) {
            null
        } else {
            errorResponseLens(response)
        }
    }

    override fun getBestChargeTime(chargeDetails: ChargeDetails): ChargeTime {
        val timestamp = formatWith(schedulerPattern).format(chargeDetails.startTime)
        var query = "current=$timestamp"
        if (chargeDetails.endTime != null) {
            val endTimestamp = formatWith(schedulerPattern).format(chargeDetails.endTime)
            query += "&end=$endTimestamp"
        }
        if (chargeDetails.duration != null) {
            query += "&duration=${chargeDetails.duration}"
        }
        return chargeTimeLens(httpHandler(Request(GET, "/charge-time?$query")))
    }

    override fun getIntensitiesData(): SchedulerIntensitiesData {
        return schedulerIntensitiesDataLens(httpHandler(Request(GET, "/intensities")))
    }

    override fun deleteData() {
        httpHandler(Request(DELETE, "/intensities"))
    }
}

fun schedulerClient() = ClientFilters.SetHostFrom(Uri.of("http://localhost:8000")).then(JavaHttpClient())

interface NationalGrid {
    fun dateIntensity(date: LocalDate): NationalGridData
}

class NationalGridCloud(val httpHandler: HttpHandler) : NationalGrid {
    override fun dateIntensity(date: LocalDate): NationalGridData {
        val dateIntensity = httpHandler(Request(GET, "/intensity/date/$date"))
        return nationalGridDataLens(dateIntensity)
    }
}

fun nationalGridClient() = ClientFilters.SetHostFrom(Uri.of("https://api.carbonintensity.org.uk"))
    .then(JavaHttpClient())

data class ChargeDetails(val startTime: Instant, val endTime: Instant?, val duration: Int?)
private fun ChargeDetails.isValid() = endTime == null || endTime >= startTime.plusSeconds(duration?.times(60L) ?: 0)

data class IntensitiesResponse(val intensities: List<Int>)
data class Intensities(val intensities: List<Int>, val date: Instant)
data class ChargeTime(val chargeTime: Instant?, val error: String?)
data class SchedulerIntensitiesData(val intensities: List<Int>?, val date: Instant?, val error: String?)
data class ChargeTimeResponse(val chargeTime: Instant)
data class ErrorResponse(val error: String)
data class NationalGridData(val data: List<HalfHourData>)
data class HalfHourData(val from: Instant, val to: Instant, val intensity: Intensity)
data class Intensity(val forecast: Int, val actual: Int?, val index: String)

private fun ChargeTime.toResponse() = ChargeTimeResponse(this.chargeTime!!)

val intensitiesResponseLens = Jackson.autoBody<IntensitiesResponse>().toLens()
val chargeDetailsLens = SchedulerJackson.autoBody<ChargeDetails>().toLens()
val intensitiesLens = SchedulerJackson.autoBody<Intensities>().toLens()
val chargeTimeLens = SchedulerJackson.autoBody<ChargeTime>().toLens()
val schedulerIntensitiesDataLens = SchedulerJackson.autoBody<SchedulerIntensitiesData>().toLens()
val chargeTimeResponseLens = SchedulerJackson.autoBody<ChargeTimeResponse>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
val nationalGridDataLens = NationalGridJackson.autoBody<NationalGridData>().toLens()

const val schedulerPattern: String = "yyyy-MM-dd'T'HH:mm:ss"
const val gridPattern = "yyyy-MM-dd'T'HH:mmX"

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
