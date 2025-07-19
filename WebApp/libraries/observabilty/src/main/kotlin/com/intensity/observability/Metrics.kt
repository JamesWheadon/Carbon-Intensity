package com.intensity.observability

import com.intensity.observability.Metric.Type
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.DoubleCounter
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME

class Metrics(private val openTelemetry: OpenTelemetry, private val serviceName: String) {
    private val registry = mutableMapOf<MetricName, MetricInstrument<out Any>>()

    fun <T> measure(metric: Metric<T>) {
        metricInstrument(metric).measure(metric, serviceName)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Metrics.metricInstrument(metric: Metric<T>): MetricInstrument<T> {
        val metricInstrument = registry[metric.name]
        return when {
            metricInstrument == null -> {
                val answer = createMetricInstrument(metric)
                registry[metric.name] = answer
                answer as MetricInstrument<T>
            }
            metricInstrument.type != metric.type() -> {
                throw IllegalStateException("Metric '$metric' already exists with type ${metricInstrument.type}")
            }
            else -> metricInstrument as MetricInstrument<T>
        }
    }

    private fun <T> createMetricInstrument(metric: Metric<T>) =
        when (metric) {
            is CounterMetric -> LongCounterWrapper(openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).build())
            is DoubleMetric -> DoubleCounterWrapper(openTelemetry.getMeter("com.intensity.observability").counterBuilder(metric.name.value).ofDoubles().build())
        }
}

sealed interface MetricInstrument<T> {
    val type: Type

    fun measure(metric: Metric<T>, serviceName: String)
}

class LongCounterWrapper(private val counter: LongCounter) : MetricInstrument<Long> {
    override val type = Type.Counter

    override fun measure(metric: Metric<Long>, serviceName: String) {
        counter.add(metric.value, Attributes.of(SERVICE_NAME, serviceName))
    }
}

class DoubleCounterWrapper(private val counter: DoubleCounter) : MetricInstrument<Double> {
    override val type = Type.DoubleCounter

    override fun measure(metric: Metric<Double>, serviceName: String) {
        counter.add(metric.value, Attributes.of(SERVICE_NAME, serviceName))
    }
}

sealed interface Metric<T> {
    val name: MetricName
    val value: T

    enum class Type {
        Counter,
        DoubleCounter
    }
}

data class CounterMetric(override val name: MetricName, override val value: Long = 1) : Metric<Long>
data class DoubleMetric(override val name: MetricName, override val value: Double) : Metric<Double>

@JvmInline
value class MetricName(val value: String) {
    override fun toString() = value
}

private fun <T> Metric<T>.type() =
    when(this) {
        is CounterMetric -> Type.Counter
        is DoubleMetric -> Type.DoubleCounter
    }
