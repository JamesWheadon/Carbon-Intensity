package com.intensity.central

import com.intensity.core.ErrorResponse
import com.intensity.core.errorResponseLens
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.nationalgrid.nationalGridClient
import com.intensity.openapi.ContractSchema
import com.intensity.openapi.openApi3
import com.intensity.scheduler.ChargeDetails
import com.intensity.scheduler.Intensities
import com.intensity.scheduler.PythonScheduler
import com.intensity.scheduler.Scheduler
import com.intensity.scheduler.SchedulerIntensitiesData
import com.intensity.scheduler.SchedulerJackson
import com.intensity.scheduler.schedulerClient
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.contract.ContractRoute
import org.http4k.contract.PreFlightExtraction.Companion.None
import org.http4k.contract.Tag
import org.http4k.contract.contract
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.meta
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    val schedulerUrl = System.getenv("SCHEDULER_URL") ?: "http://localhost:8080"
    val server = carbonIntensityServer(
        port,
        PythonScheduler(schedulerClient(schedulerUrl)),
        NationalGridCloud(nationalGridClient())
    ).start()
    println("Server started on " + server.port())
}

val corsMiddleware = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)

fun carbonIntensityServer(port: Int, scheduler: Scheduler, nationalGrid: NationalGrid): Http4kServer {
    return carbonIntensity(scheduler, nationalGrid).asServer(SunHttp(port))
}

fun carbonIntensity(scheduler: Scheduler, nationalGrid: NationalGrid) =
    corsMiddleware.then(
        contractRoutes(scheduler, nationalGrid)
    )

private fun contractRoutes(scheduler: Scheduler, nationalGrid: NationalGrid) = contract {
    renderer = openApi3()
    descriptionPath = "/openapi.json"
    preFlightExtraction = None
    routes += chargeTimes(scheduler)
    routes += intensities(scheduler, nationalGrid)
}

private fun chargeTimes(scheduler: Scheduler) =
    "/charge-time" meta {
        summary = "get best time to consume electricity"
        tags += Tag("Optimisation")
        description =
            "finds the best time to consume electricity given the start time and the 48 hour data in the scheduler"
        consumes += APPLICATION_JSON
        produces += APPLICATION_JSON
        receiving(
            chargeDetailsRequestLens to ChargeDetailsRequest(
                Instant.parse("2024-09-30T21:20:00Z"),
                Instant.parse("2024-10-01T02:30:00Z"),
                60
            )
        )
        returning(OK, chargeTimeResponseLens to ChargeTimeResponse(Instant.parse("2024-09-30T11:30:00Z")))
    } bindContract POST to { request ->
        val chargeDetails = chargeDetailsRequestLens(request).toChargeDetails()
        if (chargeDetails.isValid()) {
            retrieveChargeTime(scheduler, chargeDetails)
        } else {
            Response(BAD_REQUEST).with(errorResponseLens of ErrorResponse("end time must be after start time by at least the charge duration, default 30"))
        }
    }

private fun retrieveChargeTime(
    scheduler: Scheduler,
    chargeDetails: ChargeDetails
): Response {
    var bestChargeTime = scheduler.getBestChargeTime(chargeDetails)
    if (bestChargeTime == Failure("Duration has not been trained")) {
        scheduler.trainDuration(chargeDetails.duration ?: 30)
        bestChargeTime = scheduler.getBestChargeTime(chargeDetails)
    }
    return when (bestChargeTime) {
        is Success -> Response(OK).with(
            chargeTimeResponseLens of ChargeTimeResponse(bestChargeTime.valueOrNull()!!.chargeTime)
        )

        is Failure -> Response(NOT_FOUND).with(
            errorResponseLens of ErrorResponse("unable to find charge time")
        )
    }
}

private fun intensities(
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

private fun ChargeDetails.isValid() = endTime == null || endTime!! >= startTime.plusSeconds(duration?.times(60L) ?: 0)

data class ChargeDetailsRequest(val startTime: Instant, val endTime: Instant?, val duration: Int?) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "startTime" to FieldMetadata("format" to "date-time"),
            "endTime" to FieldMetadata("format" to "date-time"),
            "duration" to FieldMetadata("format" to "int32")
        )

    fun toChargeDetails() = ChargeDetails(startTime, endTime, duration)
}

data class IntensitiesResponse(val intensities: List<Int>, val date: Instant) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "intensities" to FieldMetadata("minItems" to 48, "maxItems" to 48),
            "date" to FieldMetadata("format" to "date-time")
        )
}

data class ChargeTimeResponse(val chargeTime: Instant) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "chargeTime" to FieldMetadata("format" to "date-time")
        )
}

val intensitiesResponseLens = SchedulerJackson.autoBody<IntensitiesResponse>().toLens()
val chargeDetailsRequestLens = SchedulerJackson.autoBody<ChargeDetailsRequest>().toLens()
val chargeTimeResponseLens = SchedulerJackson.autoBody<ChargeTimeResponse>().toLens()
