package com.intensity.central

import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import io.opentelemetry.api.OpenTelemetry
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.hasApprovedContent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class OpenApiTest {
    private val client = JavaHttpClient()
    private val server = carbonIntensityServer(
        1000,
        NationalGridCloud(
            FakeNationalGrid()
        ),
        OctopusCloud(
            FakeOctopus()
        ),
        LimitCalculatorCloud(
            FakeLimitCalculator(),
            OpenTelemetry.noop()
        ),
        WeightsCalculatorCloud(
            FakeWeightsCalculator()
        )
    )

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `openapi document renders`(approver: Approver) {
        val response = client(
            Request(GET, "http://localhost:${server.port()}/openapi.json")
        )

        assertThat(response, hasStatus(OK).and(approver.hasApprovedContent()))
    }
}
