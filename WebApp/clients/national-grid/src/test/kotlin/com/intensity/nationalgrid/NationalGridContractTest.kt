package com.intensity.nationalgrid

import com.intensity.coretest.inTimeRange
import com.intensity.coretest.isFailure
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
    fun `responds with forecast for the requested 48 hour period`() {
        val intensities = nationalGrid.fortyEightHourIntensity(time).valueOrNull()!!

        assertThat(intensities.data.size, equalTo(96))
        assertThat(
            time,
            inTimeRange(intensities.data.first().from, intensities.data.last().to)
        )
    }

    @Test
    fun `responds with forecast for the requested time period`() {
        val intensities = nationalGrid.intensity(time.plusMinutes(1), time.plusHours(6).minusMinutes(7))

        assertThat(intensities.data.size, equalTo(12))
        assertThat(intensities.data.first().from, equalTo(time))
        assertThat(intensities.data.last().to, equalTo(time.plusHours(6)))
    }

    @Test
    fun `responds with forecast for the requested time period and not an earlier time slot`() {
        val intensities = nationalGrid.intensity(time, time.plusHours(6))

        assertThat(intensities.data.size, equalTo(12))
        assertThat(intensities.data.first().from, equalTo(time))
        assertThat(intensities.data.last().to, equalTo(time.plusHours(6)))
    }
}

class FakeNationalGridTest : NationalGridContractTest() {
    private val fakeNationalGrid = FakeNationalGrid()
    override val nationalGrid = NationalGridCloud(fakeNationalGrid)

    @Test
    fun `responds with correct failure if error getting data`() {
        fakeNationalGrid.shouldFail()
        val response = nationalGrid.fortyEightHourIntensity(time)

        assertThat(response, isFailure(NationalGridFailed))
    }
}

@Disabled
class NationalGridTest : NationalGridContractTest() {
    override val nationalGrid = NationalGridCloud(nationalGridClient())
}
