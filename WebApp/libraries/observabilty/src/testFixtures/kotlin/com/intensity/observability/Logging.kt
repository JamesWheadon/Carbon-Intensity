package com.intensity.observability

class TestLogging: LogOutput {
    private val logs = mutableListOf<LogMessage>()

    override fun invoke(logEvent: LogEvent) {
        logs.add(logEvent.asLogMessage())
    }

    fun logs() = logs.toList()
}
