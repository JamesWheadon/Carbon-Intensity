package com.intensity.observability

import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

class TestObservability {
    private val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(InMemorySpanExporter.create()))
        .build()
    private val metricReader = InMemoryMetricReader.builder().build()
    private val openTelemetry = OpenTelemetrySdk
        .builder()
        .setTracerProvider(tracerProvider)
        .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metricReader).build())
        .build()
    private val logging = TestLogging()

    fun observability(serviceName: String): Observability {
        val openTelemetry = openTelemetry
        return Observability(
            OpenTelemetryTracer(openTelemetry, serviceName),
            Metrics(openTelemetry, serviceName),
            logging
        )
    }
}
