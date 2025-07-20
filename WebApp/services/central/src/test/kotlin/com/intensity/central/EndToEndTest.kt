package com.intensity.central

import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.observability.TestObservability
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import java.time.ZonedDateTime

abstract class EndToEndTest {
    val time: ZonedDateTime = ZonedDateTime.parse("2025-03-25T12:00:00Z")
    val octopus = FakeOctopus()
    val nationalGrid = FakeNationalGrid()
    val limitCalculator = FakeLimitCalculator()
    val weightsCalculator = FakeWeightsCalculator()
    private val observability = TestObservability().observability("central")
    val app =
        carbonIntensity(
            NationalGridCloud(nationalGrid, observability),
            OctopusCloud(octopus, observability),
            LimitCalculatorCloud(limitCalculator, observability),
            WeightsCalculatorCloud(weightsCalculator, observability),
            observability
        )

    fun getErrorResponse(message: String) = """{"error":"$message"}"""
}
