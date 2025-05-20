package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import org.http4k.core.Filter
import org.http4k.metrics.Http4kOpenTelemetry

class ManagedOpenTelemetry(private val openTelemetry: OpenTelemetry, private val serviceName: String) {
    fun span(spanName: String): ManagedSpan {
        val span = openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME)
            .spanBuilder(spanName)
            .setAttribute("service.name", serviceName)
            .startSpan()
        return ManagedSpan(span)
    }

    fun trace(spanName: String, targetName: String): Filter {
        return Filter { next ->
            { request ->
                val span = openTelemetry.getTracer(Http4kOpenTelemetry.INSTRUMENTATION_NAME)
                    .spanBuilder(spanName)
                    .setAttribute("service.name", serviceName)
                    .setAttribute("http.target", targetName)
                    .setAttribute("http.path", request.uri.path)
                    .startSpan()
                next(request).also { response ->
                    span.setAttribute("http.status", response.status.code.toLong())
                    span.end()
                }
            }
        }
    }

    fun propagateTrace(): Filter {
        return Filter { next ->
            { request ->
                val headers = request.headers.toMap().toMutableMap()
                W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, Setter)
                next(request.headers(headers.toList()))
            }
        }
    }

    fun receiveTrace(): Filter {
        return Filter { next ->
            { request ->
                val headers = request.headers.toMap().toMutableMap()
                W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, Getter).makeCurrent()
                next(request)
            }
        }
    }

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

    fun makeCurrent() {
        span.makeCurrent()
    }
}
