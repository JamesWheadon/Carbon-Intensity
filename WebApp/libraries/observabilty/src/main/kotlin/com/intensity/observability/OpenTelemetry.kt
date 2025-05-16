package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import org.http4k.metrics.Http4kOpenTelemetry

class ManagedOpenTelemetry(private val openTelemetry: OpenTelemetry, private val serviceName: String) {
    fun span(spanName: String): Span =
        openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME)
            .spanBuilder(spanName)
            .setAttribute("service.name", serviceName)
            .startSpan()
}
