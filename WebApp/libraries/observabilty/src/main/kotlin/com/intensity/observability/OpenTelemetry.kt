package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.SpanKind.CLIENT
import io.opentelemetry.api.trace.SpanKind.INTERNAL
import io.opentelemetry.api.trace.StatusCode.ERROR
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
import org.http4k.core.Request
import org.http4k.core.Uri

interface ManagedOpenTelemetry {
    fun <T> span(spanName: String, block: (ManagedSpan) -> T): T
    fun trace(spanName: String, targetName: String): Filter
    fun propagateTrace(): Filter
    fun receiveTrace(): Filter
}

class TracingOpenTelemetry(
    private val openTelemetry: OpenTelemetry,
    private val serviceName: String
) : ManagedOpenTelemetry {
    companion object {
        fun noOp() = TracingOpenTelemetry(OpenTelemetry.noop(), "")
    }

    override fun <T> span(spanName: String, block: (ManagedSpan) -> T): T {
        val span = createSpan(spanName)
        return try {
            block(span)
        } catch (e: Exception) {
            span.setError()
            throw e
        } finally {
            span.close()
        }
    }

    override fun trace(spanName: String, targetName: String): Filter {
        return Filter { next ->
            { request ->
                val span = createSpan(
                    spanName = spanName,
                    spanKind = CLIENT,
                    attributes = Attributes.of(
                        HTTP_REQUEST_METHOD, request.method.name,
                        URL_FULL, request.uri.toString(),
                        SERVER_ADDRESS, request.uri.host,
                        SERVER_PORT, request.port(),
                        AttributeKey.stringKey("http.target"), targetName,
                        AttributeKey.stringKey("http.path"), request.uri.path
                    )
                )
                try {
                    next(request).also { response ->
                        span.setAttribute(HTTP_RESPONSE_STATUS_CODE.key, response.status.code.toLong())
                    }
                } catch (e: Exception) {
                    span.setError()
                    throw e
                } finally {
                    span.close()
                }
            }
        }
    }

    private fun createSpan(
        spanName: String,
        spanKind: SpanKind = INTERNAL,
        attributes: Attributes = Attributes.empty()
    ): ManagedSpan =
        ManagedSpan(
            openTelemetry.getTracer("com.intensity.observability")
                .spanBuilder(spanName)
                .setSpanKind(spanKind)
                .setAttribute(SERVICE_NAME, serviceName)
                .setAllAttributes(attributes)
                .startSpan()
        )

    private fun Request.port() = uri.port?.toLong() ?: pathFrom(uri)

    private fun pathFrom(scheme: Uri): Long {
        return when (scheme.scheme) {
            "http" -> 80L
            "https" -> 443L
            else -> 0L
        }
    }

    override fun propagateTrace(): Filter {
        return Filter { next ->
            { request ->
                val headers = request.headers.toMap().toMutableMap()
                W3CTraceContextPropagator.getInstance().inject(Context.current(), headers, Setter)
                next(request.headers(headers.toList()))
            }
        }
    }

    override fun receiveTrace(): Filter {
        return Filter { next ->
            { request ->
                val headers = request.headers.toMap().toMutableMap()
                W3CTraceContextPropagator.getInstance().extract(Context.current(), headers, Getter).makeCurrent().use {
                    next(request)
                }
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
    private val scope = span.makeCurrent()

    fun close() {
        scope.close()
        span.end()
    }

    fun addEvent(eventName: String) {
        span.addEvent(eventName)
    }

    fun updateName(newName: String) {
        span.updateName(newName)
    }

    fun setAttribute(key: String, value: Long) {
        span.setAttribute(key, value)
    }

    fun setError() {
        span.setStatus(ERROR)
    }
}
