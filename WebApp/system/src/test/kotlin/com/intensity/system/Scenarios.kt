package com.intensity.system

import com.intensity.central.LimitCalculatorCloud
import com.intensity.central.WeightsCalculatorCloud
import com.intensity.central.carbonIntensity
import com.intensity.core.chargeTimeLens
import com.intensity.limitcalculator.limitCalculatorApp
import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import com.intensity.weightedcalculator.weightedCalculatorApp
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class Scenarios {
    private val customer = Customer()

    @Test
    fun `finding the best charge time for intensity`() {
        customer `wants to charge between at` "2025-04-10 10:30:00" `and ending at` "2025-04-10 15:30:00" `for` "30 minutes"

        customer `wants the charge time for the` "lowest intensity"

        customer `should start charging at` "2025-04-10 12:30:00" `and end charging at` "2025-04-10 13:00:00"
    }
}

class Customer {
    private val nationalGridFake = FakeNationalGrid().apply {
        val forecasts = MutableList(10) { 100 }
        forecasts[4] = 99
        this.setDateData(ZonedDateTime.parse("2025-04-10T10:30:00Z"), forecasts)
    }
    private val octopusFake = FakeOctopus()
    private val app = carbonIntensity(
        NationalGridCloud(nationalGridFake),
        OctopusCloud(octopusFake),
        LimitCalculatorCloud(limitCalculatorApp()),
        WeightsCalculatorCloud(weightedCalculatorApp())
    )
    private var startTime = ""
    private var endTime = ""
    private var minutes = 30
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
        return this
    }

    infix fun `wants the charge time for the`(condition: String) {
        response = app(Request(POST, "/intensities/charge-time").body("""{
            "start":"$startTime",
            "end":"$endTime",
            "time":$minutes
        }""".trimMargin())).also { println(it) }
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

    private fun String.toISO8601() = replace(" ", "T") + "Z"
}
