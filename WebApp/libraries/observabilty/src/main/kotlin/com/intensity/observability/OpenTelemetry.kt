package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import org.http4k.core.Filter
import org.http4k.metrics.Http4kOpenTelemetry

class ManagedOpenTelemetry(private val openTelemetry: OpenTelemetry, private val serviceName: String) {
    fun span(spanName: String): ManagedSpan {
        val span = openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME)
            .spanBuilder(spanName)
            .setAttribute("service.name", serviceName)
            .startSpan()
        return ManagedSpan(span)
    }

    fun trace(spanName: String, targetName: String): Filter {
        return Filter { next ->
            { request ->
                val span = openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME)
                    .spanBuilder(spanName)
                    .setAttribute("service.name", serviceName)
                    .setAttribute("http.target", targetName)
                    .setAttribute("http.path", request.uri.path)
                    .startSpan()
                next(request).also { response ->
                    span.setAttribute("http.status", response.status.code.toLong())
                    span.end()
                }
            }
        }
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
