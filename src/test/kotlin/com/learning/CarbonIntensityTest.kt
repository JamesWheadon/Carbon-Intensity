package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class CarbonIntensityTest {

    @Test
    fun `Ping test`() {
        assertThat(Response(OK).body("pong"), equalTo(app(Request(GET, "/ping"))))
    }
}
