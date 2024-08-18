package com.learning

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus

class EndToEndTest {
    private val client = JavaHttpClient()
    private val server = carbonIntensityServer(0)

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:${server.port()}/ping")).assertReturnsString("pong")
    }

    @Test
    fun `responds to add endpoint`() {
        client(Request(GET, "http://localhost:${server.port()}/add?value=1&value=2")).assertReturnsString("3")
    }
}

private fun Response.assertReturnsString(expected: String) {
    assertThat(this, hasStatus(OK).and(hasBody(expected)))
}
