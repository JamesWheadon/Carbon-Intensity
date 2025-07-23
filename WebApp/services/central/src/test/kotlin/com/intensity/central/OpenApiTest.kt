package com.intensity.central

import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.observability.Observability
import com.intensity.octopus.FakeOctopus
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.reverseProxyRouting
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.hasApprovedContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class OpenApiTest {
    private val observability = Observability.noOp()

    private val network = reverseProxyRouting(
        "grid" to FakeNationalGrid(),
        "octopus" to FakeOctopus(),
        "limit" to FakeLimitCalculator(),
        "weights" to FakeWeightsCalculator()
    )
    private val server = carbonIntensity(network, observability)

    @Test
    fun `openapi document renders`(approver: Approver) {
        val response = server(Request(GET, "/openapi.json"))

        assertThat(response, hasStatus(OK).and(approver.hasApprovedContent()))
    }
}
