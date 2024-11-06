package com.intensity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit

private const val TIMEZONE = "Europe/London"
private const val SECONDS_IN_HALF_HOUR = 1800L

abstract class NationalGridContractTest {
    abstract val nationalGrid: NationalGrid

    @Test
    fun `responds with forecast for the requested date`() {
        val date = LocalDate.now(UTC).minusDays(1)
        val dateIntensity = nationalGrid.dateIntensity(date)

        assertThat(dateIntensity.data.size, equalTo(48))
        assertThat(
            date.atStartOfDay().toInstant(UTC),
            inTimeRange(dateIntensity.data.first().from, dateIntensity.data.last().to)
        )
    }
}

class FakeNationalGridTest : NationalGridContractTest() {
    override val nationalGrid = NationalGridCloud(FakeNationalGrid())
}

@Disabled
class NationalGridTest : NationalGridContractTest() {
    override val nationalGrid = NationalGridCloud(nationalGridClient())
}

class FakeNationalGrid : HttpHandler {
    val routes = routes(
        "/intensity/date/{date}" bind GET to { request ->
            val date = LocalDate.parse(request.path("date")!!)
            val startTime = date.atStartOfDay(ZoneId.of(TIMEZONE)).toInstant()
            val dataWindows = createHalfHourWindows(startTime)
            Response(OK).with(nationalGridDataLens of NationalGridData(dataWindows))
        }
    )

    private fun createHalfHourWindows(
        startTime: Instant
    ): MutableList<HalfHourData> {
        val currentTime = Instant.now()
        val dataWindows = mutableListOf<HalfHourData>()
        for (window in 0 until 48) {
            val (windowStart, windowEnd) = halfHourWindow(startTime.plusSeconds(window * SECONDS_IN_HALF_HOUR))
            val actualIntensity = if (windowStart.isBefore(currentTime)) {
                60
            } else {
                null
            }
            dataWindows.add(HalfHourData(windowStart, windowEnd, Intensity(60, actualIntensity, "moderate")))
        }
        return dataWindows
    }

    private fun halfHourWindow(windowTime: Instant): Pair<Instant, Instant> {
        val truncatedToMinutes = windowTime.truncatedTo(ChronoUnit.MINUTES)
        val minutesPastNearestHalHour = truncatedToMinutes.atZone(ZoneId.of(TIMEZONE)).minute % 30
        return Pair(
            truncatedToMinutes.minusSeconds(minutesPastNearestHalHour * 60L),
            truncatedToMinutes.plusSeconds((30 - minutesPastNearestHalHour) * 60L)
        )
    }

    override fun invoke(request: Request): Response = routes(request)
}
