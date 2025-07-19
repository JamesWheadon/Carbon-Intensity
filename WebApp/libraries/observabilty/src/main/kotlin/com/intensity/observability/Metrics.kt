package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.DoubleCounter
import io.opentelemetry.api.metrics.LongCounter

class Metrics(private val openTelemetry: OpenTelemetry) {
    private val registry = mutableMapOf<MetricName, LongCounter>()
    private val doubleRegistry = mutableMapOf<MetricName, DoubleCounter>()

    fun measure(metric: Metric) {
        when (metric) {
            is CounterMetric -> registry.getOrPut(metric.name) {
                openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).build()
            }
                .add(1)
            is DoubleMetric -> doubleRegistry.getOrPut(metric.name) {
                openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).ofDoubles().build()
            }
                .add(metric.value)
        }
    }
}

sealed interface Metric {
    val name: MetricName
}

data class CounterMetric(override val name: MetricName) : Metric
data class DoubleMetric(override val name: MetricName, val value: Double) : Metric

@JvmInline
value class MetricName(val value: String)
