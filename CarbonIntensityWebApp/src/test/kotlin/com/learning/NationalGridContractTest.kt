package com.learning

import com.learning.Matchers.inTimeRange
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.SetHostFrom
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
import java.util.TimeZone

private const val TIMEZONE = "Europe/London"
private const val SECONDS_IN_HALF_HOUR = 1800L

interface NationalGridContractTest {
    val nationalGrid: NationalGrid

    @Test
    fun `responds with forecast for the most recent full half hour`() {
        val currentIntensity = nationalGrid.currentIntensity()

        assertThat(
            Instant.now().minusSeconds(30 * 60),
            inTimeRange(currentIntensity.from, currentIntensity.to.plusSeconds(TIME_DIFFERENCE_TOLERANCE))
        )
    }

    @Test
    fun `responds with forecast for the current date in UTC time for the date in the Europe-London timezone`() {
        val currentDayIntensity = nationalGrid.currentDayIntensity()

        assertThat(currentDayIntensity.data.size, equalTo(48))
        assertThat(
            Instant.now().truncatedTo(ChronoUnit.DAYS),
            inTimeRange(currentDayIntensity.data.first().from, currentDayIntensity.data.last().to)
        )
    }

    @Test
    fun `responds with forecast for the requested date`() {
        val date = LocalDate.now(UTC).minusDays(1)
        val dateIntensity = nationalGrid.dateIntensity(date)

        assertThat(dateIntensity.data.size, equalTo(48))
        assertThat(date.atStartOfDay().toInstant(UTC), inTimeRange(dateIntensity.data.first().from, dateIntensity.data.last().to))
    }
}

interface NationalGrid {
    fun currentIntensity(): HalfHourData

    fun currentDayIntensity(): NationalGridData

    fun dateIntensity(date: LocalDate): NationalGridData
}

class NationalGridCloud(val httpHandler: HttpHandler) : NationalGrid {
    override fun currentIntensity(): HalfHourData {
        val currentIntensity = httpHandler(Request(GET, "/intensity"))
        return nationalGridDataLens(currentIntensity).data.first()
    }

    override fun currentDayIntensity(): NationalGridData {
        val currentIntensity = httpHandler(Request(GET, "/intensity/date"))
        return nationalGridDataLens(currentIntensity)
    }

    override fun dateIntensity(date: LocalDate): NationalGridData {
        val dateIntensity = httpHandler(Request(GET, "/intensity/date/$date"))
        return nationalGridDataLens(dateIntensity)
    }
}

class FakeNationalGridTest : NationalGridContractTest {
    override val nationalGrid = NationalGridCloud(FakeNationalGrid())
}

@Disabled
class NationalGridTest : NationalGridContractTest {
    override val nationalGrid = NationalGridCloud(nationalGridClient())
}

fun nationalGridClient() = SetHostFrom(Uri.of("https://api.carbonintensity.org.uk")).then(JavaHttpClient())

class FakeNationalGrid : HttpHandler {
    val routes = routes(
        "/intensity" bind GET to {
            val currentIntensity = Intensity(60, 60, "moderate")
            val (windowStart, windowEnd) = halfHourWindow(Instant.now().minusSeconds(SECONDS_IN_HALF_HOUR))
            val currentHalfHour = HalfHourData(windowStart, windowEnd, currentIntensity)
            Response(OK).with(nationalGridDataLens of NationalGridData(listOf(currentHalfHour)))
        },
        "/intensity/date" bind GET to {
            val tz = TimeZone.getTimeZone(TIMEZONE)
            val offset = if (tz.useDaylightTime()) {
                tz.rawOffset + tz.dstSavings
            } else {
                tz.rawOffset
            }
            val startTime = Instant.now().truncatedTo(ChronoUnit.DAYS).minusMillis(offset.toLong())
            val dataWindows = createHalfHourWindows(startTime)
            Response(OK).with(nationalGridDataLens of NationalGridData(dataWindows))
        },
        "/intensity/date/{date}" bind GET to { request ->
            val date = LocalDate.parse(request.path("date")!!)
            val startTime = date.atStartOfDay(ZoneId.of(TIMEZONE)).toInstant()
            val dataWindows = createHalfHourWindows(startTime)
            Response(OK).with(nationalGridDataLens of NationalGridData(dataWindows))
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

private fun createHalfHourWindows(
    startTime: Instant
): MutableList<HalfHourData> {
    val currentTime = Instant.now()
    val dataWindows = mutableListOf<HalfHourData>()
    for (window in 0 until 48) {
        val (windowStart, windowEnd) = halfHourWindow(startTime.plusSeconds(window * SECONDS_IN_HALF_HOUR))
        val actualIntensity = if (windowStart.isBefore(currentTime)) {
            60L
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

data class NationalGridData(val data: List<HalfHourData>)
data class HalfHourData(
    val from: Instant,
    val to: Instant,
    val intensity: Intensity
)

data class Intensity(val forecast: Long, val actual: Long?, val index: String)

val nationalGridDataLens = NationalGridJackson.autoBody<NationalGridData>().toLens()
