package com.learning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.learning.Matchers.inLocalDateTimeRange
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.Query
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.dateTime
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
interface SchedulerContractTest {
    val httpClient: HttpHandler

    @Test
    @Order(1)
    fun `responds with no content when intensities updated`() {
        val schedulerInput = Scheduler(List(48) { 212 }, LocalDateTime.now())
        val with = Request(POST, "/intensities").with(schedulerLens of schedulerInput)
        println(with.body.toString())
        val intensitiesResponse = httpClient(
            with
        )

        assertThat(intensitiesResponse.status, equalTo(NO_CONTENT))
    }

    @Test
    fun `responds with bad request when too few intensities sent`() {
        val schedulerInput = Scheduler(List(47) { 212 }, LocalDateTime.now())
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
    fun `responds with bad request when too many intensities sent`() {
        val schedulerInput = Scheduler(List(49) { 212 }, LocalDateTime.now())
        val intensitiesResponse = httpClient(
            Request(POST, "/intensities").with(schedulerLens of schedulerInput)
        )

        assertThat(intensitiesResponse.status, equalTo(BAD_REQUEST))
        assertThat(
            errorResponseLens(intensitiesResponse).error,
            contains("too long".toRegex())
        )
    }

    @Test
    fun `responds with best time to charge when queried with current time`() {
        val timestamp = LocalDateTime.now().plusSeconds(60).format(formatter)
        val chargeTimeResponse = httpClient(Request(GET, "/charge-time?current=$timestamp"))
        val chargeTime = chargeTimeLens(chargeTimeResponse)

        assertThat(chargeTimeResponse.status, equalTo(OK))
        assertThat(chargeTime.chargeTime!!, inLocalDateTimeRange(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))
    }

    @Test
    fun `responds with not found error when queried with too early time`() {
        val timestamp = LocalDateTime.now().minusSeconds(60).format(formatter)
        val chargeTimeResponse = httpClient(Request(GET, "/charge-time?current=$timestamp"))
        val chargeTime = chargeTimeLens(chargeTimeResponse)

        assertThat(chargeTimeResponse.status, equalTo(NOT_FOUND))
        assertThat(chargeTime, equalTo(ChargeTime(null, "No data for time slot")))
    }

    @Test
    fun `responds with not found error when queried with too late time`() {
        val timestamp = LocalDateTime.now().plusDays(3).format(formatter)
        val chargeTimeResponse = httpClient(Request(GET, "/charge-time?current=$timestamp"))
        val chargeTime = chargeTimeLens(chargeTimeResponse)

        assertThat(chargeTimeResponse.status, equalTo(NOT_FOUND))
        assertThat(chargeTime, equalTo(ChargeTime(null, "No data for time slot")))
    }

    @Test
    fun `responds with the date intensities submitted for`() {
        val schedulerDateResponse = httpClient(Request(GET, "/intensities/date"))
        val schedulerDate = schedulerDateLens(schedulerDateResponse)

        assertThat(schedulerDateResponse.status, equalTo(OK))
        assertThat(
            schedulerDate.date!!,
            inLocalDateTimeRange(LocalDateTime.now().minusSeconds(60), LocalDateTime.now())
        )
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

class SchedulerTest : SchedulerContractTest {
    override val httpClient = SetHostFrom(Uri.of("http://localhost:8000")).then(JavaHttpClient())
}

class FakeScheduler : HttpHandler {
    var dataForScheduler = true

    val routes = routes(
        "/charge-time" bind GET to { request ->
            val current = Query.dateTime(formatter = DateTimeFormatter.ISO_DATE_TIME).defaulted("current", LocalDateTime.now())(request)
            if (current >= LocalDateTime.now() && current < LocalDateTime.now().plusDays(2)) {
                Response(OK).with(chargeTimeLens of ChargeTime(LocalDateTime.now(), null))
            } else {
                Response(NOT_FOUND).with(chargeTimeLens of ChargeTime(null, "No data for time slot"))
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
                Response(OK).with(schedulerDateLens of SchedulerDate(LocalDateTime.now(), null))
            } else {
                Response(NOT_FOUND).with(
                    errorResponseLens of ErrorResponse("No data has been submitted to the scheduler")
                )
            }
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class Scheduler(val intensities: List<Int>, val date: LocalDateTime)
data class ChargeTime(val chargeTime: LocalDateTime?, val error: String?)
data class SchedulerDate(val date: LocalDateTime?, val error: String?)
data class ErrorResponse(val error: String)

val schedulerLens = MyJackson.autoBody<Scheduler>().toLens()
val chargeTimeLens = Jackson.autoBody<ChargeTime>().toLens()
val schedulerDateLens = MyJackson.autoBody<SchedulerDate>().toLens()
val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()

object MyJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(StringBiDiMappings.localDateTime(DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss")))
        .done()
        .deactivateDefaultTyping()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)
