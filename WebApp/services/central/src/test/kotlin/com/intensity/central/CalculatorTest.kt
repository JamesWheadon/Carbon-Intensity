package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.ElectricityData
import com.intensity.core.Failed
import com.intensity.coretest.isFailure
import com.intensity.coretest.isSuccess
import com.intensity.nationalgrid.Intensity
import com.intensity.nationalgrid.IntensityData
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridData
import com.intensity.observability.TestProfile.Local
import com.intensity.observability.TestTracingOpenTelemetry
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
import dev.forkhandles.result4k.Success
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CalculatorTest {
    private val fakeLimit = FakeLimitCalculator()
    private val fakeWeights = FakeWeightsCalculator()
    private val openTelemetry = TestTracingOpenTelemetry(Local, "calculator")
    private val calculator = Calculator(
        OctopusFake(),
        NationalGridFake(),
        LimitCalculatorCloud(fakeLimit, openTelemetry),
        WeightsCalculatorCloud(fakeWeights, openTelemetry),
        openTelemetry
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

    @AfterEach
    fun tearDown() {
        openTelemetry.shutdown()
    }

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
        fakeWeights.setChargeTime(
            FakeWeights(0.0, 1.0),
            startTime.plusMinutes(15).toString() to startTime.plusMinutes(60).toString()
        )

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
        fakeWeights.setChargeTime(
            FakeWeights(1.0, 0.0),
            startTime.plusMinutes(15).toString() to startTime.plusMinutes(60).toString()
        )

        val chargeTime = calculator.getChargeTime(calculatorData.copy(priceLimit = BigDecimal(14.0)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime.plusMinutes(15), startTime.plusMinutes(60))))
    }

    @Test
    fun `calculates charge time using weights if present in request`() {
        fakeWeights.setChargeTime(
            FakeWeights(1.0, 0.5),
            startTime.plusMinutes(15).toString() to startTime.plusMinutes(60).toString()
        )

        val chargeTime = calculator.getChargeTime(calculatorData.copy(weights = Weights(1.0, 0.5)), electricity)

        assertThat(chargeTime, isSuccess(ChargeTime(startTime.plusMinutes(15), startTime.plusMinutes(60))))
    }

    @Test
    fun `returns failure if unable to calculate a charge time`() {
        assertThat(
            calculator.getChargeTime(calculatorData.copy(intensityLimit = BigDecimal(100)), electricity),
            isFailure(UnableToCalculateChargeTime)
        )
        assertThat(
            calculator.getChargeTime(calculatorData.copy(priceLimit = BigDecimal(14.0)), electricity),
            isFailure(UnableToCalculateChargeTime)
        )
        assertThat(
            calculator.getChargeTime(calculatorData.copy(weights = Weights(1.0, 0.5)), electricity),
            isFailure(UnableToCalculateChargeTime)
        )
    }

    @Test
    fun `spans and traces are created`() {
        fakeLimit.setIntensityChargeTime(100.0, startTime.toString() to startTime.plusMinutes(45).toString())

        calculator.calculate(
            CalculationData(
                OctopusProduct("product"),
                OctopusTariff("tariff"),
                startTime,
                startTime.plusHours(2),
                45L,
                intensityLimit = BigDecimal("100")
            )
        )

        val spans = openTelemetry.spans()
        val parentSpan = spans.first { it.name == "charge time calculation" }
        val gettingElectricityDataSpan = spans.first { it.name == "fetch electricity data" }
        val calculatingChargeTimeSpan = spans.first { it.name == "calculate charge time" }
        assertThat(gettingElectricityDataSpan.events.map { it.name }, equalTo(listOf("prices retrieved", "intensity retrieved", "electricity data created")))
        assertThat(gettingElectricityDataSpan.parentSpanId, equalTo(parentSpan.spanId))
        assertThat(calculatingChargeTimeSpan.events.map { it.name }, equalTo(listOf("calculated using intensity limit")))
        assertThat(calculatingChargeTimeSpan.parentSpanId, equalTo(parentSpan.spanId))
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
    ) = Success(
        Prices(
            listOf(
                PriceData(12.5, 13.0, start, start.plusMinutes(30)),
                PriceData(12.6, 13.1, start.plusMinutes(30), start.plusMinutes(60)),
                PriceData(12.7, 13.2, start.plusMinutes(60), start.plusMinutes(90)),
                PriceData(12.8, 13.3, start.plusMinutes(90), start.plusMinutes(120))
            )
        )
    )
}

class NationalGridFake : NationalGrid {
    override fun intensity(from: ZonedDateTime, to: ZonedDateTime) = Success(
        NationalGridData(
            listOf(
                IntensityData(from, from.plusMinutes(30), Intensity(100, null, "")),
                IntensityData(from.plusMinutes(30), from.plusMinutes(60), Intensity(99, null, "")),
                IntensityData(from.plusMinutes(60), from.plusMinutes(90), Intensity(101, null, "")),
                IntensityData(from.plusMinutes(90), from.plusMinutes(120), Intensity(102, null, ""))
            )
        )
    )
}
