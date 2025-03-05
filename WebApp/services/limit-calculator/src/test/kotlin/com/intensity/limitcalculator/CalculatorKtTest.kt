package com.intensity.limitcalculator

import com.intensity.core.Electricity
import com.intensity.core.HalfHourElectricity
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
    fun `finds all electricity data under the intensity limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underIntensityLimit(electricity, BD(65))

        assertThat(halfHoursInLimit, equalTo(listOf(secondHalfHour, thirdHalfHour)))
    }

    private fun halfHourSlot(price: BD, intensity: BD, from: ZonedDateTime = ZonedDateTime.now()) =
        HalfHourElectricity(from, from.plusMinutes(30), price, intensity)
}
