package com.learning

import com.learning.Matchers.assertReturnsString
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class EndToEndTest {
    private val client = JavaHttpClient()
    private val server = carbonIntensityServer(1000)
    private val chargeTimeBody = """{"startTime": "2024-09-30T19:55:00"}"""
    private val chargeTimeResponse = """{"chargeTime": "2024-09-30T21:00:00"}"""

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
        val response = client(Request(POST, "http://localhost:${server.port()}/charge-time").body(chargeTimeBody))

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(chargeTimeResponse))
    }
}
