package com.intensity.observability

import com.intensity.observability.TestProfile.Local
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ManagedOpenTelemetryTest {
    private val testOpenTelemetry = TestOpenTelemetry(Local)
    private val openTelemetry = ManagedOpenTelemetry(testOpenTelemetry, "test-service")

    @AfterEach
    fun tearDown() {
        testOpenTelemetry.shutdown()
    }

    @Test
    fun `creates a span with the provided name`() {
        openTelemetry.span("testSpan").end()

        assertThat(testOpenTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = testOpenTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.instrumentationName, equalTo("http4k"))
    }

    @Test
    fun `can add an event to a span`() {
        val span = openTelemetry.span("testSpan")
        span.addEvent("test-event")
        span.end()

        val spanData = testOpenTelemetry.spans().first()
        assertThat(spanData.events, equalTo(listOf(SpanEvent("test-event"))))
    }

    @Test
    fun `sets a span as the current active span`() {
        val span = openTelemetry.span("testSpan")
        span.makeCurrent()
        val childSpan = openTelemetry.span("childSpan")
        childSpan.end()
        span.end()

        val parentSpanData = testOpenTelemetry.spans().first { it.name == "testSpan" }
        val childSpanData = testOpenTelemetry.spans().first { it.name == "childSpan" }
        assertThat(childSpanData.parentSpanId, equalTo(parentSpanData.spanId))
    }

    @Test
    fun `traces an http request`() {
        openTelemetry.trace("http-request", "other-service").then { Response(OK) }(Request(GET, "/test/path"))

        assertThat(testOpenTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = testOpenTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.attributes["http.target"], equalTo("other-service"))
        assertThat(spanData.attributes["http.path"], equalTo("/test/path"))
        assertThat(spanData.attributes["http.status"], equalTo(200L))
        assertThat(spanData.instrumentationName, equalTo("http4k"))
    }

    @Test
    fun `propagates trace context over http`() {
        val span = openTelemetry.span("to be propagated").also { it.makeCurrent() }
        var sentRequest: Request? = null
        openTelemetry.propagateTrace().then { request ->
            sentRequest = request
            Response(OK)
        }(Request(GET, "/test/path/propagated"))
        span.end()

        assertThat(sentRequest!!, hasHeader("traceparent"))
    }
}
