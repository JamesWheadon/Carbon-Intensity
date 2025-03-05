package com.intensity.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

typealias BD = BigDecimal

class ChargeTimeKtTest {
    private val baseTime = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `calculate best times to use electricity based on weights and requirements`() {
        val electricity = listOf(
            listOf(
                halfHourSlot(BD("13.14"), BD("73"), baseTime),
                halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 30) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across more than one data slot`() {
        val electricity = listOf(
            listOf(
                halfHourSlot(BD("10.14"), BD("53"), baseTime),
                halfHourSlot(BD("12.40"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 60) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime, baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across a fractional one data slot`() {
        val electricity = listOf(
            listOf(
                halfHourSlot(BD("13.14"), BD("59"), baseTime),
                halfHourSlot(BD("12.40"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 20) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(50))))
    }

    @Test
    fun `calculate best times to use electricity across a fractional multiple data slot`() {
        val electricity = listOf(
            listOf(
                halfHourSlot(BD("10.50"), BD("61"), baseTime),
                halfHourSlot(BD("10.00"), BD("58"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 42) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(18), baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across non-consecutive time slots`() {
        val electricity = listOf(
            listOf(
                halfHourSlot(BD("11.00"), BD("63"), baseTime),
                halfHourSlot(BD("10.00"), BD("65"), baseTime.plusMinutes(30)),
                halfHourSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(60))
            ),
            listOf(
                halfHourSlot(BD("10.00"), BD("63"), baseTime.plusMinutes(120)),
                halfHourSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(150))
            )
        )

        val calculate = calculateChargeTime(electricity, 60) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(120), baseTime.plusMinutes(180))))
    }

    @Test
    fun `non-consecutive time slots does not run calculation in too short chunks`() {
        val electricity = listOf(
            listOf(
                halfHourSlot(BD("1.00"), BD("13"), baseTime)
            ),
            listOf(
                halfHourSlot(BD("10.00"), BD("63"), baseTime.plusMinutes(120)),
                halfHourSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(150))
            )
        )

        val calculate = calculateChargeTime(electricity, 31) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(120), baseTime.plusMinutes(151))))
    }

    private fun halfHourSlot(price: BD, intensity: BD, from: ZonedDateTime = ZonedDateTime.now()) =
        HalfHourElectricity(from, from.plusMinutes(30), price, intensity)
}
