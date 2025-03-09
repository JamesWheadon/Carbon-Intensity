package com.intensity.central

import com.intensity.core.ErrorResponse
import com.intensity.core.errorResponseLens
import com.intensity.openapi.ContractSchema
import com.intensity.scheduler.ChargeDetails
import com.intensity.scheduler.Scheduler
import com.intensity.scheduler.SchedulerJackson
import com.intensity.scheduler.UntrainedDuration
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.fold
import org.http4k.contract.Tag
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.meta
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import java.time.Instant

fun chargeTimes(scheduler: Scheduler) =
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
                .fold(
                    { chargeTime ->
                        Response(OK).with(chargeTimeResponseLens of ChargeTimeResponse(chargeTime.chargeTime))
                    },
                    { failed ->
                        Response(NOT_FOUND).with(errorResponseLens of failed.toErrorResponse())
                    }
                )
        } else {
            Response(BAD_REQUEST).with(errorResponseLens of ErrorResponse("end time must be after start time by at least the charge duration, default 30"))
        }
    }

private fun retrieveChargeTime(
    scheduler: Scheduler,
    chargeDetails: ChargeDetails
) =
    scheduler.getBestChargeTime(chargeDetails)
        .flatMapFailure { failed ->
            if (failed is UntrainedDuration) {
                scheduler.trainDuration(chargeDetails.duration ?: 30)
                scheduler.getBestChargeTime(chargeDetails)
            } else {
                Failure(failed)
            }
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

data class ChargeTimeResponse(val chargeTime: Instant) : ContractSchema {
    override fun schemas(): Map<String, FieldMetadata> =
        mapOf(
            "chargeTime" to FieldMetadata("format" to "date-time")
        )
}

val chargeDetailsRequestLens = SchedulerJackson.autoBody<ChargeDetailsRequest>().toLens()
val chargeTimeResponseLens = SchedulerJackson.autoBody<ChargeTimeResponse>().toLens()
