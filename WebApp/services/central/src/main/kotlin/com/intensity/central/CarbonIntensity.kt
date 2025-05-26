package com.intensity.central

import com.intensity.core.errorResponseLens
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.nationalgrid.nationalGridClient
import com.intensity.observability.ManagedOpenTelemetry
import com.intensity.observability.TracingOpenTelemetry
import com.intensity.octopus.InvalidRequestFailed
import com.intensity.octopus.Octopus
import com.intensity.octopus.OctopusCloud
import com.intensity.octopus.octopusClient
import com.intensity.openapi.openApi3
import org.http4k.client.JavaHttpClient
import org.http4k.contract.PreFlightExtraction.Companion.None
import org.http4k.contract.contract
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.filter.ServerFilters.Cors
import org.http4k.lens.LensFailure
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    val limitCalculatorUrl = "http://localhost:9001"
    val weightsCalculatorUrl = "http://localhost:9002"
    val openTelemetry = TracingOpenTelemetry.noOp()
    val server = carbonIntensityServer(
        port,
        NationalGridCloud(nationalGridClient(), openTelemetry),
        OctopusCloud(octopusClient(), openTelemetry),
        LimitCalculatorCloud(calculatorClient(limitCalculatorUrl), openTelemetry),
        WeightsCalculatorCloud(calculatorClient(weightsCalculatorUrl), openTelemetry),
        openTelemetry
    ).start()
    println("Server started on " + server.port())
}

fun calculatorClient(limitCalculatorUrl: String) =
    ClientFilters.SetBaseUriFrom(Uri.of(limitCalculatorUrl))
        .then(JavaHttpClient())

val corsMiddleware = Cors(CorsPolicy.UnsafeGlobalPermissive)

fun carbonIntensityServer(
    port: Int,
    nationalGrid: NationalGrid,
    octopus: Octopus,
    limitCalculator: LimitCalculator,
    weightsCalculator: WeightsCalculator,
    openTelemetry: ManagedOpenTelemetry
) = carbonIntensity(nationalGrid, octopus, limitCalculator, weightsCalculator, openTelemetry).asServer(SunHttp(port))

fun carbonIntensity(
    nationalGrid: NationalGrid,
    octopus: Octopus,
    limitCalculator: LimitCalculator,
    weightsCalculator: WeightsCalculator,
    openTelemetry: ManagedOpenTelemetry
) = corsMiddleware
    .then(CatchLensFailure { _: LensFailure ->
        Response(BAD_REQUEST).with(errorResponseLens of InvalidRequestFailed.toErrorResponse())
    })
    .then(
        routes(
            contractRoutes(nationalGrid),
            intensityChargeTime(nationalGrid, weightsCalculator),
            octopusProducts(octopus),
            octopusPrices(octopus),
            octopusChargeTimes(Calculator(octopus, nationalGrid, limitCalculator, weightsCalculator, openTelemetry))
        )
    )

private fun contractRoutes(nationalGrid: NationalGrid) = contract {
    renderer = openApi3()
    descriptionPath = "/openapi.json"
    preFlightExtraction = None
    routes += intensities(nationalGrid)
}
