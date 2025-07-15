package com.intensity.observability

import com.intensity.observability.Severity.Error
import com.intensity.observability.Severity.Info
import io.opentelemetry.api.trace.Span
import org.http4k.events.Event
import org.http4k.format.Jackson.asJsonObject

internal fun interface LogOutput: (LogEvent) -> Unit

internal fun logToStandardOut(): LogOutput = LogOutput { log ->
    println(log.asLogMessage().asJsonObject())
}

interface LogEvent : Event
interface ErrorLogEvent : LogEvent

data class LogMessage(
    val type: String,
    val severity: Severity,
    val trace: TraceId,
    val span: SpanId
)

internal fun LogEvent.asLogMessage(): LogMessage {
    val currentSpanContext = Span.current().spanContext
    val severity = when (this) {
        is ErrorLogEvent -> Error
        else -> Info
    }
    return LogMessage(
        javaClass.simpleName,
        severity,
        TraceId(currentSpanContext.traceId),
        SpanId(currentSpanContext.spanId)
    )
}

enum class Severity {
    Info,
    Error
}

@JvmInline
value class TraceId(val value: String)

@JvmInline
value class SpanId(val value: String)
