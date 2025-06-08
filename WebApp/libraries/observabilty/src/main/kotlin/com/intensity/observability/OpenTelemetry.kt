package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD
import io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE
import io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS
import io.opentelemetry.semconv.ServerAttributes.SERVER_PORT
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import io.opentelemetry.semconv.UrlAttributes.URL_FULL
import org.http4k.core.Filter

interface ManagedOpenTelemetry {
    fun span(spanName: String): ManagedSpan
    fun end(span: ManagedSpan)
    fun trace(spanName: String, targetName: String): Filter
    fun propagateTrace(): Filter
    fun receiveTrace(): Filter
}

class TracingOpenTelemetry(private val openTelemetry: OpenTelemetry, private val serviceName: String): ManagedOpenTelemetry {
    private val context = ArrayDeque<Context>()

    companion object {
        fun noOp() = TracingOpenTelemetry(OpenTelemetry.noop(), "")
    }

    override fun span(spanName: String): ManagedSpan {
        val span = spanBuilder(spanName)
            .startSpan()
        context.addLast(currentContext().with(span))
        return ManagedSpan(span)
    }

    override fun end(span: ManagedSpan) {
        context.removeLast()
        span.end()
    }

    override fun trace(spanName: String, targetName: String): Filter {
        return Filter { next ->
            { request ->
                val span = spanBuilder(spanName)
                    .setSpanKind(SpanKind.CLIENT)
                    .setAttribute("http.target", targetName)
                    .setAttribute("http.path", request.uri.path)
                    .setAttribute(HTTP_REQUEST_METHOD, request.method.name)
                    .setAttribute(URL_FULL, request.uri.toString())
                    .setAttribute(SERVER_ADDRESS, request.uri.host)
                    .setAttribute(SERVER_PORT, request.uri.port?.toLong() ?: pathFrom(request.uri.scheme))
                    .startSpan()
                context.addLast(currentContext().with(span))
                next(request).also { response ->
                    span.setAttribute(HTTP_RESPONSE_STATUS_CODE, response.status.code.toLong())
                    context.removeLast()
                    span.end()
                }
            }
        }
    }

    private fun pathFrom(scheme: String): Long {
        return when (scheme) {
            "http" -> 80L
            "https" -> 443L
            else -> 0L
        }
    }

    override fun propagateTrace(): Filter {
        return Filter { next ->
            { request ->
                val headers = request.headers.toMap().toMutableMap()
                W3CTraceContextPropagator.getInstance().inject(currentContext(), headers, Setter)
                next(request.headers(headers.toList()))
            }
        }
    }

    override fun receiveTrace(): Filter {
        return Filter { next ->
            { request ->
                val headers = request.headers.toMap().toMutableMap()
                context.addLast(W3CTraceContextPropagator.getInstance().extract(currentContext(), headers, Getter))
                next(request)
            }
        }
    }

    private fun spanBuilder(spanName: String): SpanBuilder =
        openTelemetry.getTracer("com.intensity.observability")
            .spanBuilder(spanName)
            .setAttribute(SERVICE_NAME, serviceName)
            .setParent(currentContext())

    private fun currentContext() = context.lastOrNull() ?: Context.root()

    private object Setter : TextMapSetter<MutableMap<String, String?>> {
        override fun set(carrier: MutableMap<String, String?>?, key: String, value: String) {
            carrier?.put(key, value)
        }
    }

    private object Getter : TextMapGetter<MutableMap<String, String?>> {
        override fun keys(carrier: MutableMap<String, String?>) = carrier.keys

        override fun get(carrier: MutableMap<String, String?>?, key: String) = carrier?.get(key)
    }
}

class ManagedSpan(private val span: Span) {
    fun end() {
        span.end()
    }

    fun addEvent(eventName: String) {
        span.addEvent(eventName)
    }

    fun updateName(newName: String) {
        span.updateName(newName)
    }
}
