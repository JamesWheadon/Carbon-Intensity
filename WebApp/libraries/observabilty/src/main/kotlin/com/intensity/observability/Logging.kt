package com.intensity.observability

import org.http4k.events.Event
import org.http4k.format.Jackson.asJsonObject

internal fun interface LogOutput: (LogEvent) -> Unit

internal fun logToStandardOut(): LogOutput = LogOutput { log ->
    println(log.asLogMessage())
}

interface LogEvent : Event

internal data class LogMessage(
    val type: String
)

private fun LogEvent.asLogMessage() = LogMessage(javaClass.simpleName).asJsonObject()
