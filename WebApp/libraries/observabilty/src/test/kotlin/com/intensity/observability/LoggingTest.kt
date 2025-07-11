package com.intensity.observability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

class LoggingTest {
    private val standardOut = System.out
    private val outputStreamCaptor: ByteArrayOutputStream = ByteArrayOutputStream()

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
        logToStandardOut()(TestLogEvent)

        assertThat(outputStreamCaptor.toString(Charset.defaultCharset()), containsSubstring("""{"type":"TestLogEvent"}"""))
    }
}

data object TestLogEvent : LogEvent
