package com.intensity.schedulecalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import org.junit.jupiter.api.Test

class CalculatorKtTest {
    @Test
    fun `accepts the intensity and pricing data and returns time bands`() {
        val schedule = calculate(listOf(), listOf(), 5, 5)

        assertThat(schedule, isA<List<Any>>())
    }
}
