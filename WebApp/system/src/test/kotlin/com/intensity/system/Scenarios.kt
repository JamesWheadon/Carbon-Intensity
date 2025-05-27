package com.intensity.system

import com.intensity.central.LimitCalculatorCloud
import com.intensity.central.WeightsCalculatorCloud
import com.intensity.central.carbonIntensity
import com.intensity.core.chargeTimeLens
import com.intensity.limitcalculator.limitCalculatorApp
import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.observability.TestProfile.Local
import com.intensity.observability.TestTracingOpenTelemetry
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import com.intensity.weightedcalculator.weightedCalculatorApp
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.time.ZonedDateTime

class Scenarios {
    private val customer = Customer()

    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        customer.approveTracing(testInfo.displayName)
    }

    @Test
    fun `customer finds the best charge time for intensity`() {
        customer `wants to charge between at` "2025-04-10 10:30:00" `and ending at` "2025-04-10 15:30:00" `for` "30 minutes"

        customer `wants the charge time for the` "lowest intensity"

        customer `should start charging at` "2025-04-10 12:30:00" `and end charging at` "2025-04-10 13:00:00"
    }

    @Test
    fun `customer finds the best charge time for price`() {
        customer `is an octopus customer on product` "AGILE-24-10-01" `and tariff` "E-1R-AGILE-24-10-01-A"
        customer `wants to charge between at` "2025-04-10 09:00:00" `and ending at` "2025-04-10 17:00:00" `for` "1 hour"

        customer `wants the charge time for the` "lowest price"

        customer `should start charging at` "2025-04-10 10:30:00" `and end charging at` "2025-04-10 11:30:00"
    }

    @Test
    fun `customer finds the best charge time for price under a limited value for intensity`() {
        customer `is an octopus customer on product` "AGILE-24-10-01" `and tariff` "E-1R-AGILE-24-10-01-A"
        customer `wants to charge between at` "2025-04-10 09:00:00" `and ending at` "2025-04-10 17:00:00" `for` "1 hour"

        customer `wants the charge time for the lowest price with intensity under` "100"

        customer `should start charging at` "2025-04-10 14:00:00" `and end charging at` "2025-04-10 15:00:00"
    }

    @Test
    fun `customer finds the best charge time for intensity under a limited value for price`() {
        customer `is an octopus customer on product` "AGILE-24-10-01" `and tariff` "E-1R-AGILE-24-10-01-A"
        customer `wants to charge between at` "2025-04-10 09:00:00" `and ending at` "2025-04-10 17:00:00" `for` "1 hour"

        customer `wants the charge time for the lowest intensity with price under` "10.0"

        customer `should start charging at` "2025-04-10 14:00:00" `and end charging at` "2025-04-10 15:00:00"
    }

    @Test
    fun `customer finds the best charge time for intensity with a balance for intensity and price`() {
        customer `is an octopus customer on product` "AGILE-24-10-01" `and tariff` "E-1R-AGILE-24-10-01-A"
        customer `wants to charge between at` "2025-04-10 09:00:00" `and ending at` "2025-04-10 17:00:00" `for` "45 minutes"

        customer `wants the charge time considering price and intensity` "equally"

        customer `should start charging at` "2025-04-10 16:00:00" `and end charging at` "2025-04-10 16:45:00"
    }
}

class Customer {
    private val nationalGridFake = FakeNationalGrid().apply {
        this.setDateData(
            ZonedDateTime.parse("2025-04-10T09:00:00Z"),
            listOf(101, 101, 101, 101, 101, 101, 100, 99, 100, 100, 100, 100, 100, 100, 90, 90)
        )
    }
    private val octopusFake = FakeOctopus().apply {
        this.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2025-04-10T09:00:00Z"),
            listOf(9.8, 9.8, 10.0, 10.0, 9.5, 9.5, 10.0, 10.0, 10.0, 10.0, 10.0, 9.0, 9.0, 10.0, 9.8, 9.8)
        )
    }
    private val centralOpenTelemetry = TestTracingOpenTelemetry(Local, "test")
    private val limitCalcOpenTelemetry = TestTracingOpenTelemetry(Local, "limit")
    private val app = carbonIntensity(
        NationalGridCloud(nationalGridFake, centralOpenTelemetry),
        OctopusCloud(octopusFake, centralOpenTelemetry),
        LimitCalculatorCloud(limitCalculatorApp(limitCalcOpenTelemetry), centralOpenTelemetry),
        WeightsCalculatorCloud(weightedCalculatorApp(), centralOpenTelemetry),
        centralOpenTelemetry
    )
    private var startTime = ""
    private var endTime = ""
    private var minutes = 30
    private var octopusProduct = ""
    private var octopusTariff = ""
    private lateinit var response: Response

    infix fun `wants to charge between at`(start: String): Customer {
        startTime = start.toISO8601()
        return this
    }

    infix fun `and ending at`(end: String): Customer {
        endTime = end.toISO8601()
        return this
    }

    infix fun `for`(time: String): Customer {
        minutes = time.split(" ")[0].toInt()
        if (time.split(" ")[1] == "hour") {
            minutes *= 60
        }
        return this
    }

    infix fun `wants the charge time for the`(condition: String): Customer {
        val request = when {
            condition == "lowest intensity" && octopusProduct == "" -> {
                Request(POST, "/intensities/charge-time").body("""{
                    "start":"$startTime",
                    "end":"$endTime",
                    "time":$minutes
                }""".trimMargin())
            }
            else -> {
                Request(POST, "/octopus/charge-time").body("""{
                    "product":"$octopusProduct",
                    "tariff":"$octopusTariff",
                    "start":"$startTime",
                    "end":"$endTime",
                    "time":$minutes,
                    "weights": {
                        "priceWeight":1.0,
                        "intensityWeight":0.0
                    }
                }""".trimMargin())
            }
        }
        response = app(request)
        return this
    }

    infix fun `wants the charge time for the lowest price with intensity under`(intensityLimit: String) {
        val request = Request(POST, "/octopus/charge-time").body("""{
            "product":"$octopusProduct",
            "tariff":"$octopusTariff",
            "start":"$startTime",
            "end":"$endTime",
            "time":$minutes,
            "intensityLimit":$intensityLimit
        }""".trimMargin())
        response = app(request)
    }

    infix fun `wants the charge time for the lowest intensity with price under`(priceLimit: String) {
        val request = Request(POST, "/octopus/charge-time").body("""{
            "product":"$octopusProduct",
            "tariff":"$octopusTariff",
            "start":"$startTime",
            "end":"$endTime",
            "time":$minutes,
            "priceLimit":$priceLimit
        }""".trimMargin())
        response = app(request)
    }

    infix fun `wants the charge time considering price and intensity`(condition: String): Customer {
        response = app(Request(POST, "/octopus/charge-time").body("""{
                    "product":"$octopusProduct",
                    "tariff":"$octopusTariff",
                    "start":"$startTime",
                    "end":"$endTime",
                    "time":$minutes,
                    "weights": {
                        "priceWeight":0.5,
                        "intensityWeight":0.5
                    }
                }""".trimMargin()))
        return this
    }

    infix fun `should start charging at`(expectedStartTime: String): Customer {
        assertThat(chargeTimeLens(response).from, equalTo(ZonedDateTime.parse(
            expectedStartTime.toISO8601()
        )))
        return this
    }

    infix fun `and end charging at`(expectedEndTime: String): Customer {
        assertThat(chargeTimeLens(response).to, equalTo(ZonedDateTime.parse(expectedEndTime.toISO8601())))
        return this
    }

    infix fun `is an octopus customer on product`(octopusProductCode: String): Customer {
        octopusProduct = octopusProductCode
        return this
    }

    infix fun `and tariff`(octopusTariffCode: String) {
        octopusTariff = octopusTariffCode
    }

    private fun String.toISO8601() = replace(" ", "T") + "Z"

    fun approveTracing(testName: String) {
        centralOpenTelemetry.approveSpanDiagram(testName, limitCalcOpenTelemetry)
    }
}
