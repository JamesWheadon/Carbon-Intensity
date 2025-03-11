package com.intensity.central

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.POST
import org.http4k.core.Request
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
            Request(POST, "/tariffs/octopus")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"products":[{"name":"AGILE-24-10-01","tariffs":["E-1R-AGILE-24-10-01-A"]}]}""".trimIndent()
            )
        )
    }
}
