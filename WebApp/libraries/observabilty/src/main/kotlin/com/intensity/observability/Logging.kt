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
    val span: SpanId
)

internal fun LogEvent.asLogMessage() = LogMessage(javaClass.simpleName, SpanId(Span.current().spanContext.spanId))

@JvmInline
value class SpanId(val value: String)
