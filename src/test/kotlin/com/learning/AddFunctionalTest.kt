package com.learning

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test

class AddFunctionalTest {
    private val client = app

    @Test
    fun `adds values together`() {
        client(Request(GET, "/add?value=5&value=2")).assertReturnsString("7")
    }
}
