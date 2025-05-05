package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import org.http4k.metrics.Http4kOpenTelemetry

class ManagedOpenTelemetry(private val openTelemetry: OpenTelemetry) {
    private val spans: MutableList<ManagedSpan> = mutableListOf()

    fun startSpan(spanName: String) {
        openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME).spanBuilder(spanName)
            .startSpan().also { spans.add(ManagedSpan(spanName, it)) }
    }

    fun endSpan(spanName: String) {
        spans.removeFirst { it.name == spanName }.span.end()
    }

    fun endAllSpans() {
        spans.forEach { it.span.end() }
    }
}

data class ManagedSpan(val name: String, val span: Span)

fun <T> MutableList<T>.removeFirst(predicate: (T) -> Boolean): T =
    if (isEmpty()) {
        throw NoSuchElementException("List is empty.")
    } else {
        removeAt(this.indexOfFirst(predicate))
    }
