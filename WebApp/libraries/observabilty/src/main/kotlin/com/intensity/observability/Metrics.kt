package com.intensity.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.DoubleCounter
import io.opentelemetry.api.metrics.LongCounter

class Metrics(private val openTelemetry: OpenTelemetry) {
    private val registry = mutableMapOf<MetricName, MetricInstrument<out Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> measure(metric: Metric<T>) {
        val metricInstrument = registry.getOrPut(metric.name) {
            createMetricInstrument(metric)
        } as MetricInstrument<T>
        metricInstrument.measure(metric)
    }

    private fun <T> createMetricInstrument(metric: Metric<T>) =
        when (metric) {
            is CounterMetric -> LongCounterWrapper(openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).build())
            is DoubleMetric -> DoubleCounterWrapper(openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).ofDoubles().build())
        }
}

sealed interface MetricInstrument<T> {
    fun measure(metric: Metric<T>)
}

class LongCounterWrapper(private val counter: LongCounter) : MetricInstrument<Long> {
    override fun measure(metric: Metric<Long>) {
        counter.add(metric.value)
    }
}

class DoubleCounterWrapper(private val counter: DoubleCounter) : MetricInstrument<Double> {
    override fun measure(metric: Metric<Double>) {
        counter.add(metric.value)
    }
}

sealed interface Metric<T> {
    val name: MetricName
    val value: T
}

data class CounterMetric(override val name: MetricName, override val value: Long = 1) : Metric<Long>
data class DoubleMetric(override val name: MetricName, override val value: Double) : Metric<Double>

@JvmInline
value class MetricName(val value: String)
