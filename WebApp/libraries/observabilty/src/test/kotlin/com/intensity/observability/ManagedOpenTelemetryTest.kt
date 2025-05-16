package com.intensity.observability

import com.intensity.observability.TestOpenTelemetry.Companion.TestProfile.Local
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ManagedOpenTelemetryTest {
    private val testOpenTelemetry = TestOpenTelemetry(Local)
    private val openTelemetry = ManagedOpenTelemetry(testOpenTelemetry, "test-service")

    @AfterEach
    fun tearDown() {
        testOpenTelemetry.shutdown()
    }

    @Test
    fun `creates a span with the provided name`() {
        openTelemetry.span("testSpan").end()

        assertThat(testOpenTelemetry.spans(), hasSize(equalTo(1)))
        val spanData = testOpenTelemetry.spans().first()
        assertThat(spanData.attributes["service.name"], equalTo("test-service"))
        assertThat(spanData.instrumentationName, equalTo("http4k"))
    }
}
