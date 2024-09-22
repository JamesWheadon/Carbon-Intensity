package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

interface SchedulerContractTest {
    val httpClient: HttpHandler

    @Test
    fun `responds with best time to charge when queried with current time`() {
        val chargeTimeResponse = httpClient(Request(GET, "/charge-time?current=14"))
        val chargeTime = chargeTimeLens(chargeTimeResponse)

        assertThat(chargeTimeResponse.status, equalTo(OK))
        assertThat(chargeTime.chargeTime, greaterThanOrEqualTo(14))
    }
}

class FakeSchedulerTest : SchedulerContractTest {
    override val httpClient = FakeScheduler()
}

class FakeScheduler : HttpHandler {
    val routes = routes(
        "/charge-time" bind GET to {
            Response(OK).with(chargeTimeLens of ChargeTime(15))
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class ChargeTime(
    val chargeTime: Int
)

val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()
