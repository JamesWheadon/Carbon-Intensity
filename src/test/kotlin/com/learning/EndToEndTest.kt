package com.learning

import com.learning.Matchers.assertReturnsString
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    @Test
    fun `responds to multiply endpoint`() {
        client(Request(GET, "http://localhost:${server.port()}/multiply?value=1&value=2")).assertReturnsString("8")
    }
}
