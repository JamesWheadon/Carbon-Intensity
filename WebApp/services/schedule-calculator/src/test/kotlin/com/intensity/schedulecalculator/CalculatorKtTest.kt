package com.intensity.schedulecalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

typealias BD = BigDecimal

class CalculatorKtTest {
    private val baseTime = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

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

    @Test
    fun `calculate best times to use electricity based on weights and requirements`() {
        val electricity = Electricity(
            listOf(
                halfHourSlot(BD("13.14"), BD("73"), baseTime),
                halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )
        val weights = Weights(BD("0.8"), BD("1"))

        val calculate = calculate(electricity, weights, 30)

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across more than one data slot`() {
        val electricity = Electricity(
            listOf(
                halfHourSlot(BD("10.14"), BD("53"), baseTime),
                halfHourSlot(BD("12.40"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )
        val weights = Weights(BD("0.8"), BD("1"))

        val calculate = calculate(electricity, weights, 60)

        assertThat(calculate, equalTo(ChargeTime(baseTime, baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across a fractional one data slot`() {
        val electricity = Electricity(
            listOf(
                halfHourSlot(BD("13.14"), BD("59"), baseTime),
                halfHourSlot(BD("12.40"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )
        val weights = Weights(BD("0.8"), BD("1"))

        val calculate = calculate(electricity, weights, 20)

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(50))))
    }

    @Test
    fun `calculate best times to use electricity across a fractional multiple data slot`() {
        val electricity = Electricity(
            listOf(
                halfHourSlot(BD("10.50"), BD("61"), baseTime),
                halfHourSlot(BD("10.00"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(60))
            )
        )
        val weights = Weights(BD("0.8"), BD("1"))

        val calculate = calculate(electricity, weights, 42)

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(18), baseTime.plusMinutes(60))))
    }

    private fun halfHourSlot(price: BD, intensity: BD, from: ZonedDateTime = ZonedDateTime.now()) =
        HalfHourElectricity(from, from.plusMinutes(30), price, intensity)
}
