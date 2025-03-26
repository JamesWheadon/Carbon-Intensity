package com.intensity.central

import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class FakeLimitCalculator : HttpHandler {
    private var chargeTimeToRespondWith: MutableMap<FakeLimits, Pair<String, String>> = mutableMapOf()

    val routes = routes(
        "/calculate/price/{limit}" bind POST to { request ->
            getResponse(FakeLimits(request.path("limit")!!.toDouble(), 0.0))
        },
        "/calculate/intensity/{limit}" bind POST to { request ->
            getResponse(FakeLimits(0.0, request.path("limit")!!.toDouble()))
        }
    )

    private fun getResponse(limits: FakeLimits) =
        chargeTimeToRespondWith[limits]
            ?.let { Response(OK).body("""{"from":"${it.first}","to":"${it.second}"}""") }
            ?: Response(NOT_FOUND)

    override fun invoke(request: Request) = routes(request)

    fun setIntensityChargeTime(limit: Double, chargeTime: Pair<String, String>) {
        chargeTimeToRespondWith[FakeLimits(0.0, limit)] = chargeTime
    }
}

class FakeWeightsCalculator : HttpHandler {
    private var chargeTimeToRespondWith: MutableMap<FakeWeights, Pair<String, String>> = mutableMapOf()

    val routes = routes(
        "/calculate" bind POST to { request ->
            getResponse(FakeWeights.lens(request))
        }
    )

    private fun getResponse(weights: FakeWeights) =
        chargeTimeToRespondWith[weights]
            ?.let { Response(OK).body("""{"from":"${it.first}","to":"${it.second}"}""") }
            ?: Response(NOT_FOUND)

    override fun invoke(request: Request) = routes(request)

    fun setChargeTime(weights: FakeWeights, chargeTime: Pair<String, String>) {
        chargeTimeToRespondWith[weights] = chargeTime
    }
}

data class FakeLimits(val price: Double, val intensity: Double)
data class FakeWeights(val priceWeight: Double, val intensityWeight: Double) {
    companion object {
        val lens = Jackson.autoBody<FakeWeights>().toLens()
    }
}
