package com.intensity.weightedcalculator

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class WeightedCalculatorAppKtTest {
    private val app = weightedCalculatorApp()

    @Test
    fun `returns the result of the schedule calculation`() {
        val request = Request(POST, "/calculate").body(
            """{
                    "time":45,
                    "priceWeight":1.0,
                    "intensityWeight":1.0,
                    "electricity": {
                        "data": [
                           {
                               "from":"2025-03-02T12:00:00Z",
                               "to":"2025-03-02T12:30:00Z",
                               "price":23.56,
                               "intensity":145.0
                           },
                           {
                               "from":"2025-03-02T12:30:00Z",
                               "to":"2025-03-02T13:00:00Z",
                               "price":23.57,
                               "intensity":145.0
                           }
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
    fun `returns a bad request if data windows overlap`() {
        val request = Request(POST, "/calculate").body(
            """{
                    "time":45,
                    "priceWeight":1.0,
                    "intensityWeight":1.0,
                    "electricity": {
                        "data": [
                           {
                               "from":"2025-03-02T12:00:00Z",
                               "to":"2025-03-02T12:30:00Z",
                               "price":23.56,
                               "intensity":145.0
                           },
                           {
                               "from":"2025-03-02T12:15:00Z",
                               "to":"2025-03-02T12:45:00Z",
                               "price":23.57,
                               "intensity":145.0
                           }
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(
            response.bodyString(), equalTo(
                """{"error":"Overlapping data windows"}""".trimIndent()
            )
        )
    }

    @Test
    fun `returns a bad request if no schedule possible`() {
        val request = Request(POST, "/calculate").body(
            """{
                    "time":45,
                    "priceWeight":1.0,
                    "intensityWeight":1.0,
                    "electricity": {
                        "data": [
                           {
                               "from":"2025-03-02T12:00:00Z",
                               "to":"2025-03-02T12:30:00Z",
                               "price":23.56,
                               "intensity":145.0
                           }
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.bodyString(), equalTo("""{"error":"No charge time possible"}""".trimIndent()))
    }

    @Test
    fun `returns a bad request if invalid request body`() {
        val request = Request(POST, "/calculate").body(
            """{
                    "time":45,
                    "intensityWeight":1.0,
                    "electricity": {
                        "data": [
                           {
                               "from":"2025-03-02T12:00:00Z",
                               "to":"2025-03-02T12:30:00Z",
                               "price":23.56,
                               "intensity":145.0
                           }
                        ]
                    }
                }""".trimIndent()
        )

        val response = app(request)

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("""{"error":"Invalid Request"}""".trimIndent()))
    }
}
