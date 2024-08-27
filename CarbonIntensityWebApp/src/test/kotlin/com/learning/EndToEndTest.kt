package com.learning

import com.learning.Matchers.assertReturnsString
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EndToEndTest {
    private val recorderPort = 1000
    private val client = JavaHttpClient()
    private val server = carbonIntensityServer(0, Uri.of("http://localhost:$recorderPort"))
    private val recorder = FakeRecorderHttp()
    private val recorderServer = recorder.asServer(SunHttp(recorderPort))

    @BeforeEach
    fun setup() {
        server.start()
        recorderServer.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop()
        recorderServer.stop()
    }

    @Test
    fun `responds to ping`() {
        client(Request(GET, "http://localhost:${server.port()}/ping")).assertReturnsString("pong")
    }

    @Test
    fun `responds to add endpoint`() {
        client(Request(GET, "http://localhost:${server.port()}/add?value=1&value=2")).assertReturnsString("3")
        assertThat(recorder.calls, equalTo(listOf(3)))
    }

    @Test
    fun `responds to multiply endpoint`() {
        client(Request(GET, "http://localhost:${server.port()}/multiply?value=4&value=2")).assertReturnsString("8")
        assertThat(recorder.calls, equalTo(listOf(8)))
    }
}
