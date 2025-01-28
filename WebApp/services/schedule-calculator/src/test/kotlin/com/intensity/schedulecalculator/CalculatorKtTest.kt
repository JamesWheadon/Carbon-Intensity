package com.intensity.schedulecalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

typealias BD = BigDecimal

class CalculatorKtTest {
    @Test
    fun `normalizes all values in a list so they are all a ratio of the largest`() {
        val normalized = normalize(listOf(BD("2"), BD("4"), BD("3")))

        assertThat(normalized, equalTo(listOf(BD("0.50000"), BD("1.00000"), BD("0.75000"))))
    }

    @Test
    fun `normalizes handles negative values`() {
        val normalized = normalize(listOf(BD("2"), BD("-4"), BD("1")))

        assertThat(normalized, equalTo(listOf(BD("1.00000"), BD("-2.00000"), BD("0.50000"))))
    }

    @Test
    fun `normalizes handles recursive values`() {
        val normalized = normalize(listOf(BD("2"), BD("3"), BD("1")))

        assertThat(normalized, equalTo(listOf(BD("0.66667"), BD("1.00000"), BD("0.33333"))))
    }

    @Test
    fun `normalizes electricity data`() {
        val electricity = Electricity(
            listOf(
                halfHourSlot(BD("10.14"), BD("53")),
                halfHourSlot(BD("12.40"), BD("58")),
                halfHourSlot(BD("11.67"), BD("63"))
            )
        )

        val normalizedElectricity = normalize(electricity)

        assertThat(
            normalizedElectricity.slots.map { it.price },
            equalTo(listOf(BD("0.81774"), BD("1.00000"), BD("0.94113")))
        )
        assertThat(
            normalizedElectricity.slots.map { it.intensity },
            equalTo(listOf(BD("0.84127"), BD("0.92063"), BD("1.00000")))
        )
    }

    private fun halfHourSlot(price: BigDecimal, intensity: BigDecimal) =
        HalfHourElectricity(ZonedDateTime.now(), ZonedDateTime.now(), price, intensity)
}
