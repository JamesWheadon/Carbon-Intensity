package com.intensity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class EndToEndTest {
    private val client = JavaHttpClient()
    private val scheduler = FakeScheduler()
    private val nationalGrid = FakeNationalGrid()
    private val server = carbonIntensityServer(
        1000,
        PythonScheduler(
            scheduler
        ),
        NationalGridCloud(
            nationalGrid
        )
    )

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `responds with optimal charge time`() {
        scheduler.hasTrainedForDuration(30)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z"))

        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-10-01T02:30:00")))
    }

    @Test
    fun `responds with optimal charge time with end time`() {
        scheduler.hasTrainedForDuration(30)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z"))

        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00", "2024-09-30T23:30:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T23:00:00")))
    }

    @Test
    fun `responds with optimal charge time with end time and duration`() {
        scheduler.hasTrainedForDuration(60)
        scheduler.hasBestChargeTimeForStart(Instant.parse("2024-09-30T21:20:00Z") to Instant.parse("2024-10-01T02:30:00Z"))

        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-30T21:20:00", "2024-09-30T23:30:00", 60))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-30T22:30:00")))
    }

    @Test
    fun `calls national grid and updates intensities in scheduler when best charge time is not found`() {
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-02T10:30:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-02T11:00:00")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `calls national grid and updates intensities in scheduler when best charge time is not found with end time`() {
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-02T10:30:00", "2024-09-02T12:30:00"))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-02T11:00:00")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `calls national grid and updates intensities in scheduler when best charge time is not found with end time and duration`() {
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-02T10:30:00", "2024-09-02T12:30:00", 75))
        )

        assertThat(response.status, equalTo(OK))
        assertThat(response.body.toString(), equalTo(getChargeTimeResponse("2024-09-02T11:15:00")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `responds with not found and error if can't calculate best charge time`() {
        scheduler.canNotGetChargeTimeFor(Instant.parse("2024-10-02T10:31:00Z"))

        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-10-02T10:31:00"))
        )

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.body.toString(), equalTo(getErrorResponse("unable to find charge time")))
        assertThat(scheduler.data, isNotNull())
    }

    @Test
    fun `responds with bad request and error if end time before start`() {
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
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
        val response = client(
            Request(POST, "http://localhost:${server.port()}/charge-time")
                .body(getChargeTimeBody("2024-09-02T10:31:00", "2024-09-02T10:56:00", 30))
        )

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(
            response.body.toString(),
            equalTo(getErrorResponse("end time must be after start time by at least the charge duration, default 30"))
        )
    }

    private fun getChargeTimeBody(startTimestamp: String) = """{"startTime":"$startTimestamp"}"""
    private fun getChargeTimeBody(startTimestamp: String, endTimestamp: String) =
        """{"startTime":"$startTimestamp","endTime": "$endTimestamp"}"""

    private fun getChargeTimeBody(startTimestamp: String, endTimestamp: String, duration: Int) =
        """{"startTime":"$startTimestamp","endTime": "$endTimestamp","duration":$duration}"""

    private fun getChargeTimeResponse(chargeTimestamp: String) = """{"chargeTime":"$chargeTimestamp"}"""
    private fun getErrorResponse(message: String) = """{"error":"$message"}"""
}
