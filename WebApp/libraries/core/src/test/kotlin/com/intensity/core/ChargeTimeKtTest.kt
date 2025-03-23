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
                timeSlot(BD("13.14"), BD("73"), baseTime),
                timeSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30)),
                timeSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 30) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across more than one data slot`() {
        val electricity = listOf(
            listOf(
                timeSlot(BD("10.14"), BD("53"), baseTime),
                timeSlot(BD("12.40"), BD("58"), baseTime.plusMinutes(30)),
                timeSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 60) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime, baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across a fractional one data slot`() {
        val electricity = listOf(
            listOf(
                timeSlot(BD("13.14"), BD("59"), baseTime),
                timeSlot(BD("12.40"), BD("58"), baseTime.plusMinutes(30)),
                timeSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 20) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(50))))
    }

    @Test
    fun `calculate best times to use electricity across a fractional multiple data slot`() {
        val electricity = listOf(
            listOf(
                timeSlot(BD("10.50"), BD("61"), baseTime),
                timeSlot(BD("10.00"), BD("58"), baseTime.plusMinutes(30)),
                timeSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(60))
            )
        )

        val calculate = calculateChargeTime(electricity, 42) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(18), baseTime.plusMinutes(60))))
    }

    @Test
    fun `calculate best times to use electricity across non-consecutive time slots`() {
        val electricity = listOf(
            listOf(
                timeSlot(BD("11.00"), BD("63"), baseTime),
                timeSlot(BD("10.00"), BD("65"), baseTime.plusMinutes(30)),
                timeSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(60))
            ),
            listOf(
                timeSlot(BD("10.00"), BD("63"), baseTime.plusMinutes(120)),
                timeSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(150))
            )
        )

        val calculate = calculateChargeTime(electricity, 60) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(120), baseTime.plusMinutes(180))))
    }

    @Test
    fun `non-consecutive time slots does not run calculation in too short chunks`() {
        val electricity = listOf(
            listOf(
                timeSlot(BD("1.00"), BD("13"), baseTime)
            ),
            listOf(
                timeSlot(BD("10.00"), BD("63"), baseTime.plusMinutes(120)),
                timeSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(150))
            )
        )

        val calculate = calculateChargeTime(electricity, 31) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(120), baseTime.plusMinutes(151))))
    }

    @Test
    fun `handles time slots of varying length`() {
        val electricity = listOf(
            listOf(
                timeSlot(BD("11.00"), BD("63"), baseTime, 15),
                timeSlot(BD("11.00"), BD("64"), baseTime.plusMinutes(15), 20),
                timeSlot(BD("11.00"), BD("63"), baseTime.plusMinutes(35), 30),
            ),
            listOf(
                timeSlot(BD("10.00"), BD("63"), baseTime.plusMinutes(120), 29),
                timeSlot(BD("11.00"), BD("62"), baseTime.plusMinutes(149), 8)
            )
        )

        val calculate = calculateChargeTime(electricity, 30) { it.intensity }

        assertThat(calculate, equalTo(ChargeTime(baseTime.plusMinutes(127), baseTime.plusMinutes(157))))
    }

    private fun timeSlot(price: BD, intensity: BD, from: ZonedDateTime = ZonedDateTime.now(), length: Long = 30) =
        HalfHourElectricity(from, from.plusMinutes(length), price, intensity)
}
