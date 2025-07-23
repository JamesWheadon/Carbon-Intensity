package com.intensity.central

import com.intensity.core.errorResponseLens
import com.intensity.nationalgrid.NationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.nationalgrid.nationalGridClient
import com.intensity.observability.Observability
import com.intensity.octopus.InvalidRequestFailed
import com.intensity.octopus.OctopusCloud
import com.intensity.octopus.octopusClient
import com.intensity.openapi.openApi3
import org.http4k.client.JavaHttpClient
import org.http4k.contract.PreFlightExtraction.Companion.None
import org.http4k.contract.contract
import org.http4k.core.HttpHandler
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
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.reverseProxyRouting
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    val limitCalculatorUrl = "http://localhost:9001"
    val weightsCalculatorUrl = "http://localhost:9002"
    val observability = Observability.noOp()
    val network = reverseProxyRouting(
        "grid" to nationalGridClient(),
        "octopus" to octopusClient(),
        "limit" to calculatorClient(limitCalculatorUrl),
        "weights" to calculatorClient(weightsCalculatorUrl)
    )
    carbonIntensity(
        network,
        observability
    ).asServer(SunHttp(port)).start()
}

fun calculatorClient(limitCalculatorUrl: String) =
    ClientFilters.SetBaseUriFrom(Uri.of(limitCalculatorUrl))
        .then(JavaHttpClient())

val corsMiddleware = Cors(CorsPolicy.UnsafeGlobalPermissive)

fun carbonIntensity(
    network: HttpHandler,
    observability: Observability
): RoutingHttpHandler {
    val nationalGrid = NationalGridCloud(network, observability)
    val octopus = OctopusCloud(network, observability)
    val limitCalculator = LimitCalculatorCloud(network, observability)
    val weightsCalculator = WeightsCalculatorCloud(network, observability)
    return corsMiddleware
        .then(observability.inboundHttp())
        .then(CatchLensFailure { _: LensFailure ->
            Response(BAD_REQUEST).with(errorResponseLens of InvalidRequestFailed.toErrorResponse())
        })
        .then(
            routes(
                contractRoutes(nationalGrid),
                intensityChargeTime(nationalGrid, weightsCalculator),
                octopusProducts(octopus),
                octopusPrices(octopus),
                octopusChargeTimes(Calculator(octopus, nationalGrid, limitCalculator, weightsCalculator, observability))
            )
        )
}

private fun contractRoutes(nationalGrid: NationalGrid) = contract {
    renderer = openApi3()
    descriptionPath = "/openapi.json"
    preFlightExtraction = None
    routes += intensities(nationalGrid)
}
