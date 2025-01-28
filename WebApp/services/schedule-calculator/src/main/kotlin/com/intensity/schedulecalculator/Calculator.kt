package com.intensity.schedulecalculator

import java.math.BigDecimal

fun calculate(intensities: List<Any>, prices: List<Any>, intensityWeight: Int, priceWeight: Int): List<Any> {
    return listOf()
}

fun normalize(values: List<BigDecimal>): List<BigDecimal> {
    val max = values.max()
    return values.map { it.divide(max) }
}
