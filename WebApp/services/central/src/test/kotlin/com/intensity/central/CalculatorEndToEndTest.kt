package com.intensity.central

import com.intensity.coretest.hasBody
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CalculatorEndToEndTest : EndToEndTest() {
    @Test
    fun `responds with optimal charge time`() {
        val time = ZonedDateTime.parse("2025-03-25T12:00:00Z")

        octopus.setPricesFor("octopusProduct", "octopusTariff" to time.formatted(), listOf(14.8, 13.7, 13.6))
        nationalGrid.setDateData(time.toInstant(), listOf(100, 100, 101), listOf(null, null, null))
        limitCalculator.setChargeTime("2025-03-25T12:30:00Z" to "2025-03-25T13:00:00Z")

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "intensityLimit":100
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody("""{"from":"${time.plusMinutes(30).formatted()}","to":"${time.plusMinutes(60).formatted()}"}""")
        )
    }
}

private fun ZonedDateTime.formatted() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
