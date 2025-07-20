package com.intensity.observability

import com.intensity.observability.SpanData.SpanEvent
import com.intensity.observability.SpanData.Status.Error
import com.intensity.observability.SpanData.Status.Unset
import com.intensity.observability.SpanData.Type.Client
import com.intensity.observability.SpanData.Type.Internal
import com.intensity.observability.SpanData.Type.Server
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.stream.Stream

class OpenTelemetryTracerTest {
    private val openTelemetry = TestOpenTelemetry()
    private val tracer = OpenTelemetryTracer(openTelemetry, "test-service")

    companion object {
        @JvmStatic
        private fun basePaths(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("https://fake-base-path", 443L),
                Arguments.of("http://fake-base-path", 80L),
                Arguments.of("http://localhost:8080/fake-base-path", 8080L)
            )
        }
    }

    @AfterEach
    fun tearDown() {
        openTelemetry.shutdown()
    }

    @Test
    fun `creates a span with the provided name`() {
        tracer.span("testSpan") { }

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.instrumentationName, equalTo("com.intensity.observability"))
        assertThat(spanData.type, equalTo(Internal))
        assertThat(spanData.status, equalTo(Unset))
    }

    @Test
    fun `can add an event to a span`() {
        tracer.span("testSpan") { span ->
            span.addEvent("test-event")
        }

        val spanData = openTelemetry.spans().first()
        assertThat(spanData.events, equalTo(listOf(SpanEvent("test-event"))))
    }

    @Test
    fun `marks a span as an error if an exception thrown`() {
        assertThrows<RuntimeException> {
            tracer.span("testSpan") {
                throw RuntimeException()
            }
        }

        val spanData = openTelemetry.spans().first()
        assertThat(spanData.status, equalTo(Error))
    }

    @Test
    fun `sets a span as the current active span and removes it when ended`() {
        tracer.span("testSpan") {
            tracer.span("firstChildSpan") { }
            tracer.span("secondChildSpan") { }
        }

        val parentSpanData = openTelemetry.spans().first { it.name == "testSpan" }
        val firstChildSpanData = openTelemetry.spans().first { it.name == "firstChildSpan" }
        val secondChildSpanData = openTelemetry.spans().first { it.name == "secondChildSpan" }
        assertThat(firstChildSpanData.parentSpanId, equalTo(parentSpanData.spanId))
        assertThat(secondChildSpanData.parentSpanId, equalTo(parentSpanData.spanId))
    }

    @Test
    fun `traces an outbound http request`() {
        tracer.outboundHttp("http-request", "other-service").then { Response(OK) }(
            Request(GET, "https://fake-base-path/test/path")
        )

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.attributes["http.target"], equalTo("other-service"))
        assertThat(spanData.attributes["http.path"], equalTo("/test/path"))
        assertThat(spanData.attributes["http.request.method"], equalTo("GET"))
        assertThat(spanData.attributes["url.full"], equalTo("https://fake-base-path/test/path"))
        assertThat(spanData.attributes["server.address"], equalTo("fake-base-path"))
        assertThat(spanData.attributes["server.port"], equalTo(443L))
        assertThat(spanData.attributes["http.response.status_code"], equalTo(200L))
        assertThat(spanData.instrumentationName, equalTo("com.intensity.observability"))
        assertThat(spanData.type, equalTo(Client))
        assertThat(spanData.status, equalTo(Unset))
    }

    @ParameterizedTest
    @MethodSource("basePaths")
    fun `adds the correct port attribute value for a trace`(basePath: String, port: Long) {
        tracer.outboundHttp("http-request", "other-service").then { Response(OK) }(
            Request(
                GET,
                "$basePath/test/path"
            )
        )

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.attributes["server.port"], equalTo(port))
    }

    @Test
    fun `sets client http trace as an error on a 4XX or 5XX response`() {
        tracer.outboundHttp("http-request", "other-service").then { Response(INTERNAL_SERVER_ERROR) }(
            Request(GET, "https://fake-base-path/test/path")
        )

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.status, equalTo(Error))
    }

    @Test
    fun `traces an inbound http request`() {
        tracer.inboundHttp("http-request").then { Response(OK) }(
            Request(GET, "https://fake-base-path/test/path")
        )

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.attributes["http.request.method"], equalTo("GET"))
        assertThat(spanData.attributes["url.path"], equalTo("/test/path"))
        assertThat(spanData.attributes["url.scheme"], equalTo("https"))
        assertThat(spanData.attributes["http.response.status_code"], equalTo(200L))
        assertThat(spanData.instrumentationName, equalTo("com.intensity.observability"))
        assertThat(spanData.type, equalTo(Server))
        assertThat(spanData.status, equalTo(Unset))
    }

    @Test
    fun `sets inbound http trace as an error on a 4XX or 5XX response`() {
        tracer.inboundHttp("http-request").then { Response(NOT_FOUND) }(
            Request(GET, "https://fake-base-path/test/path")
        )

        assertThat(openTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = openTelemetry.spans().first()
        assertThat(spanData.status, equalTo(Error))
    }

    @Test
    fun `propagates trace context over http`() {
        var sentRequest: Request? = null
        tracer.outboundHttp("to be propagated", "test-target").then { request ->
            sentRequest = request
            Response(OK)
        }(Request(GET, "/test/path/propagated"))

        assertThat(sentRequest!!, hasHeader("traceparent"))
    }

    @Test
    fun `receives trace context over http`(testInfo: TestInfo) {
        val executor = Executors.newSingleThreadExecutor()

        tracer.outboundHttp("starting span", "test-target").then { request ->
            executor.submit(Callable {
                tracer.inboundHttp("received span").then {
                    Response(OK)
                }(request)
            }).get()
        }(Request(GET, "/test/path/propagated"))

        val receivedSpanData = openTelemetry.spans().first { it.name == "received span" }
        val sentSpanData = openTelemetry.spans().first { it.name == "starting span" }
        assertThat(receivedSpanData.parentSpanId, equalTo(sentSpanData.spanId))
    }
}