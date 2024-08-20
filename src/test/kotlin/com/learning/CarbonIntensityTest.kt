package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.junit.jupiter.api.Test

class CarbonIntensityTest {

    @Test
    fun `Ping test`() {
        assertThat(
            Response(OK).body("pong"),
            equalTo(
                app(SetHostFrom(Uri.of("http://localhost:1000")).then(JavaHttpClient()))(Request(GET, "/ping"))
            )
        )
    }
}
