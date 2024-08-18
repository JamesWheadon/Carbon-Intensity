package com.learning

import com.learning.Matchers.assertReturnsString
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test

class MultiplyFunctionalTest {
    private val client = app

    @Test
    fun `multiplies values together`() {
        client(Request(GET, "/multiply?value=5&value=2")).assertReturnsString("10")
    }
}
