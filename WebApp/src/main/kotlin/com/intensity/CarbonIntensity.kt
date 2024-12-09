package com.intensity

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.contract.ContractRoute
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.PreFlightExtraction.Companion.None
import org.http4k.contract.Tag
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.OpenApiVersion
import org.http4k.contract.openapi.v3.AutoJsonToJsonSchema
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.lens.LensFailure
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    val schedulerUrl = System.getenv("SCHEDULER_URL") ?: "http://localhost:8000"
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

fun carbonIntensity(scheduler: Scheduler, nationalGrid: NationalGrid): (Request) -> Response {
    return corsMiddleware.then(
        contractRoutes(scheduler, nationalGrid)
    )
}

private fun contractRoutes(scheduler: Scheduler, nationalGrid: NationalGrid) = contract {
    renderer = OpenApi3(
        apiInfo = ApiInfo("Carbon Intensity Calculator", "v1.0"),
        json = OpenAPIJackson,
        extensions = emptyList(),
        apiRenderer = ApiRenderer.Auto(OpenAPIJackson, AutoJsonToJsonSchema(OpenAPIJackson, emptyMap())),
        errorResponseRenderer = object : ErrorResponseRenderer {
            override fun badRequest(lensFailure: LensFailure) = failedToParseRequest(lensFailure)
        },
        servers = emptyList(),
        version = OpenApiVersion._3_0_0
    )
    descriptionPath = "/openapi.json"
    preFlightExtraction = None
    routes += chargeTimes(scheduler)
    routes += intensities(scheduler, nationalGrid)
}

private fun failedToParseRequest(failure: LensFailure): Response {
    return when (failure.cause) {
        is MismatchedInputException -> Response(BAD_REQUEST).with(errorResponseLens of ErrorResponse("incorrect request body or headers"))
        else -> Response(UNSUPPORTED_MEDIA_TYPE).with(errorResponseLens of ErrorResponse("invalid content type"))
    }
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
            chargeDetailsLens to ChargeDetails(
                Instant.parse("2024-09-30T21:20:00Z"),
                Instant.parse("2024-10-01T02:30:00Z"),
                60
            )
        )
        returning(OK, chargeTimeResponseLens to ChargeTimeResponse(Instant.parse("2024-09-30T11:30:00Z")))
    } bindContract POST to { request ->
        val chargeDetails = chargeDetailsLens(request)
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

data class ChargeDetails(val startTime: Instant, val endTime: Instant?, val duration: Int?)

private fun ChargeDetails.isValid() = endTime == null || endTime >= startTime.plusSeconds(duration?.times(60L) ?: 0)

data class IntensitiesResponse(val intensities: List<Int>, val date: Instant)
data class ChargeTimeResponse(val chargeTime: Instant)
data class ErrorResponse(val error: String)

val intensitiesResponseLens = SchedulerJackson.autoBody<IntensitiesResponse>().toLens()
val chargeDetailsLens = SchedulerJackson.autoBody<ChargeDetails>().toLens()
val chargeTimeResponseLens = SchedulerJackson.autoBody<ChargeTimeResponse>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()

fun formatWith(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    .withZone(ZoneOffset.UTC)
