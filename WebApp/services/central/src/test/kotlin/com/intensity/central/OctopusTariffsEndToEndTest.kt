package com.intensity.central

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class OctopusTariffsEndToEndTest : EndToEndTest() {
    @Test
    fun `returns octopus products`() {
        octopus.setPricesFor(
            "AGILE-24-10-01",
            "E-1R-AGILE-24-10-01-A" to "2023-03-26T00:00:00Z",
            listOf(23.4, 26.0, 24.3)
        )

        val response = User(events, server).call(
            Request(GET, "/tariffs")
        )

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
            "E-1R-AGILE-24-10-01-A" to "2023-03-26T00:00:00Z",
            listOf(23.4, 26.0, 24.3)
        )
        octopus.incorrectOctopusProductCode("error-product")

        val response = User(events, server).call(
            Request(GET, "/tariffs")
        )

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

        val response = User(events, server).call(
            Request(GET, "/tariffs")
        )

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

        val response = User(events, server).call(
            Request(GET, "/tariffs")
        )

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"error":"No Octopus products"}""".trimIndent()
            )
        )
    }
}
