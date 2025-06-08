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
import io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD
import io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE
import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import org.http4k.core.Status
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("unused")
class TestTracingOpenTelemetry private constructor(
    private val testOpenTelemetry: TestOpenTelemetry,
    openTelemetry: ManagedOpenTelemetry
) : ManagedOpenTelemetry by openTelemetry {
    constructor(profile: TestProfile, serviceName: String) : this(TestOpenTelemetry(profile), serviceName)
    private constructor(testOpenTelemetry: TestOpenTelemetry, serviceName: String) :
            this(testOpenTelemetry, TracingOpenTelemetry(testOpenTelemetry, serviceName))

    fun spans() = testOpenTelemetry.spans()

    fun spans(vararg telemetry: TestTracingOpenTelemetry): List<SpanData> =
        testOpenTelemetry.spans().plus(telemetry.flatMap { it.spans() })

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
            span.instrumentationScopeInfo.name,
            span.startEpochNanos
        )
    }

    fun spans(vararg telemetry: TestOpenTelemetry): List<SpanData> = spans().plus(telemetry.flatMap { it.spans() })

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

    fun approveSpanDiagram(spans: List<SpanData>, testName: String) {
        val sortedSpans = spans.sortedBy { it.startEpochNanos }
        val (roots, children) = spans.partition { it.parentSpanId == "0000000000000000" }

        children.firstOrNull { span -> spans.none { it.spanId == span.parentSpanId } }?.let { span ->
            throw AssertionError("A Span was not ended, parent of ${span.name}")
        }

        val spanDiagram = roots.map {
            it.toTreeNode(spans)
        }.joinToString("\n")
        val directory = "../generated/${testName.removeSuffix("()").removeSuffix("(TestInfo)").replace(" ", "-")}"
        val approvedOutput = File("$directory/span-tree.txt")
        val actualOutput = File("$directory/span-tree-actual.txt").also { it.delete() }

        if (approvedOutput.exists()) {
            createSequenceDiagram(sortedSpans, testName, directory)
            val approvedText = approvedOutput.readText()
            if (approvedText == spanDiagram) {
                return
            } else {
                actualOutput.writeText(spanDiagram)
            }
        } else {
            File("../generated").mkdir()
            File(directory).mkdir()
            actualOutput.writeText(spanDiagram)
            createSequenceDiagram(sortedSpans, testName, directory)
        }
        throw AssertionError("Span diagram is not approved")
    }

    private fun createSequenceDiagram(spans: List<SpanData>, testName: String, directory: String) {
        val calls = spans.filter { it.attributes.containsKey("http.target") }.map { it.toHttpSpan() }
        val participants = calls.flatMap { listOf(it.caller, it.target) }.toSet()
        val puml = """
            @startuml
            title $testName
            ${participants.joinToString("\n") { """participant "$it"""" }}
            ${
            calls.mapIndexed { index, span ->
                val callerFirst = calls.indexOfFirst { it.caller == span.caller || it.target == span.caller } == index
                val callerLast = calls.indexOfLast { it.caller == span.caller || it.target == span.caller } == index
                val targetFirst = calls.indexOfFirst { it.caller == span.target || it.target == span.target } == index
                val targetLast = calls.indexOfLast { it.caller == span.target || it.target == span.target } == index
                val request = """"${span.caller}" -> "${span.target}": ${span.method} ${span.path}"""
                val response =
                    """"${span.target}" -> "${span.caller}": ${span.status.code} ${span.status.description}"""
                """
                $request
                    ${if (callerFirst) "activate \"${span.caller}\"" else ""}
                    ${if (targetFirst) "activate \"${span.target}\"" else ""}
                $response
                    ${if (targetLast) "deactivate \"${span.target}\"" else ""}
                    ${if (callerLast) "deactivate \"${span.caller}\"" else ""}
                """.trimIndent()
            }.joinToString("\n")
        }
            @enduml
        """.trimIndent().lines().map { it.trimIndent() }

        File("$directory/sequence.puml").writeText(puml.joinToString("\n"))
    }
}

enum class TestProfile {
    Local,
    Jaeger
}

private fun SpanData.toTreeNode(spans: List<SpanData>): TreeNode {
    return TreeNode(this, spans.filter { it.parentSpanId == spanId }.map { it.toTreeNode(spans) })
}

class TreeNode(private val span: SpanData, private val children: List<TreeNode>) {
    override fun toString(): String {
        val buffer = StringBuilder(50)
        print(buffer, "", "")
        return buffer.toString()
    }

    private fun print(buffer: StringBuilder, prefix: String, childrenPrefix: String) {
        buffer.append(prefix)
        buffer.append(span.name)
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

data class SpanData(
    val name: String,
    val spanId: String,
    val parentSpanId: String,
    val attributes: Map<String, Any>,
    val events: List<SpanEvent>,
    val instrumentationName: String,
    val startEpochNanos: Long
)

data class SpanEvent(val name: String)

private data class HttpSpan(
    val caller: String,
    val target: String,
    val method: String,
    val path: String,
    val status: Status
)

private fun SpanData.toHttpSpan() = HttpSpan(
    this.attributes[SERVICE_NAME.key]!! as String,
    this.attributes["http.target"]!! as String,
    this.attributes[HTTP_REQUEST_METHOD.key]!! as String,
    this.attributes["http.path"]!! as String,
    Status.fromCode((this.attributes[HTTP_RESPONSE_STATUS_CODE.key] as Long).toInt())!!
)
