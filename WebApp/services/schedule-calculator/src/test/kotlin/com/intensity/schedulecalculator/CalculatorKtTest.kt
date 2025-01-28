package com.intensity.schedulecalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CalculatorKtTest {
    @Test
    fun `normalizes all values in a list so they are all a ratio of the largest`() {
        val normalized = normalize(listOf(BigDecimal("2"), BigDecimal("4"), BigDecimal("3")))

        assertThat(normalized, equalTo(listOf(BigDecimal("0.50000"), BigDecimal("1.00000"), BigDecimal("0.75000"))))
    }

    @Test
    fun `normalizes handles negative values`() {
        val normalized = normalize(listOf(BigDecimal("2"), BigDecimal("-4"), BigDecimal("1")))

        assertThat(normalized, equalTo(listOf(BigDecimal("1.00000"), BigDecimal("-2.00000"), BigDecimal("0.50000"))))
    }

    @Test
    fun `normalizes handles recursive values`() {
        val normalized = normalize(listOf(BigDecimal("2"), BigDecimal("3"), BigDecimal("1")))

        assertThat(normalized, equalTo(listOf(BigDecimal("0.66667"), BigDecimal("1.00000"), BigDecimal("0.33333"))))
    }
}
