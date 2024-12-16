package com.intensity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.MULTIPART_FORM_DATA
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.events.EventFilters.AddServiceName
import org.http4k.events.EventFilters.AddZipkinTraces
import org.http4k.events.Events
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.events.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.ResetRequestTracing
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ServerFilters
import org.http4k.lens.contentType
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorResolver
import org.http4k.tracing.ActorType
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.junit.TracerBulletEvents
import org.http4k.tracing.persistence.FileSystem
import org.http4k.tracing.renderer.PumlSequenceDiagram
import org.http4k.tracing.tracer.HttpTracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val actor = ActorResolver {
    println(it)
    Actor(it.metadata["service"].toString(), ActorType.System)
}

private fun traceEvents(actorName: String) = AddZipkinTraces().then(AddServiceName(actorName))

private fun clientStack(events: Events) =
    ClientFilters.RequestTracing()
        .then(ReportHttpTransaction { events(Outgoing(it)) })

private fun serverStack(events: Events) =
    ServerFilters.RequestTracing()
        .then(ReportHttpTransaction { events(Incoming(it)) })

private class User(rawEvents: Events, rawHttp: HttpHandler) {
    private val events = traceEvents("User").then(rawEvents)

    private val http = ResetRequestTracing().then(clientStack(events)).then(rawHttp)

    fun call(request: Request) = http(request)
}

class EndToEndTest {
    @RegisterExtension
    val events = TracerBulletEvents(
        listOf(HttpTracer(actor)),
        listOf(PumlSequenceDiagram),
        TraceRenderPersistence.FileSystem(File("./sequences"))
    )

    private val scheduler = FakeScheduler()
    private val nationalGrid = FakeNationalGrid()
    private val appClientStack = clientStack(traceEvents("App").then(events))
    private val server = serverStack(traceEvents("App").then(events)).then(
        carbonIntensity(
            PythonScheduler(
                appClientStack.then(TracedHttpHandler(scheduler, serverStack(traceEvents("Scheduler").then(events))))
            ),
            NationalGridCloud(
                appClientStack.then(
                    TracedHttpHandler(
                        nationalGrid,
                        serverStack(traceEvents("National Grid").then(events))
                    )
                )
            )
        )
    )

    @Test
    fun `responds with optimal charge time`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.hasTrainedForDuration(30)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z"))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-10-01T02:30:00")))
    }

    @Test
    fun `responds with optimal charge time with end time`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.hasTrainedForDuration(30)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z"))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00", "2024-09-30T23:30:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T23:00:00")))
    }

    @Test
    fun `responds with optimal charge time with end time and duration`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.hasTrainedForDuration(60)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z"))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00", "2024-09-30T23:30:00", 60))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T22:30:00")))
    }

    @Test
    fun `calls scheduler to train for duration when best charge time is not found`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T21:50:00")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `calls scheduler to train for duration when best charge time is not found with end time`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00", "2024-09-30T22:00:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T21:30:00")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `calls scheduler to train for duration when best charge time is not found with end time and duration`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00", "2024-09-30T23:30:00", 60))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T22:20:00")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `only calls scheduler to train for duration when error is about untrained duration`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.hasTrainedForDuration(30)
        scheduler.canNotGetChargeTimeFor(Instant.parse("2024-09-30T21:20:00Z"))

        server(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00"))
        )

        assertThat(scheduler.trainedCalled, equalTo(0))
    }

    @Test
    fun `responds with not found and error if can't calculate best charge time`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.hasTrainedForDuration(30)
        scheduler.canNotGetChargeTimeFor(Instant.parse("2024-10-02T10:31:00Z"))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-10-02T10:31:00"))
        )

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.body.toString(), equalTo(getErrorResponse("unable to find charge time")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `responds with bad request and error if end time before start`() {
        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-02T10:31:00", "2024-09-02T10:30:00"))
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(
            response.body.toString(),
            equalTo(getErrorResponse("end time must be after start time by at least the charge duration, default 30"))
        )
    }

    @Test
    fun `responds with bad request and error if difference between start and end less than duration`() {
        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-09-02T10:31:00", "2024-09-02T10:56:00", 30))
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(
            response.body.toString(),
            equalTo(getErrorResponse("end time must be after start time by at least the charge duration, default 30"))
        )
    }

    @Test
    fun `responds with bad request and error when no start timestamp`() {
        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(""""endTime": "${"2024-09-02T10:56:00"}","duration":${30}}""")
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(
            response.bodyString(),
            equalTo(getErrorResponse("incorrect request body or headers"))
        )
    }

    @Test
    fun `responds with unsupported media type and error when incorrect content type`() {
        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .contentType(MULTIPART_FORM_DATA)
                .form(
                    "startTime" to "2024-09-02T10:31:00",
                    "endTime" to "2024-09-02T10:56:00"
                )
        )

        assertThat(response.status, equalTo(UNSUPPORTED_MEDIA_TYPE))
        assertThat(
            response.bodyString(),
            equalTo(getErrorResponse("invalid content type"))
        )
    }

    @Test
    fun `responds with optimal charge time starting the next day`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.hasTrainedForDuration(30)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-10-01T21:20:00Z") to Instant.parse("2024-10-02T02:30:00Z"))

        val response = User(events, server).call(
            Request(POST, "/charge-time")
                .body(getChargeTimeBody("2024-10-01T21:20:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-10-02T02:30:00")))
    }

    @Test
    fun `returns intensity data from the scheduler`() {
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, date.toInstant()))

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(212)},"date":"${
                    date.format(
                        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
                    )
                }"}"""
            )
        )
    }

    @Test
    fun `calls national grid and updates scheduler if no data present in scheduler then returns intensities`() {
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        nationalGrid.setDateData(date.toInstant(), List(97) { 210 }, List(97) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(210)},"date":"${
                    date.format(
                        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
                    )
                }"}"""
            )
        )
    }

    @Test
    fun `calls national grid and updates scheduler if scheduler is out of date`() {
        scheduler.hasIntensityData(Intensities(List(96) { 212 }, getTestInstant()))
        val date = LocalDate.now(ZoneId.of("Europe/London")).atStartOfDay(ZoneId.of("Europe/London"))
        nationalGrid.setDateData(date.toInstant(), List(97) { 210 }, List(97) { null })

        val response = User(events, server).call(
            Request(POST, "/intensities")
        )

        assertThat(response.status, equalTo(OK))
        assertThat(
            response.body.toString(),
            equalTo(
                """{"intensities":${intensityList(210)},"date":"${
                    date.format(
                        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
                    )
                }"}"""
            )
        )
    }

    private fun getChargeTimeBody(startTimestamp: String) = """{"startTime":"$startTimestamp"}"""
    private fun getChargeTimeBody(startTimestamp: String, endTimestamp: String) =
        """{"startTime":"$startTimestamp","endTime": "$endTimestamp"}"""

    private fun getChargeTimeBody(startTimestamp: String, endTimestamp: String, duration: Int) =
        """{"startTime":"$startTimestamp","endTime": "$endTimestamp","duration":$duration}"""

    private fun getChargeTimeResponse(chargeTimestamp: String) = """{"chargeTime":"$chargeTimestamp"}"""
    private fun getErrorResponse(message: String) = """{"error":"$message"}"""

    private fun intensityList(intensity: Int) =
        List(96) { intensity }.joinToString(prefix = "[", separator = ",", postfix = "]")
}
