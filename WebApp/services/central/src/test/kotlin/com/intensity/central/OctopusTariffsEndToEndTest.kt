package com.intensity.central

import com.intensity.coretest.formatted
import com.intensity.coretest.hasBody
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class OctopusTariffsEndToEndTest : EndToEndTest() {
    private val currentTime = LocalDateTime.now().atZone(ZoneId.of("UTC").normalized())

    @Test
    fun `returns octopus products`() {
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            listOf(23.4, 26.0, 24.3)
        )

        val response = app(Request(GET, "/tariffs"))

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"products":[{"name":"AGILE-24-10-01","tariffs":["E-1R-AGILE-24-10-01-A"]}]}""".trimIndent()
            )
        )
    }

    @Test
    fun `returns only the octopus products that have tariffs`() {
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            listOf(23.4, 26.0, 24.3)
        )
        octopus.incorrectOctopusProductCode("error-product")

        val response = app(Request(GET, "/tariffs"))

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"products":[{"name":"AGILE-24-10-01","tariffs":["E-1R-AGILE-24-10-01-A"]}]}""".trimIndent()
            )
        )
    }

    @Test
    fun `handles failure case when unable to retrieve products`() {
        octopus.fail()

        val response = app(Request(GET, "/tariffs"))

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"error":"Failure communicating with Octopus"}""".trimIndent()
            )
        )
    }

    @Test
    fun `handles failure case when no correct products are found`() {
        octopus.incorrectOctopusProductCode("error-product")

        val response = app(Request(GET, "/tariffs"))

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"error":"No Octopus products"}""".trimIndent()
            )
        )
    }

    @Test
    fun `returns price data for a tariff`() {
        val currentHalfHour = zonedDateTimeAtHalfHour(currentTime)
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to currentHalfHour,
            mutableListOf(23.4, 26.0, 24.3)
        )

        val response = app(Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-A"))

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody(
                """{
                        "prices":[
                            {
                                "wholesalePrice":23.4,
                                "retailPrice":24.57,
                                "from":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(60)).formatted()}",
                                "to":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(90)).formatted()}"
                            },
                            {
                                "wholesalePrice":26.0,
                                "retailPrice":27.3,
                                "from":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(30)).formatted()}",
                                "to":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(60)).formatted()}"
                            },
                            {
                                "wholesalePrice":24.3,
                                "retailPrice":25.515,
                                "from":"${zonedDateTimeAtHalfHour(currentTime).formatted()}",
                                "to":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(30)).formatted()}"
                            }
                        ]
                    }"""
            )
        )
    }

    @Test
    fun `accepts an end time for pricing data`() {
        val currentHalfHour = zonedDateTimeAtHalfHour(currentTime)
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to currentHalfHour,
            mutableListOf(23.4, 26.0, 24.3)
        )

        val response = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-A?end=${zonedDateTimeAtHalfHour(currentTime.plusMinutes(30)).formatted()}")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody(
                """{
                        "prices":[
                            {
                                "wholesalePrice":23.4,
                                "retailPrice":24.57,
                                "from":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(30)).formatted()}",
                                "to":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(60)).formatted()}"
                            },
                            {
                                "wholesalePrice":26.0,
                                "retailPrice":27.3,
                                "from":"${zonedDateTimeAtHalfHour(currentTime).formatted()}",
                                "to":"${zonedDateTimeAtHalfHour(currentTime.plusMinutes(30)).formatted()}"
                            }
                        ]
                    }"""
            )
        )
    }

    @Test
    fun `accepts a start time for pricing data`() {
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            mutableListOf(23.4, 26.0, 24.3)
        )

        val response = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-A?start=2023-03-26T00:00:00Z")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response,
            hasBody(
                """{
                        "prices":[
                            {
                                "wholesalePrice":23.4,
                                "retailPrice":24.57,
                                "from":"2023-03-26T01:00:00Z",
                                "to":"2023-03-26T01:30:00Z"
                            },
                            {
                                "wholesalePrice":26.0,
                                "retailPrice":27.3,
                                "from":"2023-03-26T00:30:00Z",
                                "to":"2023-03-26T01:00:00Z"
                            },
                            {
                                "wholesalePrice":24.3,
                                "retailPrice":25.515,
                                "from":"2023-03-26T00:00:00Z",
                                "to":"2023-03-26T00:30:00Z"
                            }
                        ]
                    }"""
            )
        )
    }

    @Test
    fun `handles tariff not existing`() {
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            mutableListOf(23.4, 26.0, 24.3)
        )

        val response = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-D")
        )

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response, hasBody("""{"error":"Incorrect Octopus tariff code"}"""))
    }

    @Test
    fun `handles octopus not responding`() {
        octopus.fail()

        val response = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-D")
        )

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
        assertThat(response, hasBody("""{"error":"Failure communicating with Octopus"}"""))
    }

    @Test
    fun `handles error when end time before start time`() {
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to ZonedDateTime.parse("2023-03-26T00:00:00Z"),
            mutableListOf(23.4, 26.0, 24.3)
        )

        val response = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-A?start=2023-03-26T00:00:00Z&end=2023-03-25T23:59:59Z")
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response, hasBody("""{"error":"Invalid request"}"""))
    }

    @Test
    fun `handles invalid timestamps for start and end times`() {
        val invalidStart = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-A?start=2023-03-26.00:00:00Z")
        )

        assertThat(invalidStart.status, equalTo(BAD_REQUEST))
        assertThat(invalidStart, hasBody("""{"error":"Invalid request"}"""))

        val invalidEnd = app(
            Request(GET, "/tariffs/AGILE-24-10-01/E-1R-AGILE-24-10-01-A?end=23-03-25T23:59:59Z")
        )

        assertThat(invalidEnd.status, equalTo(BAD_REQUEST))
        assertThat(invalidEnd, hasBody("""{"error":"Invalid request"}"""))
    }

    private fun zonedDateTimeAtHalfHour(time: ZonedDateTime) =
        time.truncatedTo(ChronoUnit.HOURS).plusMinutes(time.minute / 30 * 30L)
}
