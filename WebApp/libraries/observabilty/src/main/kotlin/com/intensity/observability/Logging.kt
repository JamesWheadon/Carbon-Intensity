package com.intensity.observability

import io.opentelemetry.api.trace.Span
import org.http4k.events.Event
import org.http4k.format.Jackson.asJsonObject

internal fun interface LogOutput: (LogEvent) -> Unit

internal fun logToStandardOut(): LogOutput = LogOutput { log ->
    println(log.asLogMessage().asJsonObject())
}

interface LogEvent : Event

data class LogMessage(
    val type: String,
    val trace: TraceId,
    val span: SpanId
)

internal fun LogEvent.asLogMessage(): LogMessage {
    val currentSpanContext = Span.current().spanContext
    return LogMessage(
        javaClass.simpleName,
        TraceId(currentSpanContext.traceId),
        SpanId(currentSpanContext.spanId)
    )
}

@JvmInline
value class TraceId(val value: String)

@JvmInline
value class SpanId(val value: String)
