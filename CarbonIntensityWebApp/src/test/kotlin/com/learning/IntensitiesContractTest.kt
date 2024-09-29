package com.learning

import com.learning.Matchers.inTimeRange
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.format.Jackson
import org.http4k.lens.BiDiMapping
import org.http4k.lens.Query
import org.http4k.lens.map
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.Instant

private const val SECONDS_IN_DAY = 86400L

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
interface IntensitiesContractTest {
    val scheduler: Scheduler

    @Test
    @Order(1)
    fun `responds with no content when intensities updated`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(48) { 212 }, Instant.now()))

        assertThat(errorResponse, equalTo(null))
    }

    @Test
    fun `responds with bad request when too few intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(47) { 212 }, Instant.now()))

        assertThat(errorResponse!!.error, contains("too short".toRegex()))
    }

    @Test
    fun `responds with bad request when too many intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(49) { 212 }, Instant.now()))

        assertThat(errorResponse!!.error, contains("too long".toRegex()))
    }

    @Test
    fun `responds with best time to charge when queried with current time`() {
        val chargeTime = scheduler.getBestChargeTime(Instant.now().plusSeconds(60))

        assertThat(chargeTime.chargeTime!!, inTimeRange(Instant.now(), Instant.now().plusSeconds(SECONDS_IN_DAY)))
        assertThat(chargeTime.error, equalTo(null))
    }

    @Test
    fun `responds with not found error when queried with too early time`() {
        val chargeTime = scheduler.getBestChargeTime(Instant.now().minusSeconds(60))

        assertThat(chargeTime.chargeTime, equalTo(null))
        assertThat(chargeTime.error!!, equalTo("No data for time slot"))
    }

    @Test
    fun `responds with not found error when queried with too late time`() {
        val chargeTime = scheduler.getBestChargeTime(Instant.now().plusSeconds(3 * SECONDS_IN_DAY))

        assertThat(chargeTime.chargeTime, equalTo(null))
        assertThat(chargeTime.error!!, equalTo("No data for time slot"))
    }
}

interface Scheduler {
    fun sendIntensities(intensities: Intensities): ErrorResponse?

    fun getBestChargeTime(chargeTime: Instant): ChargeTime
}

class PythonScheduler(val httpHandler: HttpHandler) : Scheduler {
    override fun sendIntensities(intensities: Intensities): ErrorResponse? {
        val response = httpHandler(Request(POST, "/intensities").with(intensitiesLens of intensities))
        return if (response.status == NO_CONTENT) {
            null
        } else {
            errorResponseLens(response)
        }
    }

    override fun getBestChargeTime(chargeTime: Instant): ChargeTime {
        val timestamp = formatWith(schedulerPattern).format(chargeTime)
        return chargeTimeLens(httpHandler(Request(GET, "/charge-time?current=$timestamp")))
    }

}

class FakeSchedulerTest : IntensitiesContractTest {
    override val scheduler = PythonScheduler(FakeScheduler())
}

@Disabled
class IntensitiesTest : IntensitiesContractTest {
    override val scheduler = PythonScheduler(schedulerClient())
}

fun schedulerClient() = SetHostFrom(Uri.of("http://localhost:8000")).then(JavaHttpClient())

class FakeScheduler : HttpHandler {
    val routes = routes(
        "/charge-time" bind GET to { request ->
            val current = Query.map(
                BiDiMapping(
                    { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                    { instant -> formatWith(schedulerPattern).format(instant) }
                )
            ).defaulted("current", null)(request)
            if (current != null && current >= Instant.now() && current < Instant.now().plusSeconds(2 * SECONDS_IN_DAY)
            ) {
                Response(OK).with(chargeTimeLens of ChargeTime(Instant.now(), null))
            } else {
                Response(NOT_FOUND).with(chargeTimeLens of ChargeTime(null, "No data for time slot"))
            }
        },
        "/intensities" bind POST to { request ->
            val requestBody = intensitiesLens(request)
            if (requestBody.intensities.size == 48) {
                Response(NO_CONTENT)
            } else {
                val errorMessage = if (requestBody.intensities.size > 48) {
                    "${requestBody.intensities} is too long"
                } else {
                    "${requestBody.intensities} is too short"
                }
                Response(BAD_REQUEST).with(
                    errorResponseLens of ErrorResponse(errorMessage)
                )
            }
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class Intensities(val intensities: List<Int>, val date: Instant)
data class ChargeTime(val chargeTime: Instant?, val error: String?)
data class ErrorResponse(val error: String)

val intensitiesLens = SchedulerJackson.autoBody<Intensities>().toLens()
val chargeTimeLens = SchedulerJackson.autoBody<ChargeTime>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
