package com.intensity.schedulecalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CalculatorKtTest {
    @Test
    fun `accepts the intensity and pricing data and returns time bands`() {
        val schedule = calculate(listOf(), listOf(), 5, 5)

        assertThat(schedule, isA<List<Any>>())
    }

    @Test
    fun `normalizes all values in a list so they are all a ratio of the largest`() {
        val normalized = normalize(listOf(BigDecimal("2"), BigDecimal("4"), BigDecimal("3")))

        assertThat(normalized, equalTo(listOf(BigDecimal("0.5"), BigDecimal("1"), BigDecimal("0.75"))))
    }
}
