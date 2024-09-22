package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Query
import org.http4k.lens.int
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
        assertThat(chargeTime.chargeTime!!, greaterThanOrEqualTo(14))
    }

    @Test
    fun `responds with not found error when queried with invalid time`() {
        val chargeTimeResponse = httpClient(Request(GET, "/charge-time?current=1000"))
        val chargeTime = chargeTimeLens(chargeTimeResponse)

        assertThat(chargeTimeResponse.status, equalTo(NOT_FOUND))
        assertThat(chargeTime, equalTo(ChargeTime(null, "no data for time slot")))
    }
}

class FakeSchedulerTest : SchedulerContractTest {
    override val httpClient = FakeScheduler()
}

class FakeScheduler : HttpHandler {
    val routes = routes(
        "/charge-time" bind GET to { request ->
            val current = Query.int().defaulted("current", 0)(request)
            if (current < 96) {
                Response(OK).with(chargeTimeLens of ChargeTime(15, null))
            } else {
                Response(NOT_FOUND).with(chargeTimeLens of ChargeTime(null, "no data for time slot"))
            }
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class ChargeTime(
    val chargeTime: Int?,
    val error: String?
)

val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()
