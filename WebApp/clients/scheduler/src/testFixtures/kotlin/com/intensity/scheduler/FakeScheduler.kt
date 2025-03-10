package com.intensity.scheduler

import com.intensity.core.ErrorResponse
import com.intensity.core.errorResponseLens
import com.intensity.core.formatWith
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.BiDiMapping
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.map
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Instant

const val SECONDS_IN_DAY = 86400L

class FakeScheduler : HttpHandler {
    private val chargeTimes = mutableMapOf<Instant, Instant>()
    private val errorChargeTime = mutableListOf<Instant>()
    private val trainedDurations = mutableSetOf<Int>()
    var data: Intensities? = null
    var trainedCalled = 0
    val routes = routes(
        "/charge-time" bind GET to { request ->
            val current = Query.map(
                BiDiMapping(
                    { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                    { instant -> formatWith(schedulerPattern).format(instant) }
                )
            ).defaulted("current", null)(request)
            val end = Query.map(
                BiDiMapping(
                    { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                    { instant -> formatWith(schedulerPattern).format(instant) }
                )
            ).defaulted("end", null)(request)
            val duration = Query.int().defaulted("duration", 30)(request)
            if (end != null && end.isBefore(current.plusSeconds(duration * 60L))) {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("End must be after current plus duration"))
            } else if (!trainedDurations.contains(duration)) {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("Duration has not been trained"))
            } else if (
                current.isBefore(data!!.date)
                || current.isAfter(data!!.date.plusSeconds(2 * SECONDS_IN_DAY))
                || errorChargeTime.contains(current)
            ) {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("No data for time slot"))
            } else {
                val response = chargeTimes[current]?.let { chargeTime -> getChargeTime(chargeTime, end, duration) }
                    ?: getChargeTime(current.plusSeconds(duration * 60L), end, duration)
                Response(OK).with(chargeTimeLens of response)
            }
        },
        "/intensities" bind POST to { request ->
            trainedDurations.clear()
            val requestBody = intensitiesLens(request)
            if (requestBody.intensities.size == 96) {
                data = requestBody
                Response(NO_CONTENT)
            } else {
                val errorMessage = if (requestBody.intensities.size > 96) {
                    "${requestBody.intensities} is too long"
                } else {
                    "${requestBody.intensities} is too short"
                }
                Response(BAD_REQUEST).with(
                    errorResponseLens of ErrorResponse(errorMessage)
                )
            }
        },
        "/intensities" bind GET to {
            if (data != null) {
                Response(OK).with(intensitiesLens of data!!)
            } else {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("No intensity data for scheduler"))
            }
        },
        "/intensities/train" bind PATCH to { request ->
            trainedCalled++
            if (data != null) {
                trainedDurations.add(Query.int().required("duration")(request))
                Response(NO_CONTENT)
            } else {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("No intensity data for scheduler"))
            }
        }
    )

    fun hasTrainedForDuration(duration: Int) {
        trainedDurations.add(duration)
    }

    fun hasBestChargeTimeForStart(startTimeToBestTime: Pair<Instant, Instant>) {
        chargeTimes[startTimeToBestTime.first] = startTimeToBestTime.second
    }

    fun canNotGetChargeTimeFor(badTime: Instant) {
        errorChargeTime.add(badTime)
    }

    fun hasIntensityData(intensities: Intensities) {
        data = intensities
    }

    private fun getChargeTime(chargeTime: Instant, end: Instant?, duration: Int): ChargeTime {
        val actionTime = if (end != null && end.minusSeconds(duration * 60L) < chargeTime) {
            end.minusSeconds(duration * 60L)
        } else {
            chargeTime
        }
        return ChargeTime(actionTime)
    }

    override fun invoke(request: Request) = routes(request)
}
