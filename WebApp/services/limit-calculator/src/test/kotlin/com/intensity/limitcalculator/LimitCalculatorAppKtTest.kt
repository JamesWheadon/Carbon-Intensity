package com.intensity.limitcalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isNullOrEmptyString
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class LimitCalculatorAppKtTest {
    private val app = limitCalculatorApp()

    @Test
    fun `returns the charge time working under the intensity limit`() {
        val request = Request(POST, "/calculate/intensity/150").body(
            """{
                    "time":45,
                    "electricityData": [
                       {
                           "startTime":"2025-03-02T12:00:00Z",
                           "price":23.56,
                           "intensity":145.0
                       },
                       {
                           "startTime":"2025-03-02T12:30:00Z",
                           "price":23.55,
                           "intensity":145.0
                       }
                    ]
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
                    "electricityData": [
                       {
                           "startTime":"2025-03-02T12:00:00Z",
                           "price":23.56,
                           "intensity":144.0
                       },
                       {
                           "startTime":"2025-03-02T12:30:00Z",
                           "price":23.55,
                           "intensity":145.0
                       }
                    ]
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
                    "electricityData": [
                       {
                           "startTime":"2025-03-02T12:00:00Z",
                           "price":23.56,
                           "intensity":144.0
                       },
                       {
                           "startTime":"2025-03-02T12:30:00Z",
                           "price":23.55,
                           "intensity":145.0
                       }
                    ]
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), isNullOrEmptyString)
    }

    @Test
    fun `returns the correct response when the intensity limit is not a valid number`() {
        val request = Request(POST, "/calculate/intensity/invalid").body(
            """{
                    "time":45,
                    "electricityData": [
                       {
                           "startTime":"2025-03-02T12:00:00Z",
                           "price":23.56,
                           "intensity":144.0
                       },
                       {
                           "startTime":"2025-03-02T12:30:00Z",
                           "price":23.55,
                           "intensity":145.0
                       }
                    ]
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), isNullOrEmptyString)
    }
}
