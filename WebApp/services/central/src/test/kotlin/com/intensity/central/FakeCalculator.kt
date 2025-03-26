package com.intensity.central

import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class FakeLimitCalculator : HttpHandler {
    private var chargeTimeToRespondWith: MutableMap<Limits, Pair<String, String>> = mutableMapOf()

    val routes = routes(
        "/calculate/price/{limit}" bind POST to { request ->
            val limits = Limits(request.path("limit")!!.toDouble(), 0.0)
            getResponse(limits)
        },
        "/calculate/intensity/{limit}" bind POST to { request ->
            val limits = Limits(0.0, request.path("limit")!!.toDouble())
            getResponse(limits)
        }
    )

    private fun getResponse(limits: Limits) = (chargeTimeToRespondWith[limits]
        ?.let { Response(OK).body("""{"from":"${it.first}","to":"${it.second}"}""") }
        ?: Response(NOT_FOUND))

    override fun invoke(request: Request) = routes(request)

    fun setIntensityChargeTime(limit: Double, chargeTime: Pair<String, String>) {
        chargeTimeToRespondWith[Limits(0.0, limit)] = chargeTime
    }
}

class FakeWeightsCalculator : HttpHandler {
    private var chargeTimeToRespondWith: Pair<String, String>? = null

    val routes = routes(
        "/calculate" bind POST to { _ ->
            if (chargeTimeToRespondWith == null) {
                Response(NOT_FOUND)
            } else {
                Response(OK).body("""{"from":"${chargeTimeToRespondWith!!.first}","to":"${chargeTimeToRespondWith!!.second}"}""")
            }
        }
    )

    override fun invoke(request: Request) = routes(request)

    fun setChargeTime(chargeTime: Pair<String, String>) {
        chargeTimeToRespondWith = chargeTime
    }
}

data class Limits(val price: Double, val intensity: Double)
