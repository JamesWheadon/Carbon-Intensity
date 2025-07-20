package com.intensity.observability

import com.intensity.observability.SpanData.SpanEvent
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.StatusData.unset
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD
import io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import org.http4k.core.Status

class TestOpenTelemetry : OpenTelemetry {
    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build()
    private val metricReader = InMemoryMetricReader.builder().build()
    private val openTelemetry = OpenTelemetrySdk
        .builder()
        .setTracerProvider(tracerProvider)
        .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metricReader).build())
        .build()

    override fun getTracerProvider(): TracerProvider = openTelemetry.tracerProvider

    override fun getPropagators(): ContextPropagators = openTelemetry.propagators

    fun spans(): List<SpanData> = this.spanExporter.finishedSpanItems.map { span ->
        SpanData(
            span.name,
            TraceId(span.traceId),
            SpanId(span.spanId),
            SpanId(span.parentSpanId),
            span.attributes.asMap().mapKeys { key -> key.key.key },
            span.events.map { SpanEvent(it.name) },
            span.status(),
            span.type(),
            span.instrumentationScopeInfo.name,
            span.startEpochNanos
        )
    }

    fun shutdown() {
        openTelemetry.shutdown()
    }
}

data class SpanData(
    val name: String,
    val traceId: TraceId,
    val spanId: SpanId,
    val parentSpanId: SpanId,
    val attributes: Map<String, Any>,
    val events: List<SpanEvent>,
    val status: Status,
    val type: Type,
    val instrumentationName: String,
    val startEpochNanos: Long
) {
    enum class Status {
        Unset,
        Error
    }

    enum class Type {
        Internal,
        Client,
        Server
    }

    data class SpanEvent(val name: String)

    fun toHttpSpan() = HttpSpan(
        this.attributes[SERVICE_NAME.key]!! as String,
        this.attributes["http.target"]!! as String,
        this.attributes[HTTP_REQUEST_METHOD.key]!! as String,
        this.attributes["http.path"]!! as String,
        org.http4k.core.Status.fromCode((this.attributes[HTTP_RESPONSE_STATUS_CODE.key] as Long).toInt())!!
    )

    fun toTreeNode(spans: List<SpanData>): TreeNode {
        return TreeNode(this, spans.filter { it.parentSpanId == spanId }.map { it.toTreeNode(spans) })
    }
}

data class HttpSpan(
    val caller: String,
    val target: String,
    val method: String,
    val path: String,
    val status: Status
)

fun io.opentelemetry.sdk.trace.data.SpanData.status(): SpanData.Status =
    when(status) {
        unset() -> SpanData.Status.Unset
        else -> SpanData.Status.Error
    }

fun io.opentelemetry.sdk.trace.data.SpanData.type(): SpanData.Type =
    when(kind) {
        SpanKind.CLIENT -> SpanData.Type.Client
        SpanKind.SERVER -> SpanData.Type.Server
        else -> SpanData.Type.Internal
    }
