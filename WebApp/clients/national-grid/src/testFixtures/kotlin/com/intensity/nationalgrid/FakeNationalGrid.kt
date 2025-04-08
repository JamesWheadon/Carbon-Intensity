package com.intensity.nationalgrid

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

private const val SECONDS_IN_HALF_HOUR = 1800L

class FakeNationalGrid : HttpHandler {
    private var dayData: NationalGridData? = null
    private var failure = false

    val routes = routes(
        "/intensity/{time}/fw48h" bind Method.GET to handler@{ request ->
            if (failure) {
                return@handler Response(INTERNAL_SERVER_ERROR)
            }
            if (dayData != null) {
                Response(Status.OK).with(nationalGridDataLens of dayData!!)
            } else {
                val startTime = ZonedDateTime.parse(request.path("time")!!)
                val dataWindows = createHalfHourWindows(startTime.minusSeconds(30 * 60), 97)
                Response(Status.OK).with(nationalGridDataLens of NationalGridData(dataWindows))
            }
        },
        "/intensity/{from}/{to}" bind Method.GET to handler@{ request ->
            if (failure) {
                return@handler Response(INTERNAL_SERVER_ERROR)
            }
            var startTime = ZonedDateTime.parse(request.path("from")!!)
            startTime = startTime.minusMinutes(30 + startTime.minute % 30L)
            var endTime = ZonedDateTime.parse(request.path("to")!!)
            endTime = endTime.minusMinutes(endTime.minute % 30L)
            val dataWindows = createHalfHourWindows(startTime, ((Duration.between(startTime, endTime).toMinutes() + 29) / 30).toInt())
            Response(Status.OK).with(nationalGridDataLens of NationalGridData(dataWindows))
        }
    )

    private fun createHalfHourWindows(startTime: ZonedDateTime, timeBlocks: Int = 97): MutableList<IntensityData> {
        val currentTime = ZonedDateTime.now()
        val dataWindows = mutableListOf<IntensityData>()
        for (window in 0 until timeBlocks) {
            val (windowStart, windowEnd) = halfHourWindow(startTime.plusSeconds(window * SECONDS_IN_HALF_HOUR))
            val actualIntensity = if (windowStart.isBefore(currentTime)) {
                60
            } else {
                null
            }
            dataWindows.add(IntensityData(windowStart, windowEnd, Intensity(60, actualIntensity, "moderate")))
        }
        return dataWindows
    }

    private fun halfHourWindow(windowTime: ZonedDateTime): Pair<ZonedDateTime, ZonedDateTime> {
        val truncatedToMinutes = windowTime.truncatedTo(ChronoUnit.MINUTES)
        val minutesPastNearestHalHour = truncatedToMinutes.minute % 30
        return Pair(
            truncatedToMinutes.minusSeconds(minutesPastNearestHalHour * 60L),
            truncatedToMinutes.plusSeconds((30 - minutesPastNearestHalHour) * 60L)
        )
    }

    fun setDateData(startOfDay: ZonedDateTime, forecasts: List<Int>, actual: List<Int?>) {
        dayData = NationalGridData(
            forecasts.zip(actual).mapIndexed { i, data ->
                val (start, end) = halfHourWindow(startOfDay.plusSeconds(i * SECONDS_IN_HALF_HOUR))
                val index = when {
                    data.first <= 40 -> "very low"
                    data.first <= 80 -> "low"
                    data.first <= 120 -> "moderate"
                    data.first <= 160 -> "high"
                    else -> "very high"
                }
                IntensityData(start, end, Intensity(data.first, data.second, index))
            }
        )
    }

    fun shouldFail() {
        failure = true
    }

    override fun invoke(request: Request) = routes(request)
}
