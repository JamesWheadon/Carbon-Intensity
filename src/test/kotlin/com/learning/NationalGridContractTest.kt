package com.learning

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


abstract class NationalGridContractTest {
    abstract val httpClient: HttpHandler

    @Test
    fun `responds when pinged on the base path`() {
        assertThat(httpClient(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `responds with forecast for the current half hour`() {
        val currentIntensity = httpClient(Request(GET, "/intensity"))
        val halfHourResponses = nationalGridDataLens(currentIntensity)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(1))
    }

    @Test
    fun `responds with forecast for the current day`() {
        val currentIntensity = httpClient(Request(GET, "/intensity/date"))
        val halfHourResponses = nationalGridDataLens(currentIntensity)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(48))
    }
}

class FakeNationalGridTest : NationalGridContractTest() {
    override val httpClient = FakeNationalGrid()
}

class FakeNationalGrid : HttpHandler {
    val routes = routes(
        "/" bind GET to { Response(OK) },
        "intensity" bind GET to {
            val currentIntensity = Intensity(60, 60, "moderate")
            val (windowStart, windowEnd) = halfHourWindow(Instant.now())
            val currentHalfHour = HalfHourData(windowStart, windowEnd, currentIntensity)
            Response(OK).with(nationalGridDataLens of NationalGridData(listOf(currentHalfHour)))
        },
        "intensity/date" bind GET to {
            val currentTime = Instant.now()
            val startTime = currentTime.truncatedTo(ChronoUnit.DAYS)
            val dataWindows = mutableListOf<HalfHourData>()
            for (window in 0 until 48) {
                val (windowStart, windowEnd) = halfHourWindow(startTime.plusSeconds(window * 30 * 60L))
                val actualIntensity = if (windowStart.isBefore(currentTime)) {
                    60L
                } else {
                    null
                }
                dataWindows.add(HalfHourData(windowStart, windowEnd, Intensity(60, actualIntensity, "moderate")))
            }
            Response(OK).with(nationalGridDataLens of NationalGridData(dataWindows))
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class Intensity(
    val forecast: Long,
    val actual: Long?,
    val index: String
)

private fun halfHourWindow(windowTime: Instant): Pair<Instant, Instant> {
    val truncatedToMinutes = windowTime.truncatedTo(ChronoUnit.MINUTES)
    val minutesPastNearestHalHour = truncatedToMinutes.atZone(ZoneOffset.UTC).minute % 30
    return Pair(
        truncatedToMinutes.minusSeconds(minutesPastNearestHalHour * 60L),
        truncatedToMinutes.plusSeconds((30 - minutesPastNearestHalHour) * 60L)
    )
}

data class HalfHourData(
    @JsonSerialize(using = NationalGridInstantSerializer::class)
    @JsonDeserialize(using = NationalGridInstantDeserializer::class)
    val from: Instant,
    @JsonSerialize(using = NationalGridInstantSerializer::class)
    @JsonDeserialize(using = NationalGridInstantDeserializer::class)
    val to: Instant,
    val intensity: Intensity
)

data class NationalGridData(
    val data: List<HalfHourData>
)

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
