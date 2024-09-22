package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNPROCESSABLE_ENTITY
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

    @Test
    fun `responds with no content when intensities updated`() {
        val schedulerInput = Scheduler(List(48) { TimeSlot(212) })
        val intensitiesResponse = httpClient(
            Request(POST, "/intensities").with(schedulerLens of schedulerInput)
        )

        assertThat(intensitiesResponse.status, equalTo(NO_CONTENT))
    }

    @Test
    fun `responds with unprocessable entity when invalid intensities sent`() {
        val schedulerInput = Scheduler(List(10) { TimeSlot(212) })
        val intensitiesResponse = httpClient(
            Request(POST, "/intensities").with(schedulerLens of schedulerInput)
        )

        assertThat(intensitiesResponse.status, equalTo(UNPROCESSABLE_ENTITY))
        assertThat(
            errorResponseLens(intensitiesResponse),
            equalTo(ErrorResponse("invalid intensities, should be an array of 48 time slots"))
        )
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
        },
        "/intensities" bind POST to { request ->
            val requestBody = schedulerLens(request)
            if (requestBody.data.size == 48) {
                Response(NO_CONTENT)
            } else {
                Response(UNPROCESSABLE_ENTITY).with(
                    errorResponseLens of ErrorResponse("invalid intensities, should be an array of 48 time slots")
                )
            }
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class ChargeTime(val chargeTime: Int?, val error: String?)
data class Scheduler(val data: List<TimeSlot>)
data class TimeSlot(val intensity: Int)
data class ErrorResponse(val error: String)

val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()
val schedulerLens = Jackson.autoBody<Scheduler>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
