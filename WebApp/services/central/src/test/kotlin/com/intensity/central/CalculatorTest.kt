package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.HalfHourElectricity
import com.intensity.nationalgrid.HalfHourData
import com.intensity.nationalgrid.Intensity
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridData
import com.intensity.octopus.HalfHourPrices
import com.intensity.octopus.Octopus
import com.intensity.octopus.OctopusProduct
import com.intensity.octopus.OctopusTariff
import com.intensity.octopus.Prices
import com.intensity.octopus.ProductDetails
import com.intensity.octopus.Products
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Result
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CalculatorTest {
    private val calculator = Calculator(OctopusFake(), NationalGridFake(), LimitFake(), WeightsFake())

    @Test
    fun `creates electricity data from prices and intensity data`() {
        val startTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val prices = Prices(
            listOf(
                HalfHourPrices(12.5, 13.0, startTime, startTime.plusMinutes(30)),
                HalfHourPrices(12.6, 13.1, startTime.plusMinutes(30), startTime.plusMinutes(60)),
                HalfHourPrices(12.7, 13.2, startTime.plusMinutes(60), startTime.plusMinutes(90)),
                HalfHourPrices(12.8, 13.3, startTime.plusMinutes(90), startTime.plusMinutes(120))
            )
        )
        val intensity = NationalGridData(
            listOf(
                HalfHourData(startTime.toInstant(), startTime.plusMinutes(30).toInstant(), Intensity(100, null, "")),
                HalfHourData(
                    startTime.plusMinutes(30).toInstant(),
                    startTime.plusMinutes(60).toInstant(),
                    Intensity(99, null, "")
                ),
                HalfHourData(
                    startTime.plusMinutes(60).toInstant(),
                    startTime.plusMinutes(90).toInstant(),
                    Intensity(101, null, "")
                ),
                HalfHourData(
                    startTime.plusMinutes(90).toInstant(),
                    startTime.plusMinutes(120).toInstant(),
                    Intensity(102, null, "")
                )
            )
        )
        val expectedElectricity = Electricity(
            listOf(
                HalfHourElectricity(startTime, startTime.plusMinutes(30), BigDecimal("13.0"), BigDecimal("100")),
                HalfHourElectricity(
                    startTime.plusMinutes(30),
                    startTime.plusMinutes(60),
                    BigDecimal("13.1"),
                    BigDecimal("99")
                ),
                HalfHourElectricity(
                    startTime.plusMinutes(60),
                    startTime.plusMinutes(90),
                    BigDecimal("13.2"),
                    BigDecimal("101")
                ),
                HalfHourElectricity(
                    startTime.plusMinutes(90),
                    startTime.plusMinutes(120),
                    BigDecimal("13.3"),
                    BigDecimal("102")
                )
            )
        )

        val electricity = calculator.createElectricityFrom(prices, intensity)

        assertThat(electricity, equalTo(expectedElectricity))
    }


    @Test
    fun `creates electricity data only with data at same time slots`() {
        val startTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val prices = Prices(
            listOf(
                HalfHourPrices(12.5, 13.0, startTime, startTime.plusMinutes(30)),
                HalfHourPrices(12.6, 13.1, startTime.plusMinutes(30), startTime.plusMinutes(60)),
                HalfHourPrices(12.7, 13.2, startTime.plusMinutes(60), startTime.plusMinutes(90)),
                HalfHourPrices(12.8, 13.3, startTime.plusMinutes(120), startTime.plusMinutes(150))
            )
        )
        val intensity = NationalGridData(
            listOf(
                HalfHourData(startTime.toInstant(), startTime.plusMinutes(30).toInstant(), Intensity(100, null, "")),
                HalfHourData(
                    startTime.plusMinutes(30).toInstant(),
                    startTime.plusMinutes(60).toInstant(),
                    Intensity(99, null, "")
                ),
                HalfHourData(
                    startTime.plusMinutes(60).toInstant(),
                    startTime.plusMinutes(90).toInstant(),
                    Intensity(101, null, "")
                ),
                HalfHourData(
                    startTime.plusMinutes(90).toInstant(),
                    startTime.plusMinutes(120).toInstant(),
                    Intensity(102, null, "")
                )
            )
        )
        val expectedElectricity = Electricity(
            listOf(
                HalfHourElectricity(startTime, startTime.plusMinutes(30), BigDecimal("13.0"), BigDecimal("100")),
                HalfHourElectricity(
                    startTime.plusMinutes(30),
                    startTime.plusMinutes(60),
                    BigDecimal("13.1"),
                    BigDecimal("99")
                ),
                HalfHourElectricity(
                    startTime.plusMinutes(60),
                    startTime.plusMinutes(90),
                    BigDecimal("13.2"),
                    BigDecimal("101")
                )
            )
        )

        val electricity = calculator.createElectricityFrom(prices, intensity)

        assertThat(electricity, equalTo(expectedElectricity))
    }
}

class OctopusFake : Octopus {
    override fun products(): Result<Products, Failed> {
        TODO("Not yet implemented")
    }

    override fun product(product: OctopusProduct): Result<ProductDetails, Failed> {
        TODO("Not yet implemented")
    }

    override fun prices(
        product: OctopusProduct,
        tariff: OctopusTariff,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): Result<Prices, Failed> {
        TODO("Not yet implemented")
    }
}

class NationalGridFake : NationalGrid {
    override fun fortyEightHourIntensity(time: Instant): Result<NationalGridData, Failed> {
        TODO("Not yet implemented")
    }
}

class LimitFake : LimitCalculator {
    override fun intensityLimit(electricity: Electricity, limit: BigDecimal, time: Long): ChargeTime? {
        TODO("Not yet implemented")
    }

    override fun priceLimit(electricity: Electricity, limit: BigDecimal, time: Long): ChargeTime? {
        TODO("Not yet implemented")
    }
}

class WeightsFake : WeightsCalculator {
    override fun chargeTime(electricity: Electricity, weights: Weights, time: Long): ChargeTime {
        TODO("Not yet implemented")
    }
}
