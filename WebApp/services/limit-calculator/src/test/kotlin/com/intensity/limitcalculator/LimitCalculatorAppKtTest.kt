package com.intensity.limitcalculator

import com.intensity.observability.TestProfile.Local
import com.intensity.observability.TestTracingOpenTelemetry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class LimitCalculatorAppKtTest {
    private val openTelemetry = TestTracingOpenTelemetry(Local, "limit-test")
    private val app = limitCalculatorApp(openTelemetry)

    @Test
    fun `returns the charge time working under the intensity limit`() {
        val request = Request(POST, "/calculate/intensity/150").body(
            """{
                    "time":45,
                    "start":"2025-03-02T12:00:00Z",
                    "end":"2025-03-02T13:00:00Z",
                    "electricity": {
                        "data":[
                            ${halfHourJSON("2025-03-02T12:00:00Z", 23.56, 145.0)},
                            ${halfHourJSON("2025-03-02T12:30:00Z", 23.55, 145.0)}
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.bodyString(), equalTo(
                """{"from":"2025-03-02T12:15:00Z","to":"2025-03-02T13:00:00Z"}""".trimIndent()
            )
        )
    }

    @Test
    fun `returns the charge time working under the price limit`() {
        val request = Request(POST, "/calculate/price/25.19").body(
            """{
                    "time":45,
                    "start":"2025-03-02T12:00:00Z",
                    "end":"2025-03-02T13:00:00Z",
                    "electricity": {
                        "data": [
                            ${halfHourJSON("2025-03-02T12:00:00Z", 23.56, 144.0)},
                            ${halfHourJSON("2025-03-02T12:30:00Z", 23.55, 145.0)}
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.bodyString(), equalTo(
                """{"from":"2025-03-02T12:00:00Z","to":"2025-03-02T12:45:00Z"}""".trimIndent()
            )
        )
    }

    @Test
    fun `returns the correct response when the price limit is not a valid number`() {
        val request = Request(POST, "/calculate/price/invalid").body(
            """{
                    "time":45,
                    "start":"2025-03-02T12:00:00Z",
                    "end":"2025-03-02T13:00:00Z",
                    "electricity": {
                        "slots": [
                            ${halfHourJSON("2025-03-02T12:00:00Z", 23.56, 144.0)},
                            ${halfHourJSON("2025-03-02T12:30:00Z", 23.55, 145.0)}
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("""{"error":"Invalid Request"}""".trimIndent()))
    }

    @Test
    fun `returns the correct response when the intensity limit is not a valid number`() {
        val request = Request(POST, "/calculate/intensity/invalid").body(
            """{
                    "time":45,
                    "start":"2025-03-02T12:00:00Z",
                    "end":"2025-03-02T13:00:00Z",
                    "electricity": {
                        "slots": [
                            ${halfHourJSON("2025-03-02T12:00:00Z", 23.56, 144.0)},
                            ${halfHourJSON("2025-03-02T12:30:00Z", 23.55, 145.0)}
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("""{"error":"Invalid Request"}""".trimIndent()))
    }

    @Test
    fun `returns the correct response when calculation fails due to overlapping windows`() {
        val overlappingDataBody = """{
                    "time":75,
                    "start":"2025-03-02T12:00:00Z",
                    "end":"2025-03-02T13:00:00Z",
                    "electricity": {
                        "data": [
                            ${halfHourJSON("2025-03-02T12:00:00Z", 23.56, 144.0)},
                            ${halfHourJSON("2025-03-02T12:15:00Z", 23.55, 145.0)}
                        ]
                    }
                }""".trimIndent()

        val priceResponse = app(Request(POST, "/calculate/price/26.00").body(overlappingDataBody))

        assertThat(priceResponse.status, equalTo(BAD_REQUEST))
        assertThat(priceResponse.bodyString(), equalTo("""{"error":"Overlapping data windows"}"""))

        val intensityResponse = app(Request(POST, "/calculate/intensity/146.00").body(overlappingDataBody))

        assertThat(intensityResponse.status, equalTo(BAD_REQUEST))
        assertThat(intensityResponse.bodyString(), equalTo("""{"error":"Overlapping data windows"}"""))
    }

    @Test
    fun `returns the correct response when calculation fails due to insufficient data for time`() {
        val insufficientDataBody = """{
                    "time":75,
                    "start":"2025-03-02T12:00:00Z",
                    "end":"2025-03-02T13:00:00Z",
                    "electricity": {
                        "data": [
                            ${halfHourJSON("2025-03-02T12:00:00Z", 23.56, 144.0)},
                            ${halfHourJSON("2025-03-02T12:30:00Z", 23.55, 145.0)}
                        ]
                    }
                }""".trimIndent()

        val priceResponse = app(Request(POST, "/calculate/price/26.00").body(insufficientDataBody))

        assertThat(priceResponse.status, equalTo(NOT_FOUND))
        assertThat(priceResponse.bodyString(), equalTo("""{"error":"No charge time possible"}"""))

        val intensityResponse = app(Request(POST, "/calculate/intensity/150.00").body(insufficientDataBody))

        assertThat(intensityResponse.status, equalTo(NOT_FOUND))
        assertThat(intensityResponse.bodyString(), equalTo("""{"error":"No charge time possible"}"""))
    }

    private fun halfHourJSON(timestamp: String, price: Double, intensity: Double) =
        """{
                "from":"$timestamp",
                "to":"${ZonedDateTime.parse(timestamp).plusMinutes(30)}",
                "price":$price,
                "intensity":$intensity
            }"""
}
