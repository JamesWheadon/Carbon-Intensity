package com.intensity.central

import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.nationalgrid.nationalGridClient
import com.intensity.octopus.Octopus
import com.intensity.octopus.OctopusCloud
import com.intensity.octopus.octopusClient
import com.intensity.openapi.openApi3
import com.intensity.scheduler.PythonScheduler
import com.intensity.scheduler.Scheduler
import com.intensity.scheduler.schedulerClient
import org.http4k.contract.PreFlightExtraction.Companion.None
import org.http4k.contract.contract
import org.http4k.core.then
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters.Cors
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    val schedulerUrl = System.getenv("SCHEDULER_URL") ?: "http://localhost:8080"
    val server = carbonIntensityServer(
        port,
        PythonScheduler(schedulerClient(schedulerUrl)),
        NationalGridCloud(nationalGridClient()),
        OctopusCloud(octopusClient())
    ).start()
    println("Server started on " + server.port())
}

val corsMiddleware = Cors(CorsPolicy.UnsafeGlobalPermissive)

fun carbonIntensityServer(port: Int, scheduler: Scheduler, nationalGrid: NationalGrid, octopus: Octopus): Http4kServer {
    return carbonIntensity(scheduler, nationalGrid, octopus).asServer(SunHttp(port))
}

fun carbonIntensity(scheduler: Scheduler, nationalGrid: NationalGrid, octopus: Octopus) =
    corsMiddleware.then(
        routes(
            contractRoutes(scheduler, nationalGrid),
            octopusProducts(octopus),
            octopusPrices(octopus)
        )
    )

private fun contractRoutes(scheduler: Scheduler, nationalGrid: NationalGrid) = contract {
    renderer = openApi3()
    descriptionPath = "/openapi.json"
    preFlightExtraction = None
    routes += chargeTimes(scheduler)
    routes += intensities(scheduler, nationalGrid)
}
