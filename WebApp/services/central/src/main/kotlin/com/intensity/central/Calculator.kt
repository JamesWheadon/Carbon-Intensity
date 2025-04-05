package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.ElectricityData
import com.intensity.core.Failed
import com.intensity.nationalgrid.IntensityData
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridData
import com.intensity.octopus.Octopus
import com.intensity.octopus.PriceData
import com.intensity.octopus.Prices
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatZip
import java.math.BigDecimal
import java.time.ZonedDateTime

class Calculator(
    private val octopus: Octopus,
    private val nationalGrid: NationalGrid,
    private val limitCalc: LimitCalculator,
    private val weightsCalc: WeightsCalculator
) {
    fun calculate(calculationData: CalculationData): Result<ChargeTime, Failed> {
        val prices =
            octopus.prices(calculationData.product, calculationData.tariff, calculationData.start, calculationData.end)
        val intensity = nationalGrid.fortyEightHourIntensity(calculationData.start)
        return flatZip(prices, intensity) { priceData, intensityData ->
            Success(createElectricityFrom(priceData, intensityData))
        }.flatMap { electricity ->
            getChargeTime(calculationData, electricity)
        }
    }

    fun createElectricityFrom(
        prices: Prices,
        intensity: NationalGridData
    ): Electricity {
        val sortedPrice = prices.results.sortedBy { it.from }
        val sortedIntensity = intensity.data.sortedBy { it.from }
        val slots = sortedPrice.flatMap { price ->
            sortedIntensity.filter { it.overlaps(price.from, price.to) }.map { createElectricityData(price, it) }
        }
        return Electricity(slots)
    }

    fun getChargeTime(
        calculationData: CalculationData,
        electricity: Electricity
    ) = when {
        calculationData.intensityLimit != null -> Success(
            intensityLimitedChargeTime(
                electricity,
                calculationData.intensityLimit,
                calculationData.time
            )
        )

        calculationData.priceLimit != null -> Success(priceLimitedChargeTime(electricity,
            calculationData.priceLimit, calculationData.time))

        else -> Success(weightLimitedChargeTime(electricity, calculationData.weights!!, calculationData.time))
    }

    private fun createElectricityData(price: PriceData, intensity: IntensityData): ElectricityData {
        return ElectricityData(
            latest(price.from, intensity.from),
            earliest(price.to, intensity.to),
            price.retailPrice.toBigDecimal(),
            intensity.intensity.forecast.toBigDecimal()
        )
    }

    private fun latest(first: ZonedDateTime, second: ZonedDateTime) =
        if (first.isBefore(second)) {
            second
        } else {
            first
        }

    private fun earliest(first: ZonedDateTime, second: ZonedDateTime) =
        if (first.isBefore(second)) {
            first
        } else {
            second
        }

    private fun intensityLimitedChargeTime(
        electricity: Electricity,
        intensityLimit: BigDecimal,
        time: Long
    ): ChargeTime {
        val chargeTime = limitCalc.intensityLimit(
            electricity,
            intensityLimit,
            time
        )
        if (chargeTime == null) {
            return weightsCalc.chargeTime(
                electricity,
                Weights(0.0, 1.0),
                time
            )
        }
        return chargeTime
    }

    private fun priceLimitedChargeTime(electricity: Electricity, priceLimit: BigDecimal, time: Long): ChargeTime {
        val chargeTime = limitCalc.priceLimit(
            electricity,
            priceLimit,
            time
        )
        if (chargeTime == null) {
            return weightsCalc.chargeTime(
                electricity,
                Weights(1.0, 0.0),
                time
            )
        }
        return chargeTime
    }

    private fun weightLimitedChargeTime(electricity: Electricity, weights: Weights, time: Long): ChargeTime {
        return weightsCalc.chargeTime(electricity, weights, time)
    }
}

private fun IntensityData.overlaps(from: ZonedDateTime, to: ZonedDateTime) =
    this.from >= from && this.from < to || this.to > from && this.to <= to
