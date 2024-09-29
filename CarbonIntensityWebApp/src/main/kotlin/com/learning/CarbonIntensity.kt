package com.learning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main() {
    val server = carbonIntensityServer(9000).start()

    println("Server started on " + server.port())
}

fun carbonIntensityServer(port: Int): Http4kServer {
    return carbonIntensity().asServer(SunHttp(port))
}

fun carbonIntensity(): (Request) -> Response {
    return CatchLensFailure.then(
        routes(
            "/ping" bind GET to {
                Response(OK).body("pong")
            }
        )
    )
}

const val schedulerPattern: String = "yyyy-MM-dd'T'HH:mm:ss"

object SchedulerJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(
            BiDiMapping(
                { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                { instant -> formatWith(schedulerPattern).format(instant) }
            )
        )
        .done()
        .deactivateDefaultTyping()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)

const val gridPattern = "yyyy-MM-dd'T'HH:mmX"

object NationalGridJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(
            BiDiMapping(
                { timestamp -> Instant.from(formatWith(gridPattern).parse(timestamp)) },
                { instant -> formatWith(gridPattern).format(instant) }
            )
        )
        .done()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)

fun formatWith(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    .withZone(ZoneOffset.UTC)
