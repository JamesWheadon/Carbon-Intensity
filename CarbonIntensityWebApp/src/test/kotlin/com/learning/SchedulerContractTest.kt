package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.format.Jackson
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.*
import java.time.LocalDate

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
interface SchedulerContractTest {
    val httpClient: HttpHandler

    @Test
    @Order(1)
    fun `responds with no content when intensities updated`() {
        val schedulerInput = Scheduler(List(48) { 212 }, LocalDate.now())
        val intensitiesResponse = httpClient(
            Request(POST, "/intensities").with(schedulerLens of schedulerInput)
        )

        assertThat(intensitiesResponse.status, equalTo(NO_CONTENT))
    }

    @Test
    fun `responds with bad request when invalid intensities sent`() {
        val schedulerInput = Scheduler(List(10) { 212 }, LocalDate.now())
        val intensitiesResponse = httpClient(
            Request(POST, "/intensities").with(schedulerLens of schedulerInput)
        )

        assertThat(intensitiesResponse.status, equalTo(BAD_REQUEST))
        assertThat(
            errorResponseLens(intensitiesResponse).error,
            contains("too short".toRegex())
        )
    }

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
    fun `responds with the date intensities submitted for`() {
        val schedulerDateResponse = httpClient(Request(GET, "/intensities/date"))
        val schedulerDate = schedulerDateLens(schedulerDateResponse)

        assertThat(schedulerDateResponse.status, equalTo(OK))
        assertThat(schedulerDate.date, equalTo(LocalDate.now()))
    }
}

class FakeSchedulerTest : SchedulerContractTest {
    override val httpClient = FakeScheduler()

    @AfterEach
    fun tearDown() {
        httpClient.dataForScheduler = true
    }

    @Test
    fun `responds with not found when no data sent to the scheduler`() {
        httpClient.dataForScheduler = false

        val schedulerDateResponse = httpClient(Request(GET, "/intensities/date"))
        val schedulerDate = errorResponseLens(schedulerDateResponse)

        assertThat(schedulerDateResponse.status, equalTo(NOT_FOUND))
        assertThat(schedulerDate.error, equalTo("No data has been submitted to the scheduler"))
    }
}

@Disabled
class SchedulerTest : SchedulerContractTest {
    override val httpClient = SetHostFrom(Uri.of("http://localhost:8000")).then(JavaHttpClient())
}

class FakeScheduler : HttpHandler {
    var dataForScheduler = true

    val routes = routes(
        "/charge-time" bind GET to { request ->
            val current = Query.int().defaulted("current", 0)(request)
            if (current < 48) {
                Response(OK).with(chargeTimeLens of ChargeTime(15, null))
            } else {
                Response(NOT_FOUND).with(chargeTimeLens of ChargeTime(null, "no data for time slot"))
            }
        },
        "/intensities" bind POST to { request ->
            val requestBody = schedulerLens(request)
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
        },
        "/intensities/date" bind GET to {
            if (dataForScheduler) {
                Response(OK).with(schedulerDateLens of SchedulerDate(LocalDate.now(), null))
            } else {
                Response(NOT_FOUND).with(
                    errorResponseLens of ErrorResponse("No data has been submitted to the scheduler")
                )
            }
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class Scheduler(val intensities: List<Int>, val date: LocalDate)
data class ChargeTime(val chargeTime: Int?, val error: String?)
data class SchedulerDate(val date: LocalDate?, val error: String?)
data class ErrorResponse(val error: String)

val schedulerLens = Jackson.autoBody<Scheduler>().toLens()
val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()
val schedulerDateLens = Jackson.autoBody<SchedulerDate>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
