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
import org.junit.jupiter.api.TestInfo

class TracingOpenTelemetryTest {
    private val openTelemetry = TestTracingOpenTelemetry(Local, "test-service")

    @AfterEach
    fun tearDown() {
        openTelemetry.shutdown()
    }

    @Test
    fun `creates a span with the provided name`() {
        openTelemetry.span("testSpan").end()

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.instrumentationName, equalTo("com.intensity.observability"))
    }

    @Test
    fun `can add an event to a span`() {
        val span = openTelemetry.span("testSpan")
        span.addEvent("test-event")
        openTelemetry.end(span)

        val spanData = openTelemetry.spans().first()
        assertThat(spanData.events, equalTo(listOf(SpanEvent("test-event"))))
    }

    @Test
    fun `sets a span as the current active span and removes it when ended`() {
        val span = openTelemetry.span("testSpan")
        val firstChildSpan = openTelemetry.span("firstChildSpan")
        openTelemetry.end(firstChildSpan)
        val secondChildSpan = openTelemetry.span("secondChildSpan")
        openTelemetry.end(secondChildSpan)
        openTelemetry.end(span)

        val parentSpanData = openTelemetry.spans().first { it.name == "testSpan" }
        val firstChildSpanData = openTelemetry.spans().first { it.name == "firstChildSpan" }
        val secondChildSpanData = openTelemetry.spans().first { it.name == "secondChildSpan" }
        assertThat(firstChildSpanData.parentSpanId, equalTo(parentSpanData.spanId))
        assertThat(secondChildSpanData.parentSpanId, equalTo(parentSpanData.spanId))
    }

    @Test
    fun `traces an http request`() {
        openTelemetry.trace("http-request", "other-service").then { Response(OK) }(Request(GET, "/test/path"))

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.attributes["http.target"], equalTo("other-service"))
        assertThat(spanData.attributes["http.method"], equalTo("GET"))
        assertThat(spanData.attributes["http.path"], equalTo("/test/path"))
        assertThat(spanData.attributes["http.status"], equalTo(200L))
        assertThat(spanData.instrumentationName, equalTo("com.intensity.observability"))
    }

    @Test
    fun `propagates trace context over http`() {
        val span = openTelemetry.span("to be propagated")
        var sentRequest: Request? = null
        openTelemetry.propagateTrace().then { request ->
            sentRequest = request
            Response(OK)
        }(Request(GET, "/test/path/propagated"))
        openTelemetry.end(span)

        assertThat(sentRequest!!, hasHeader("traceparent"))
    }

    @Test
    fun `receives trace context over http`(testInfo: TestInfo) {
        val callerOpenTelemetry = TestTracingOpenTelemetry(Local, "other-service")
        val startSpan = callerOpenTelemetry.span("starting span")
        callerOpenTelemetry.propagateTrace().then(openTelemetry.receiveTrace()).then {
            val span = openTelemetry.span("received span")
            openTelemetry.end(span)
            Response(OK)
        }(Request(GET, "/test/path/propagated"))
        openTelemetry.end(startSpan)

        val receivedSpanData = openTelemetry.spans().first { it.name == "received span" }
        val sentSpanData = callerOpenTelemetry.spans().first { it.name == "starting span" }
        assertThat(receivedSpanData.parentSpanId, equalTo(sentSpanData.spanId))
    }
}
