package com.learning

import com.learning.Matchers.assertReturnsString
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class AddFunctionalTest {
    private val client = app(SetHostFrom(Uri.of("http://localhost:1000")).then(JavaHttpClient()))

    @Test
    fun `adds values together`() {
        client(Request(GET, "/add?value=5&value=2")).assertReturnsString("7")
    }

    @Test
    fun `answer is zero when no values`() {
        client(Request(GET, "/add")).assertReturnsString("0")
    }

    @Test
    fun `bad request when some values are not numbers`() {
        assertThat(
            client(Request(GET, "/add?value=1&value=notANumber")),
            hasStatus(BAD_REQUEST)
        )
    }
}
