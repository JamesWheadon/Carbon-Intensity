package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("unused")
class TestTracingOpenTelemetry(profile: TestProfile, serviceName: String): ManagedOpenTelemetry {
    private val testOpenTelemetry = TestOpenTelemetry(profile)
    private val openTelemetry = TracingOpenTelemetry(testOpenTelemetry, serviceName)

    override fun span(spanName: String) = openTelemetry.span(spanName)

    override fun trace(spanName: String, targetName: String) = openTelemetry.trace(spanName, targetName)

    override fun propagateTrace() = openTelemetry.propagateTrace()

    override fun receiveTrace() = openTelemetry.receiveTrace()

    fun spans() = testOpenTelemetry.spans()

    fun spans(vararg telemetry: TestTracingOpenTelemetry): List<SpanData> = testOpenTelemetry.spans().plus(telemetry.flatMap { it.spans() } )

    fun spanNames(): List<String> = spans().map { it.name }

    fun spanNames(vararg telemetry: TestTracingOpenTelemetry): List<String> = spans(*telemetry).map { it.name }

    fun approveSpanDiagram(testName: String) {
        val spans = spans().toMutableList()
        testOpenTelemetry.approveSpanDiagram(spans, testName)
    }

    fun approveSpanDiagram(testName: String, vararg telemetry: TestTracingOpenTelemetry) {
        val spans = spans(*telemetry).toMutableList()
        testOpenTelemetry.approveSpanDiagram(spans, testName)
    }

    fun shutdown() {
        testOpenTelemetry.shutdown()
    }
}

@Suppress("unused")
class TestOpenTelemetry(profile: TestProfile) : OpenTelemetry {
    private val serviceNameResource =
        Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, "otel-jaeger-example-kotlin"))
    private val inMemorySpanExporter = InMemorySpanExporter.create()
    private val jaegerOtlpExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint("http://localhost:4317")
        .setTimeout(30, TimeUnit.SECONDS)
        .build()
    private val tracerProvider = if (profile == TestProfile.Jaeger) {
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
    private val openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()

    override fun getTracerProvider(): TracerProvider = openTelemetry.tracerProvider

    override fun getPropagators(): ContextPropagators = openTelemetry.propagators

    fun spans(): List<SpanData> = inMemorySpanExporter.finishedSpanItems.map { span ->
        SpanData(
            span.name,
            span.spanId,
            span.parentSpanId,
            span.attributes.asMap().mapKeys { key -> key.key.key },
            span.events.map { SpanEvent(it.name) },
            span.instrumentationScopeInfo.name
        )
    }

    fun spans(vararg telemetry: TestOpenTelemetry): List<SpanData> = spans().plus(telemetry.flatMap { it.spans() } )

    fun spanNames(): List<String> = spans().map { it.name }

    fun spanNames(vararg telemetry: TestOpenTelemetry): List<String> = spans(*telemetry).map { it.name }

    fun shutdown() {
        openTelemetry.shutdown()
    }

    fun approveSpanDiagram(testName: String) {
        val spans = spans().toMutableList()
        approveSpanDiagram(spans, testName)
    }

    fun approveSpanDiagram(testName: String, vararg telemetry: TestOpenTelemetry) {
        val spans = spans(*telemetry).toMutableList()
        approveSpanDiagram(spans, testName)
    }

    fun approveSpanDiagram(spans: MutableList<SpanData>, testName: String) {
        val spanTree = mutableMapOf<SpanData, MutableList<SpanData>>()
        val roots = mutableListOf<SpanData>()
        spans.filter { it.parentSpanId == "0000000000000000" }.forEach {
            spanTree[it] = mutableListOf()
            spans.remove(it)
            roots.add(it)
        }
        while (spans.isNotEmpty()) {
            spans.filter { span -> span.parentSpanId in spanTree.keys.map { it.spanId } }
                .forEach { span ->
                    spanTree[spanTree.keys.first { it.spanId == span.parentSpanId }]!!.add(span)
                    spanTree[span] = mutableListOf()
                    spans.remove(span)
                }
        }
        val spanDiagram = roots.joinToString("\n") { it.toTreeNode(spanTree).toString() }
        val fileName = testName.substring(0, testName.indexOf("(TestInfo")).replace(" ", "-")
        val directory = "../generated/$fileName"
        val approvedOutput = File("$directory/span-tree.txt")

        if (approvedOutput.exists()) {
            val approvedText = approvedOutput.readText()
            if (approvedText == spanDiagram) {
                return
            } else {
                File("$directory/span-tree-actual.txt").writeText(spanDiagram)
            }
        } else {
            File("../generated").mkdir()
            File(directory).mkdir()
            File("$directory/span-tree-actual.txt").writeText(spanDiagram)
        }
        throw AssertionError("Span diagram is not approved")
    }
}

enum class TestProfile {
    Local,
    Jaeger
}

private fun SpanData.toTreeNode(spanTree: MutableMap<SpanData, MutableList<SpanData>>): TreeNode {
    return TreeNode(this.name, spanTree[this]?.map { it.toTreeNode(spanTree) } ?: emptyList())
}

class TreeNode(private val name: String, private val children: List<TreeNode>) {
    override fun toString(): String {
        val buffer = StringBuilder(50)
        print(buffer, "", "")
        return buffer.toString()
    }

    private fun print(buffer: StringBuilder, prefix: String, childrenPrefix: String) {
        buffer.append(prefix)
        buffer.append(name)
        buffer.append('\n')
        val it = children.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (it.hasNext()) {
                next.print(buffer, "$childrenPrefix├── ", "$childrenPrefix│   ")
            } else {
                next.print(buffer, "$childrenPrefix└── ", "$childrenPrefix    ")
            }
        }
    }
}

data class SpanData(val name: String, val spanId: String, val parentSpanId: String, val attributes: Map<String, Any>, val events: List<SpanEvent>, val instrumentationName: String)
data class SpanEvent(val name: String)
