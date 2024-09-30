package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class EndToEndTest {
    private val client = JavaHttpClient()
    private val server = carbonIntensityServer(
        1000, PythonScheduler(
            FakeScheduler(
                mapOf(
                    Instant.parse("2024-09-30T19:55:00Z") to Instant.parse("2024-09-30T21:00:00Z"),
                    Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z")
                )
            )
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
    fun `responds with optimal charge time`() {
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-30T19:55:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T21:00:00")))
    }

    @Test
    fun `responds with optimal charge time using the scheduler service`() {
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-10-01T02:30:00")))
    }

    private fun getChargeTimeBody(startTimestamp: String) = """{"startTime":"$startTimestamp"}"""
    private fun getChargeTimeResponse(chargeTimestamp: String) = """{"chargeTime":"$chargeTimestamp"}"""
}
