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
import java.io.File
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

    fun spanNames(): List<String> = inMemorySpanExporter.finishedSpanItems.map { it.name }

    fun shutdown() {
        openTelemetry.shutdown()
    }

    fun spanDiagram(testName: String) {
        val spanTree = mutableMapOf<SpanData, MutableList<SpanData>>()
        val spans = spans().toMutableList()
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
        File("../generated").mkdir()
        val directory = "../generated/${testName.removeSuffix("(TestInfo)").replace(" ", "-")}"
        File(directory).mkdir()
        File("$directory/span-tree.txt").writeText(roots.joinToString("\n") { it.toTreeNode(spanTree).toString() })
    }

    @Suppress("unused")
    companion object {
        enum class TestProfile {
            Local,
            Jaeger
        }
    }
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
