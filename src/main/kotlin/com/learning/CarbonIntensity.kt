package com.learning

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
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
    "/add" bind GET to calculateResult { values -> values.sum() },
    "/multiply" bind GET to calculateResult { values -> values.fold(1) { acc, next -> acc * next } }
)

private fun calculateResult(calculation: (List<Int>) -> Int): (Request) -> Response = { request ->
    val values = extractQueryValuesFrom(request)
    Response(OK).body(performCalculation(values, calculation).toString())
}

private fun extractQueryValuesFrom(request: Request) = Query.int().multi.defaulted("value", emptyList())(request)

private fun performCalculation(values: List<Int>, operation: (List<Int>) -> Int) = if (values.isNotEmpty()) {
    operation(values)
} else {
    0
}

val app: HttpHandler = CatchLensFailure.then(
    appRoutes
)

fun main() {
    val server = carbonIntensityServer(9000).start()

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int): Http4kServer = app.asServer(SunHttp(port))
