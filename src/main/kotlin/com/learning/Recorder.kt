package com.learning

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

class Recorder(private val client: HttpHandler) {
    fun record(value: Int) {
        client(Request(Method.POST, "/$value"))
    }
}
