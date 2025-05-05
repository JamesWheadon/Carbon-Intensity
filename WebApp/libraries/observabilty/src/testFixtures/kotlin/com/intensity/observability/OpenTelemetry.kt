package com.intensity.observability

import com.intensity.observability.TestOpenTelemetry.Companion.TestProfile.Jaeger
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import java.util.concurrent.TimeUnit

class TestOpenTelemetry(profile: TestProfile) : OpenTelemetry {
    private val serviceNameResource =
        Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, "otel-jaeger-example-kotlin"))
    private val inMemorySpanExporter = InMemorySpanExporter.create()
    private val jaegerOtlpExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint("http://localhost:4317")
        .setTimeout(30, TimeUnit.SECONDS)
        .build()
    private val tracerProvider = if (profile == Jaeger) {
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
            .addSpanProcessor(SimpleSpanProcessor.create(jaegerOtlpExporter))
            .setResource(Resource.getDefault().merge(serviceNameResource))
            .build()
    } else {
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
            .build()
    }
    private val openTelemetry =
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()


    override fun getTracerProvider(): TracerProvider = openTelemetry.tracerProvider

    override fun getPropagators(): ContextPropagators = openTelemetry.propagators

    fun spans(): List<SpanData> = inMemorySpanExporter.finishedSpanItems

    fun shutdown() {
        openTelemetry.shutdown()
    }

    @Suppress("unused")
    companion object {
        enum class TestProfile {
            Local,
            Jaeger
        }
    }
}
