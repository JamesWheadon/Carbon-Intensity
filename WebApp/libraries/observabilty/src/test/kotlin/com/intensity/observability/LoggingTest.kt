package com.intensity.observability

import com.intensity.observability.Severity.Error
import com.intensity.observability.Severity.Info
import com.intensity.observability.TestProfile.Local
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

class LoggingTest {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()
    private val testLogging = TestLogging()
    private val tracing = TestTracingOpenTelemetry(Local, "logging-test")

    @BeforeEach
    fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(standardOut)
    }

    @Test
    fun `prints a log message to stdout as json`() {
        logToStandardOut()(TestLogEvent("Info log message"))

        assertThat(outputStreamCaptor.toString(Charset.defaultCharset()), containsSubstring(""""type":"TestLogEvent""""))
    }

    @Test
    fun `sets a log level`() {
        tracing.span("test") {
            testLogging(TestLogEvent("Info log message"))
        }

        val logs = testLogging.logs()
        assertThat(logs, hasSize(equalTo(1)))
        assertThat(logs.first().severity, equalTo(Info))
        assertThat(logs.first().message, equalTo("Info log message"))
        assertThat(logs.first().span, equalTo(tracing.spans().first().spanId))
        assertThat(logs.first().trace, equalTo(tracing.spans().first().traceId))
    }

    @Test
    fun `adds trace and span information to a log`() {
        tracing.span("test") {
            testLogging(TestErrorLogEvent("Error log message"))
        }

        val logs = testLogging.logs()
        assertThat(logs.first().severity, equalTo(Error))
        assertThat(logs.first().message, equalTo("Error log message"))
    }
}

data class TestLogEvent(override val message: String) : LogEvent
data class TestErrorLogEvent(override val message: String) : ErrorLogEvent
