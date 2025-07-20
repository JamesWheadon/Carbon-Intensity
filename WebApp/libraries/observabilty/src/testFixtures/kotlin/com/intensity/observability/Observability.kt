package com.intensity.observability

import java.io.File

class TestObservability {
    private val openTelemetry = TestOpenTelemetry()
    private val logging = TestLogging()

    fun observability(serviceName: String): Observability {
        val openTelemetry = openTelemetry
        return Observability(
            OpenTelemetryTracer(openTelemetry, serviceName),
            Metrics(openTelemetry, serviceName),
            logging
        )
    }

    fun spans() = openTelemetry.spans()

    fun approveSpanDiagram(testName: String) {
        val spans = spans().toMutableList()
        approveSpanDiagram(spans, testName)
    }
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

private fun approveSpanDiagram(spans: List<SpanData>, testName: String) {
    val sortedSpans = spans.sortedBy { it.startEpochNanos }
    val (roots, children) = spans.partition { it.parentSpanId == SpanId("0000000000000000") }

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
