package com.learning

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val app: HttpHandler = routes(
    "/ping" bind GET to {
        Response(OK).body("pong")
    }
)

fun main() {
    val server = carbonIntensityServer(9000)

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int): Http4kServer =
    { _: Request -> Response(OK) }.asServer(SunHttp(port))
