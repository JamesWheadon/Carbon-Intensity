package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.LongCounter

class Metrics(private val openTelemetry: OpenTelemetry) {
    private val registry = mutableMapOf<MetricName, LongCounter>()
    fun measure(metric: Metric) {
        registry.getOrPut(metric.name) {
            openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).build()
        }
            .add(1)
    }
}

data class Metric(val name: MetricName)

@JvmInline
value class MetricName(val value: String)
