package com.intensity.limitcalculator

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.HalfHourElectricity
import com.intensity.core.NoChargeTimePossible
import com.intensity.core.OverlappingData
import com.intensity.coretest.isFailure
import com.intensity.coretest.isSuccess
import com.natpryce.hamkrest.assertion.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

typealias BD = BigDecimal

class CalculatorKtTest {
    private val baseTime = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `finds the best time to charge by price when under the intensity limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underIntensityLimit(electricity, BD("65"), 45L)

        assertThat(halfHoursInLimit, isSuccess(ChargeTime(baseTime.plusMinutes(30), baseTime.plusMinutes(75))))
    }

    @Test
    fun `rejects invalid electricity data with overlapping windows for intensity limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(15))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underIntensityLimit(electricity, BD("65"), 30L)

        assertThat(halfHoursInLimit, isFailure(OverlappingData))
    }

    @Test
    fun `can not calculate schedule if not enough time under limit for intensity limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underIntensityLimit(electricity, BD("65"), 75L)

        assertThat(halfHoursInLimit, isFailure(NoChargeTimePossible))
    }

    @Test
    fun `finds the best time to charge by price when under the price limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("57"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underPriceLimit(electricity, BD("13.0"), 45L)

        assertThat(halfHoursInLimit, isSuccess(ChargeTime(baseTime.plusMinutes(45), baseTime.plusMinutes(90))))
    }

    @Test
    fun `rejects invalid electricity data with overlapping windows for price limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(15))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underPriceLimit(electricity, BD("13.0"), 30L)

        assertThat(halfHoursInLimit, isFailure(OverlappingData))
    }

    @Test
    fun `can not calculate schedule if not enough time under limit for price limit`() {
        val firstHalfHour = halfHourSlot(BD("13.14"), BD("73"), baseTime)
        val secondHalfHour = halfHourSlot(BD("10.40"), BD("58"), baseTime.plusMinutes(30))
        val thirdHalfHour = halfHourSlot(BD("11.67"), BD("63"), baseTime.plusMinutes(60))
        val electricity = Electricity(listOf(firstHalfHour, secondHalfHour, thirdHalfHour))

        val halfHoursInLimit = underPriceLimit(electricity, BD("13.0"), 75L)

        assertThat(halfHoursInLimit, isFailure(NoChargeTimePossible))
    }

    private fun halfHourSlot(price: BD, intensity: BD, from: ZonedDateTime = ZonedDateTime.now()) =
        HalfHourElectricity(from, from.plusMinutes(30), price, intensity)
}
