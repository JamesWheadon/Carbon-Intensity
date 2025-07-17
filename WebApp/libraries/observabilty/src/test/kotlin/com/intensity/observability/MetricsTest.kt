package com.intensity.observability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.junit.jupiter.api.Test

class MetricsTest {
    private val metricReader = InMemoryMetricReader.builder().build()
    private val openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metricReader).build()).build()

    @Test
    fun `increments a counter by one`() {
        Metrics(openTelemetry).measure(Metric(MetricName("testMetric")))

        val metricData = metricReader.collectAllMetrics().first { it.name == "testMetric" }
        assertThat(metricData.longSumData.points.sumOf { it.value }, equalTo(1))
    }
}
