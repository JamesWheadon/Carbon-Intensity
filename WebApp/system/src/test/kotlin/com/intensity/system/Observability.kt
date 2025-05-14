package com.intensity.system

import com.intensity.central.LimitCalculatorCloud
import com.intensity.central.WeightsCalculatorCloud
import com.intensity.central.carbonIntensity
import com.intensity.limitcalculator.limitCalculatorApp
import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.observability.TestOpenTelemetry
import com.intensity.observability.TestOpenTelemetry.Companion.TestProfile.Local
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import com.intensity.weightedcalculator.weightedCalculatorApp
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.time.ZonedDateTime

class Observability {
    private val nationalGridFake = FakeNationalGrid()
    private val octopusFake = FakeOctopus()
    private val openTelemetry = TestOpenTelemetry(Local)
    private val secondOpenTelemetry = TestOpenTelemetry(Local)
    private val app = carbonIntensity(
        NationalGridCloud(nationalGridFake, openTelemetry),
        OctopusCloud(octopusFake),
        LimitCalculatorCloud(limitCalculatorApp(secondOpenTelemetry), openTelemetry),
        WeightsCalculatorCloud(weightedCalculatorApp()),
        openTelemetry
    )

    @Test
    fun `can observe a customer journey retrieving an intensity limited charge time`(testInfo: TestInfo) {
        nationalGridFake.setDateData(
            ZonedDateTime.parse("2025-04-10T09:00:00Z"),
            listOf(101, 101, 101, 101, 101, 101, 100, 99, 100, 100, 100, 100, 100, 100, 90, 90)
        )
        octopusFake.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2025-04-10T09:00:00Z"),
            listOf(9.8, 9.8, 10.0, 10.0, 9.5, 9.5, 10.0, 10.0, 10.0, 10.0, 10.0, 9.0, 9.0, 10.0, 9.8, 9.8)
        )
        val request = Request(POST, "/octopus/charge-time").body("""{
            "product":"AGILE-24-10-01",
            "tariff":"E-1R-AGILE-24-10-01-A",
            "start":"2025-04-10T09:00:00Z",
            "end":"2025-04-10T17:00:00Z",
            "time":60,
            "intensityLimit":100
        }""".trimMargin())

        app(request)

        openTelemetry.approveSpanDiagram(testInfo.displayName, secondOpenTelemetry)
    }
}
