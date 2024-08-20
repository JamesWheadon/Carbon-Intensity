package com.learning

import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val server = carbonIntensityServer(9000, Uri.of("http://localhost:1000")).start()

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int, recorderBaseUri: Uri): Http4kServer {
    return app(SetHostFrom(recorderBaseUri).then(JavaHttpClient())).asServer(SunHttp(port))
}

fun app(recorderHttp: HttpHandler): (Request) -> Response {
    val recorder = Recorder(recorderHttp)
    return CatchLensFailure.then(
        routes(
            "/ping" bind GET to {
                Response(OK).body("pong")
            },
            "/add" bind GET to calculateResult(recorder) { values -> values.sum() },
            "/multiply" bind GET to calculateResult(recorder) { values -> values.fold(1) { acc, next -> acc * next } }
        )
    )
}


private fun calculateResult(recorder: Recorder, calculation: (List<Int>) -> Int): (Request) -> Response = { request ->
    val values = extractQueryValuesFrom(request)
    val answer = performCalculation(values, calculation)
    recorder.record(answer)
    Response(OK).body(answer.toString())
}

private fun extractQueryValuesFrom(request: Request) = Query.int().multi.defaulted("value", emptyList())(request)

private fun performCalculation(values: List<Int>, operation: (List<Int>) -> Int) = if (values.isNotEmpty()) {
    operation(values)
} else {
    0
}
