package com.intensity.central

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
    fun `calls national grid and updates scheduler if no data present in scheduler then returns intensities`() {
        nationalGrid.setDateData(date, List(96) { 212 }, List(96) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(212)},"date":"${
                    date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'"))
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

    private fun intensityList(intensity: Int) =
        List(96) { intensity }.joinToString(prefix = "[", separator = ",", postfix = "]")
}
