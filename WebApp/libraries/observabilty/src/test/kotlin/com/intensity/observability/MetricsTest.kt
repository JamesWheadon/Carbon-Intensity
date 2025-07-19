package com.intensity.observability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.DoubleGaugeBuilder
import io.opentelemetry.api.metrics.DoubleHistogramBuilder
import io.opentelemetry.api.metrics.LongCounterBuilder
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.junit.jupiter.api.Test

class MetricsTest {
    private val metricReader = InMemoryMetricReader.builder().build()
    private val openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metricReader).build()).build()

    @Test
    fun `increments a counter by one`() {
        Metrics(openTelemetry).measure(CounterMetric(MetricName("testMetric")))

        val metricData = metricReader.collectAllMetrics().first { it.name == "testMetric" }
        assertThat(metricData.longSumData.points.sumOf { it.value }, equalTo(1))
    }

    @Test
    fun `only creates a counter once`() {
        val spy = OpenTelemetrySpy(openTelemetry)
        val metrics = Metrics(spy)
        metrics.measure(CounterMetric(MetricName("testMetric")))
        metrics.measure(CounterMetric(MetricName("testMetric")))

        assertThat(spy.metricsCreated(), equalTo(listOf("testMetric")))
        val metricData = metricReader.collectAllMetrics().first { it.name == "testMetric" }
        assertThat(metricData.longSumData.points.sumOf { it.value }, equalTo(2))
    }

    @Test
    fun `increments a double metric by the supplied amount`() {
        val spy = OpenTelemetrySpy(openTelemetry)
        val metrics = Metrics(spy)
        metrics.measure(DoubleMetric(MetricName("testDoubleMetric"), 2.0))
        metrics.measure(DoubleMetric(MetricName("testDoubleMetric"), 3.0))

        val metricData = metricReader.collectAllMetrics().first { it.name == "testDoubleMetric" }
        assertThat(metricData.doubleSumData.points.sumOf { it.value }, equalTo(5.0))
    }
}

class OpenTelemetrySpy(private val openTelemetry: OpenTelemetry) : OpenTelemetry {
    private val meters = mutableListOf<MeterSpy>()
    override fun getTracerProvider(): TracerProvider = openTelemetry.tracerProvider

    override fun getPropagators(): ContextPropagators = openTelemetry.propagators

    override fun getMeter(instrumentationScopeName: String): Meter {
        return MeterSpy(openTelemetry.getMeter(instrumentationScopeName)).also { meters.add(it) }
    }

    fun metricsCreated() = meters.flatMap { it.countersMade }
}

class MeterSpy(private val meter: Meter) : Meter {
    var countersMade = mutableListOf<String>()

    override fun counterBuilder(p0: String): LongCounterBuilder = meter.counterBuilder(p0).also { countersMade.add(p0) }

    override fun upDownCounterBuilder(p0: String): LongUpDownCounterBuilder = meter.upDownCounterBuilder(p0)

    override fun histogramBuilder(p0: String): DoubleHistogramBuilder = meter.histogramBuilder(p0)

    override fun gaugeBuilder(p0: String): DoubleGaugeBuilder = meter.gaugeBuilder(p0)
}
