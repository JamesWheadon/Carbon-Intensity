package com.intensity.nationalgrid

import com.intensity.coretest.containsEntries
import com.intensity.coretest.isFailure
import com.intensity.observability.TestProfile.Local
import com.intensity.observability.TestTracingOpenTelemetry
import com.intensity.observability.TracingOpenTelemetry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
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
    private val testOpenTelemetry = TestTracingOpenTelemetry(Local, "national-grid-test")
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
            fetchSpan.attributes,
            containsEntries(
                listOf(
                    "service.name" to "national-grid-test",
                    "http.status" to 200L,
                    "http.path" to "/intensity/${time.toLocalDate()}T00:30Z/${time.toLocalDate()}T06:00Z",
                    "http.target" to "National Grid"
                )
            )
        )
    }
}

@Disabled
class NationalGridTest : NationalGridContractTest() {
    override val nationalGrid = NationalGridCloud(nationalGridClient(), TracingOpenTelemetry.noOp())
}
