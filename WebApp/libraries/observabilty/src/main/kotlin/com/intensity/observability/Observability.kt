package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry

@Suppress("MoveLambdaOutsideParentheses")
class Observability(
    private val tracer: Tracer,
    private val metrics: Metrics,
    private val logging: LogOutput
) : Tracer by tracer {
    companion object {
        fun noOp(serviceName: String): Observability =
            Observability(
                OpenTelemetryTracer(OpenTelemetry.noop(), serviceName),
                Metrics(OpenTelemetry.noop()),
                { }
            )
    }

    fun invoke(logEvent: LogEvent) {
        logging(logEvent)
    }

    fun <T> invoke(metric: Metric<T>) {
        metrics.measure(metric)
    }
}
