package com.intensity.central

import com.intensity.scheduler.Intensities
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class IntensitiesEndToEndTest : EndToEndTest() {
    @Test
    fun `returns intensity data from the scheduler`() {
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, date.toInstant()))

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(212)},"date":"${
                    date.format(
                        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
                    )
                }"}"""
            )
        )
    }

    @Test
    fun `calls national grid and updates scheduler if no data present in scheduler then returns intensities`() {
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        nationalGrid.setDateData(date.toInstant(), List(97) { 210 }, List(97) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(210)},"date":"${
                    date.format(
                        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
                    )
                }"}"""
            )
        )
    }

    @Test
    fun `calls national grid and updates scheduler if scheduler is out of date`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        nationalGrid.setDateData(date.toInstant(), List(97) { 210 }, List(97) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(210)},"date":"${
                    date.format(
                        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
                    )
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
    fun `handles error when scheduler fails to accept data`() {
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        nationalGrid.setDateData(date.toInstant(), List(99) { 210 }, List(99) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
        assertThat(response.body.toString(), equalTo(getErrorResponse("Error updating scheduler intensities")))
    }

    private fun intensityList(intensity: Int) =
        List(96) { intensity }.joinToString(prefix = "[", separator = ",", postfix = "]")
}
