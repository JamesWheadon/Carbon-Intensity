package com.learning

import com.learning.Matchers.assertReturnsString
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class MultiplyFunctionalTest {
    private val env = AppEnvironment()

    @Test
    fun `multiplies values together`() {
        env.client(Request(GET, "/multiply?value=5&value=2")).assertReturnsString("10")
        assertThat(env.recorder.calls, equalTo(listOf(10)))
    }

    @Test
    fun `answer is zero when no values`() {
        env.client(Request(GET, "/multiply")).assertReturnsString("0")
        assertThat(env.recorder.calls, equalTo(listOf(0)))
    }

    @Test
    fun `bad request when some values are not numbers`() {
        assertThat(
            env.client(Request(GET, "/multiply?value=1&value=notANumber")),
            hasStatus(BAD_REQUEST)
        )
        assertThat(env.recorder.calls, equalTo(emptyList()))
    }
}
