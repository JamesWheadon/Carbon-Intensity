package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import org.http4k.metrics.Http4kOpenTelemetry

class ManagedOpenTelemetry(private val openTelemetry: OpenTelemetry) {
    private val spans: MutableList<Span> = mutableListOf()

    fun startSpan(spanName: String) {
        openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME).spanBuilder(spanName)
            .startSpan().also { spans.add(it) }
    }

    fun endAllSpans() {
        spans.forEach { it.end() }
    }
}
