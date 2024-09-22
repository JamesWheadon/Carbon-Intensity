package com.learning

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.learning.Matchers.inTimeRange
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

private const val TIMEZONE = "Europe/London"
private const val SECONDS_IN_HALF_HOUR = 1800L
private const val SECONDS_IN_DAY = 86400L

interface NationalGridContractTest {
    val httpClient: HttpHandler

    @Test
    fun `responds with forecast for the most recent full half hour`() {
        val currentIntensity = httpClient(Request(GET, "/intensity"))
        val halfHourResponses = nationalGridDataLens(currentIntensity)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(1))
        val currentData = halfHourResponses.data.first()
        assertThat(
            Instant.now().minusSeconds(30 * 60),
            inTimeRange(currentData.from, currentData.to.plusSeconds(TIME_DIFFERENCE_TOLERANCE))
        )
    }

    @Test
    fun `responds with forecast for the current date in UTC time for the date in the Europe-London timezone`() {
        val currentIntensity = httpClient(Request(GET, "/intensity/date"))
        val halfHourResponses = nationalGridDataLens(currentIntensity)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(48))
        assertThat(
            Instant.now().truncatedTo(ChronoUnit.DAYS),
            inTimeRange(halfHourResponses.data.first().from, halfHourResponses.data.last().to)
        )
    }

    @Test
    fun `responds with forecast for the requested date`() {
        val yesterday = Instant.now().minusSeconds(SECONDS_IN_DAY).truncatedTo(ChronoUnit.DAYS)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val dateIntensity = httpClient(Request(GET, "/intensity/date/${dateFormat.format(Date.from(yesterday))}"))
        val halfHourResponses = nationalGridDataLens(dateIntensity)

        assertThat(dateIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(48))
        assertThat(yesterday, inTimeRange(halfHourResponses.data.first().from, halfHourResponses.data.last().to))
    }
}

class FakeNationalGridTest : NationalGridContractTest {
    override val httpClient = FakeNationalGrid()
}

@Disabled
class NationalGridTest : NationalGridContractTest {
    override val httpClient = SetHostFrom(Uri.of("https://api.carbonintensity.org.uk")).then(JavaHttpClient())
}

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
    @JsonSerialize(using = NationalGridInstantSerializer::class)
    @JsonDeserialize(using = NationalGridInstantDeserializer::class)
    val from: Instant,
    @JsonSerialize(using = NationalGridInstantSerializer::class)
    @JsonDeserialize(using = NationalGridInstantDeserializer::class)
    val to: Instant,
    val intensity: Intensity
)
data class Intensity(val forecast: Long, val actual: Long?, val index: String)

val nationalGridDataLens = Jackson.autoBody<NationalGridData>().toLens()

class NationalGridInstantDeserializer : StdDeserializer<Instant?>(Instant::class.java) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX")
    }

    override fun deserialize(
        jsonparser: JsonParser, context: DeserializationContext?
    ): Instant {
        return Instant.from(formatter.parse(jsonparser.text))
    }
}

class NationalGridInstantSerializer : StdSerializer<Instant>(Instant::class.java) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX").withZone(ZoneOffset.UTC)
    }

    override fun serialize(instant: Instant, jgen: JsonGenerator, provider: SerializerProvider) {
        jgen.writeString(formatter.format(instant))
    }
}
