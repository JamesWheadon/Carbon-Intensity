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
}