package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import org.http4k.metrics.Http4kOpenTelemetry

class ManagedOpenTelemetry(private val openTelemetry: OpenTelemetry, private val serviceName: String) {
    fun span(spanName: String): ManagedSpan {
        val span = openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME)
            .spanBuilder(spanName)
            .setAttribute("service.name", serviceName)
            .startSpan()
        return ManagedSpan(span)
    }
}

class ManagedSpan(private val span: Span) {
    fun end() {
        span.end()
    }

    fun addEvent(eventName: String) {
        span.addEvent(eventName)
    }

    fun makeCurrent() {
        span.makeCurrent()
    }
}
