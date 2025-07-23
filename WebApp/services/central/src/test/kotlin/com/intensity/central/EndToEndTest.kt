package com.intensity.central

import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGrid
import com.intensity.observability.TestObservability
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.Octopus
import org.http4k.routing.reverseProxyRouting
import java.time.ZonedDateTime

abstract class EndToEndTest {
    val time: ZonedDateTime = ZonedDateTime.parse("2025-03-25T12:00:00Z")
    val octopus = FakeOctopus()
    val nationalGrid = FakeNationalGrid()
    val limitCalculator = FakeLimitCalculator()
    val weightsCalculator = FakeWeightsCalculator()
    private val observability = TestObservability().observability("central")
    private val network = reverseProxyRouting(
        NationalGrid.pathSegment to nationalGrid,
        Octopus.pathSegment to octopus,
        LimitCalculator.pathSegment to limitCalculator,
        WeightsCalculator.pathSegment to weightsCalculator
    )
    val app = carbonIntensity(
        network,
        observability
    )

    fun getErrorResponse(message: String) = """{"error":"$message"}"""
}
