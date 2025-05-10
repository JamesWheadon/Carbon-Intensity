package com.intensity.nationalgrid

import com.intensity.coretest.isFailure
import com.intensity.observability.TestOpenTelemetry
import com.intensity.observability.TestOpenTelemetry.Companion.TestProfile.Local
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.describe
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import io.opentelemetry.api.common.AttributeType.STRING
import io.opentelemetry.api.internal.InternalAttributeKeyImpl.create
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

abstract class NationalGridContractTest {
    val time: ZonedDateTime = LocalDate.now().atStartOfDay(UTC.normalized())
    abstract val nationalGrid: NationalGrid

    @Test
    fun `responds with forecast for the requested time period`() {
        val intensities = nationalGrid.intensity(time.plusMinutes(1), time.plusHours(6).minusMinutes(7)).valueOrNull()!!

        assertThat(intensities.data.size, equalTo(12))
        assertThat(intensities.data.first().from, equalTo(time))
        assertThat(intensities.data.last().to, equalTo(time.plusHours(6)))
    }

    @Test
    fun `responds with forecast for the requested time period and not an earlier time slot`() {
        val intensities = nationalGrid.intensity(time, time.plusHours(6)).valueOrNull()!!

        assertThat(intensities.data.size, equalTo(12))
        assertThat(intensities.data.first().from, equalTo(time))
        assertThat(intensities.data.last().to, equalTo(time.plusHours(6)))
    }
}

class FakeNationalGridTest : NationalGridContractTest() {
    private val fakeNationalGrid = FakeNationalGrid()
    private val testOpenTelemetry = TestOpenTelemetry(Local)
    override val nationalGrid = NationalGridCloud(fakeNationalGrid, testOpenTelemetry)

    @Test
    fun `responds with correct failure if error getting intensity data for time period`() {
        fakeNationalGrid.shouldFail()
        val response = nationalGrid.intensity(time, time.plusHours(6))

        assertThat(response, isFailure(NationalGridFailed))
    }

    @Test
    fun `creates a span with data about the call`() {
        nationalGrid.intensity(time, time.plusHours(6))

        assertThat(testOpenTelemetry.spanNames(), equalTo(listOf("Fetch Carbon Intensity")))
        val fetchSpan = testOpenTelemetry.spans().first { it.name == "Fetch Carbon Intensity" }
        assertThat(
            fetchSpan.attributes.asMap(),
            containsEntries(
                listOf(
                    create<String>("http.status_code", STRING) to "200",
                    create<String>("http.url", STRING) to "/intensity/${time.toLocalDate()}T00:30Z/${time.toLocalDate()}T06:00Z",
                    create<String>("http.method", STRING) to "GET",
                    create<String>("target.name", STRING) to "National Grid"
                )
            )
        )
    }
}

@Disabled
class NationalGridTest : NationalGridContractTest() {
    override val nationalGrid = NationalGridCloud(nationalGridClient())
}

private fun <T, E> containsEntries(expected: List<Pair<T, E>>) = object : Matcher<Map<T, E>> {
    override fun invoke(actual: Map<T, E>): MatchResult {
        val entries = actual.entries.map { it.key to it.value }
        return if (expected.all { entries.contains(it) }) {
            Match
        } else {
            Mismatch("was: ${describe(actual)}")
        }
    }

    override val description: String get() = "contains entries ${describe(expected)}"
    override val negatedDescription: String get() = "does not contain entries ${describe(expected)}"
}
