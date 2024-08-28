package com.learning

import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val server = carbonIntensityServer(9000).start()

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int): Http4kServer {
    return app().asServer(SunHttp(port))
}

fun app(): (Request) -> Response {
    return CatchLensFailure.then(
        routes(
            "/ping" bind GET to {
                Response(OK).body("pong")
            }
        )
    )
}
