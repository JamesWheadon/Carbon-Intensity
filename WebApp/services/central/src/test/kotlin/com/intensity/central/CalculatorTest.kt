package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.ElectricityData
import com.intensity.core.Failed
import com.intensity.coretest.isSuccess
import com.intensity.nationalgrid.Intensity
import com.intensity.nationalgrid.IntensityData
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridData
import com.intensity.octopus.Octopus
import com.intensity.octopus.OctopusProduct
import com.intensity.octopus.OctopusTariff
import com.intensity.octopus.PriceData
import com.intensity.octopus.Prices
import com.intensity.octopus.ProductDetails
import com.intensity.octopus.Products
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Result
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CalculatorTest {
    private val fakeLimit = FakeLimitCalculator()
    private val fakeWeights = FakeWeightsCalculator()
    private val calculator = Calculator(
        OctopusFake(),
        NationalGridFake(),
        LimitCalculatorCloud(fakeLimit),
        WeightsCalculatorCloud(fakeWeights)
    )
    private val startTime = LocalDateTime.now().atZone(ZoneId.of("UTC").normalized()).truncatedTo(ChronoUnit.MINUTES)
    private val calculatorData = CalculationData(
        OctopusProduct("not needed"),
        OctopusTariff("not needed"),
        startTime,
        startTime.plusMinutes(90),
        45L
    )
    private val electricity = Electricity(
        listOf(
            ElectricityData(startTime, startTime.plusMinutes(15), BigDecimal("13.0"), BigDecimal("100")),
            ElectricityData(
                startTime.plusMinutes(15),
                startTime.plusMinutes(30),
                BigDecimal("13.0"),
                BigDecimal("102")
            ),
            ElectricityData(
                startTime.plusMinutes(30),
                startTime.plusMinutes(60),
                BigDecimal("13.1"),
                BigDecimal("99")
            ),
            ElectricityData(
                startTime.plusMinutes(60),
                startTime.plusMinutes(75),
                BigDecimal("13.2"),
                BigDecimal("101")
            ),
            ElectricityData(
                startTime.plusMinutes(75),
                startTime.plusMinutes(90),
                BigDecimal("13.3"),
                BigDecimal("101")
            )
        )
    )

    @Test
    fun `creates electricity data from prices and intensity data`() {
        val prices = Prices(
            listOf(
                PriceData(12.5, 13.0, startTime, startTime.plusMinutes(30)),
                PriceData(12.6, 13.1, startTime.plusMinutes(30), startTime.plusMinutes(60)),
                PriceData(12.7, 13.2, startTime.plusMinutes(60), startTime.plusMinutes(90)),
                PriceData(12.8, 13.3, startTime.plusMinutes(90), startTime.plusMinutes(120))
            )
        )
        val intensity = NationalGridData(
            listOf(
                IntensityData(startTime, startTime.plusMinutes(30), Intensity(100, null, "")),
                IntensityData(startTime.plusMinutes(30), startTime.plusMinutes(60), Intensity(99, null, "")),
                IntensityData(startTime.plusMinutes(60), startTime.plusMinutes(90), Intensity(101, null, "")),
                IntensityData(startTime.plusMinutes(90), startTime.plusMinutes(120), Intensity(102, null, ""))
            )
        )
        val expectedElectricity = Electricity(
            listOf(
                ElectricityData(startTime, startTime.plusMinutes(30), BigDecimal("13.0"), BigDecimal("100")),
                ElectricityData(
                    startTime.plusMinutes(30),
                    startTime.plusMinutes(60),
                    BigDecimal("13.1"),
                    BigDecimal("99")
                ),
                ElectricityData(
                    startTime.plusMinutes(60),
                    startTime.plusMinutes(90),
                    BigDecimal("13.2"),
                    BigDecimal("101")
                ),
                ElectricityData(
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
        val prices = Prices(
            listOf(
                PriceData(12.5, 13.0, startTime, startTime.plusMinutes(30)),
                PriceData(12.6, 13.1, startTime.plusMinutes(30), startTime.plusMinutes(60)),
                PriceData(12.7, 13.2, startTime.plusMinutes(60), startTime.plusMinutes(90)),
                PriceData(12.8, 13.3, startTime.plusMinutes(120), startTime.plusMinutes(150))
            )
        )
        val intensity = NationalGridData(
            listOf(
                IntensityData(startTime, startTime.plusMinutes(30), Intensity(100, null, "")),
                IntensityData(startTime.plusMinutes(30), startTime.plusMinutes(60), Intensity(99, null, "")),
                IntensityData(startTime.plusMinutes(60), startTime.plusMinutes(90), Intensity(101, null, "")),
                IntensityData(startTime.plusMinutes(90), startTime.plusMinutes(120), Intensity(102, null, ""))
            )
        )
        val expectedElectricity = Electricity(
            listOf(
                ElectricityData(startTime, startTime.plusMinutes(30), BigDecimal("13.0"), BigDecimal("100")),
                ElectricityData(
                    startTime.plusMinutes(30),
                    startTime.plusMinutes(60),
                    BigDecimal("13.1"),
                    BigDecimal("99")
                ),
                ElectricityData(
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

    @Test
    fun `creates electricity data only with data at same time slots even when not a full half hour`() {
        val prices = Prices(
            listOf(
                PriceData(12.5, 13.0, startTime, startTime.plusMinutes(30)),
                PriceData(12.6, 13.1, startTime.plusMinutes(30), startTime.plusMinutes(60)),
                PriceData(12.7, 13.2, startTime.plusMinutes(60), startTime.plusMinutes(75)),
                PriceData(12.8, 13.3, startTime.plusMinutes(75), startTime.plusMinutes(90))
            )
        )
        val intensity = NationalGridData(
            listOf(
                IntensityData(startTime, startTime.plusMinutes(15), Intensity(100, null, "")),
                IntensityData(startTime.plusMinutes(15), startTime.plusMinutes(30), Intensity(102, null, "")),
                IntensityData(startTime.plusMinutes(30), startTime.plusMinutes(60), Intensity(99, null, "")),
                IntensityData(startTime.plusMinutes(60), startTime.plusMinutes(90), Intensity(101, null, ""))
            )
        )
        val expectedElectricity = Electricity(
            listOf(
                ElectricityData(startTime, startTime.plusMinutes(15), BigDecimal("13.0"), BigDecimal("100")),
                ElectricityData(
                    startTime.plusMinutes(15),
                    startTime.plusMinutes(30),
                    BigDecimal("13.0"),
                    BigDecimal("102")
                ),
                ElectricityData(
                    startTime.plusMinutes(30),
                    startTime.plusMinutes(60),
                    BigDecimal("13.1"),
                    BigDecimal("99")
                ),
                ElectricityData(
                    startTime.plusMinutes(60),
                    startTime.plusMinutes(75),
                    BigDecimal("13.2"),
                    BigDecimal("101")
                ),
                ElectricityData(
                    startTime.plusMinutes(75),
                    startTime.plusMinutes(90),
                    BigDecimal("13.3"),
                    BigDecimal("101")
                )
            )
        )

        val electricity = calculator.createElectricityFrom(prices, intensity)

        assertThat(electricity, equalTo(expectedElectricity))
    }

    @Test
    fun `calculates charge time using intensity limit`() {
        fakeLimit.setIntensityChargeTime(100.0, startTime.toString() to startTime.plusMinutes(45).toString())

        val chargeTime = calculator.getChargeTime(calculatorData.copy(intensityLimit = BigDecimal(100)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime, startTime.plusMinutes(45))))
    }

    @Test
    fun `calculates charge time using full intensity weight if intensity limit not possible`() {
        fakeWeights.setChargeTime(FakeWeights(0.0, 1.0), startTime.plusMinutes(15).toString() to startTime.plusMinutes(60).toString())

        val chargeTime = calculator.getChargeTime(calculatorData.copy(intensityLimit = BigDecimal(100)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime.plusMinutes(15), startTime.plusMinutes(60))))
    }

    @Test
    fun `calculates charge time using price limit`() {
        fakeLimit.setPriceChargeTime(14.0, startTime.toString() to startTime.plusMinutes(45).toString())

        val chargeTime = calculator.getChargeTime(calculatorData.copy(priceLimit = BigDecimal(14.0)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime, startTime.plusMinutes(45))))
    }

    @Test
    fun `calculates charge time using full price weight if intensity limit not possible`() {
        fakeWeights.setChargeTime(FakeWeights(1.0, 0.0), startTime.plusMinutes(15).toString() to startTime.plusMinutes(60).toString())

        val chargeTime = calculator.getChargeTime(calculatorData.copy(priceLimit = BigDecimal(14.0)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime.plusMinutes(15), startTime.plusMinutes(60))))
    }

    @Test
    fun `calculates charge time using weights if present in request`() {
        fakeWeights.setChargeTime(FakeWeights(1.0, 0.5), startTime.plusMinutes(15).toString() to startTime.plusMinutes(60).toString())

        val chargeTime = calculator.getChargeTime(calculatorData.copy(weights = Weights(1.0, 0.5)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime.plusMinutes(15), startTime.plusMinutes(60))))
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
    override fun fortyEightHourIntensity(time: ZonedDateTime): Result<NationalGridData, Failed> {
        TODO("Not yet implemented")
    }
}
