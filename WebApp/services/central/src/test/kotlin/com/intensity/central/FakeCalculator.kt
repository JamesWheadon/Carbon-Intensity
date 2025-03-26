package com.intensity.central

import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeLimitCalculator : HttpHandler {
    private var chargeTimeToRespondWith: Pair<String, String>? = null

    val routes = routes(
        "/calculate/price/{limit}" bind POST to { _ ->
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
