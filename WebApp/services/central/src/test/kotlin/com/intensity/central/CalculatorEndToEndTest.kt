package com.intensity.central

import com.intensity.coretest.formatted
import com.intensity.coretest.hasBody
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class CalculatorEndToEndTest : EndToEndTest() {
    @Test
    fun `responds with lowest price charge time using intensity limit`() {
        octopus.setPricesFor("octopusProduct", "octopusTariff" to time, listOf(14.8, 13.7, 13.6))
        nationalGrid.setDateData(time, listOf(100, 100, 101), listOf(null, null, null))
        limitCalculator.setIntensityChargeTime(100.0, "2025-03-25T12:30:00Z" to "2025-03-25T13:00:00Z")

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
            hasBody("""{"from":"2025-03-25T12:30:00Z","to":"2025-03-25T13:00:00Z"}""")
        )
    }

    @Test
    fun `responds with lowest intensity charge time when not possible under intensity limit`() {
        octopus.setPricesFor("octopusProduct", "octopusTariff" to time, listOf(14.8, 13.7, 13.6))
        nationalGrid.setDateData(time, listOf(100, 100, 101), listOf(null, null, null))
        weightsCalculator.setChargeTime(FakeWeights(0.0, 1.0), "2025-03-25T13:00:00Z" to "2025-03-25T13:30:00Z")

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "intensityLimit":99
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody("""{"from":"2025-03-25T13:00:00Z","to":"2025-03-25T13:30:00Z"}""")
        )
    }

    @Test
    fun `responds with lowest intensity charge time using price limit`() {
        octopus.setPricesFor("octopusProduct", "octopusTariff" to time, listOf(13.8, 13.7, 13.6))
        nationalGrid.setDateData(time, listOf(99, 100, 101), listOf(null, null, null))
        limitCalculator.setPriceChargeTime(14.0, "2025-03-25T12:00:00Z" to "2025-03-25T12:30:00Z")

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "priceLimit":14.0
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody("""{"from":"2025-03-25T12:00:00Z","to":"2025-03-25T12:30:00Z"}""")
        )
    }

    @Test
    fun `responds with lowest price charge time when not possible under price limit`() {
        octopus.setPricesFor("octopusProduct", "octopusTariff" to time, listOf(13.8, 13.7, 13.6))
        nationalGrid.setDateData(time, listOf(99, 100, 101), listOf(null, null, null))
        weightsCalculator.setChargeTime(FakeWeights(1.0, 0.0), "2025-03-25T13:00:00Z" to "2025-03-25T13:30:00Z")

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "priceLimit":13
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody("""{"from":"2025-03-25T13:00:00Z","to":"2025-03-25T13:30:00Z"}""")
        )
    }

    @Test
    fun `responds with best charge time according to weights`() {
        octopus.setPricesFor("octopusProduct", "octopusTariff" to time, listOf(13.8, 13.7, 13.6))
        nationalGrid.setDateData(time, listOf(99, 100, 101), listOf(null, null, null))
        weightsCalculator.setChargeTime(FakeWeights(1.0, 0.7), "2025-03-25T13:00:00Z" to "2025-03-25T13:30:00Z")

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "weights": {
                    "priceWeight":1.0,
                    "intensityWeight":0.7
                }
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody("""{"from":"2025-03-25T13:00:00Z","to":"2025-03-25T13:30:00Z"}""")
        )
    }

    @Test
    fun `no intensity data exists for calculation`() {
        octopus.setPricesFor("octopusProduct", "octopusTariff" to time, listOf(13.8, 13.7, 13.6))
        nationalGrid.shouldFail()

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "priceLimit":13
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `no price data exists for calculation`() {
        octopus.fail()
        nationalGrid.setDateData(time, listOf(99, 100, 101), listOf(null, null, null))

        val requestBody = """{
                "product":"octopusProduct",
                "tariff":"octopusTariff",
                "start":"${time.formatted()}",
                "end":"${time.plusMinutes(90).formatted()}",
                "time":30,
                "priceLimit":13
            }""".trimMargin()
        val response = User(events, server).call(
            Request(POST, "/octopus/charge-time").body(requestBody)
        )

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }
}
