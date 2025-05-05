package com.intensity.observability

import com.intensity.observability.TestOpenTelemetry.Companion.TestProfile.Local
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ManagedOpenTelemetryTest {
    private val testOpenTelemetry = TestOpenTelemetry(Local)
    private val openTelemetry = ManagedOpenTelemetry(testOpenTelemetry)

    @AfterEach
    fun tearDown() {
        testOpenTelemetry.shutdown()
    }

    @Test
    fun `creates a span with the provided name`() {
        openTelemetry.startSpan("testSpan")
        openTelemetry.endAllSpans()

        assertThat(testOpenTelemetry.spanNames(), equalTo(listOf("testSpan")))
    }

    @Test
    fun `a span can be ended independently`() {
        openTelemetry.startSpan("testSpan")
        openTelemetry.startSpan("spanToNotEnd")
        openTelemetry.endSpan("testSpan")

        assertThat(testOpenTelemetry.spanNames(), equalTo(listOf("testSpan")))
    }
}
