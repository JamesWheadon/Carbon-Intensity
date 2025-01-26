package com.intensity.nationalgrid

import com.intensity.coretest.inTimeRange
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset.UTC

abstract class NationalGridContractTest {
    abstract val nationalGrid: NationalGrid

    @Test
    fun `responds with forecast for the requested 48 hour period`() {
        val time = LocalDate.now().atStartOfDay(UTC.normalized()).toInstant()
        val intensities = nationalGrid.fortyEightHourIntensity(time)

        assertThat(intensities.data.size, equalTo(96))
        assertThat(
            time,
            inTimeRange(intensities.data.first().from, intensities.data.last().to)
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
