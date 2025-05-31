package com.intensity.central

import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.observability.TestProfile.Local
import com.intensity.observability.TestTracingOpenTelemetry
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import java.time.ZonedDateTime

abstract class EndToEndTest {
    val time: ZonedDateTime = ZonedDateTime.parse("2025-03-25T12:00:00Z")
    val octopus = FakeOctopus()
    val nationalGrid = FakeNationalGrid()
    val limitCalculator = FakeLimitCalculator()
    val weightsCalculator = FakeWeightsCalculator()
    private val centralOpenTelemetry = TestTracingOpenTelemetry(Local, "central")
    val app =
        carbonIntensity(
            NationalGridCloud(nationalGrid, centralOpenTelemetry),
            OctopusCloud(octopus, centralOpenTelemetry),
            LimitCalculatorCloud(limitCalculator, centralOpenTelemetry),
            WeightsCalculatorCloud(weightsCalculator, centralOpenTelemetry),
            centralOpenTelemetry
        )

    fun getErrorResponse(message: String) = """{"error":"$message"}"""
}
