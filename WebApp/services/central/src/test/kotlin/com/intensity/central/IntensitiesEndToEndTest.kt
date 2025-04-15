package com.intensity.central

import com.intensity.coretest.formatted
import com.intensity.coretest.hasBody
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class IntensitiesEndToEndTest : EndToEndTest() {
    private val date = LocalDate.now(ZoneId.of("UTC")).atStartOfDay(ZoneId.of("UTC").normalized())

    @Test
    fun `calls national grid and returns intensities`() {
        nationalGrid.setDateData(date, List(96) { 212 }, List(96) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(212, 96)},"date":"${
                    date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'"))
                }"}"""
            )
        )
    }

    @Test
    fun `accepts start time without end time and returns 48 hour of intensities`() {
        nationalGrid.setDateData(date.plusDays(5), List(96) { 134 })

        val response = User(events, server).call(
            Request(POST, "/intensities?start=${date.plusDays(5).formatted()}")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(134, 96)},"date":"${
                    date.plusDays(5).formatted()
                }"}"""
            )
        )
    }

    @Test
    fun `accepts end time without start time and returns intensities between now and end`() {
        nationalGrid.setDateData(date, List(96) { 134 })

        val response = User(events, server).call(
            Request(POST, "/intensities?end=${date.plusHours(7).formatted()}")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(134, 14)},"date":"${
                    date.formatted()
                }"}"""
            )
        )
    }

    @Test
    fun `handles error from national grid with the correct response`() {
        nationalGrid.shouldFail()

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.body.toString(), equalTo(getErrorResponse("Failed to get intensity data")))
    }

    @Test
    fun `calculates lowest charge time based on intensity data`() {
        nationalGrid.setDateData(time, listOf(100, 100, 101, 101, 100), listOf(null, null, null, null, null))
        weightsCalculator.setChargeTime(FakeWeights(0.0, 1.0), time.formatted() to time.plusMinutes(45).formatted())

        val response = User(events, server).call(
            Request(POST, "/intensities/charge-time").body(
                """{
                    "start":"${time.formatted()}",
                    "end":"${time.plusMinutes(150).formatted()}",
                    "time":45
                    }""".trimMargin()
            )
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody("""{"from":"2025-03-25T12:00:00Z","to":"2025-03-25T12:45:00Z"}""")
        )
    }

    private fun intensityList(intensity: Int, dataPoints: Int) =
        List(dataPoints) { intensity }.joinToString(prefix = "[", separator = ",", postfix = "]")
}
