package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry

class Metrics(private val openTelemetry: OpenTelemetry) {
    fun measure(metric: Metric) {
        openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).build().add(1)
    }
}

data class Metric(val name: MetricName)

@JvmInline
value class MetricName(val value: String)
