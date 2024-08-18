package com.learning

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val appRoutes = routes(
    "/ping" bind GET to {
        Response(OK).body("pong")
    },
    "/add" bind GET to { request ->
        val valuesToAdd = Query.int().multi.defaulted("value", emptyList())(request)
        Response(OK).body(valuesToAdd.sum().toString())
    },
    "/multiply" bind GET to {
        Response(OK).body("8")
    }
)

val app: HttpHandler = CatchLensFailure.then(
    appRoutes
)

fun main() {
    val server = carbonIntensityServer(9000).start()

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int): Http4kServer = app.asServer(SunHttp(port))
