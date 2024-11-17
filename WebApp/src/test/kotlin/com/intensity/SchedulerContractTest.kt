package com.intensity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.failureOrNull
import dev.forkhandles.result4k.valueOrNull
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.BiDiMapping
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.map
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Instant

private const val SECONDS_IN_DAY = 86400L

abstract class SchedulerContractTest {
    abstract val scheduler: Scheduler

    @Test
    fun `responds with no content when intensities updated`() {
        val response = scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))

        assertThat(response, isSuccess())
    }

    @Test
    fun `responds with bad request when too few intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(95) { 212 }, getTestInstant()))

        assertThat(errorResponse.failureOrNull()!!, contains("too short".toRegex()))
    }

    @Test
    fun `responds with bad request when too many intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(97) { 212 }, getTestInstant()))

        assertThat(errorResponse.failureOrNull()!!, contains("too long".toRegex()))
    }

    @Test
    fun `responds with no content when duration trained`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))

        val response = scheduler.trainDuration(30)

        assertThat(response, isSuccess())
    }

    @Test
    fun `responds with error when attempt to train with no data`() {
        scheduler.deleteData()

        val response = scheduler.trainDuration(30)

        assertThat(response.failureOrNull()!!, equalTo("No intensity data for scheduler"))
    }

    @Test
    fun `responds with best time to charge when queried with current time`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.trainDuration(30)

        val chargeTime = scheduler.getBestChargeTime(ChargeDetails(getTestInstant().plusSeconds(60), null, null))

        assertThat(
            chargeTime.valueOrNull()!!.chargeTime,
            inTimeRange(getTestInstant(), getTestInstant().plusSeconds(2 * SECONDS_IN_DAY))
        )
    }

    @Test
    fun `responds with not found error when queried with too early time`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.trainDuration(30)

        val chargeTime = scheduler.getBestChargeTime(ChargeDetails(getTestInstant().minusSeconds(30 * 60), null, null))

        assertThat(chargeTime.failureOrNull()!!, equalTo("No data for time slot"))
    }

    @Test
    fun `responds with not found error when queried with too late time`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.trainDuration(30)

        val chargeTime =
            scheduler.getBestChargeTime(ChargeDetails(getTestInstant().plusSeconds(3 * SECONDS_IN_DAY), null, null))

        assertThat(chargeTime.failureOrNull()!!, equalTo("No data for time slot"))
    }

    @Test
    fun `responds with charge time when queried with time a day old`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.trainDuration(30)

        val chargeTime =
            scheduler.getBestChargeTime(ChargeDetails(getTestInstant().plusSeconds(1 * SECONDS_IN_DAY), null, null))

        assertThat(
            chargeTime.valueOrNull()!!.chargeTime,
            inTimeRange(getTestInstant().plusSeconds(SECONDS_IN_DAY), getTestInstant().plusSeconds(2 * SECONDS_IN_DAY))
        )
    }

    @Test
    fun `responds with not found error when model not trained for duration`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))

        val chargeTime = scheduler.getBestChargeTime(ChargeDetails(getTestInstant().plusSeconds(60), null, null))

        assertThat(chargeTime.failureOrNull()!!, equalTo("Duration has not been trained"))
    }

    @Test
    fun `responds with best time in range of current time and end time`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.trainDuration(30)

        val chargeTime = scheduler.getBestChargeTime(
            ChargeDetails(
                getTestInstant().plusSeconds(60),
                getTestInstant().plusSeconds(3000),
                null
            )
        )

        assertThat(
            chargeTime.valueOrNull()!!.chargeTime,
            inTimeRange(getTestInstant(), getTestInstant().plusSeconds(3000))
        )
    }

    @Test
    fun `responds with best time to charge when queried with current time and duration`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))
        scheduler.trainDuration(75)

        val chargeTime = scheduler.getBestChargeTime(
            ChargeDetails(
                getTestInstant().plusSeconds(60),
                getTestInstant().plusSeconds(6000),
                75
            )
        )

        assertThat(
            chargeTime.valueOrNull()!!.chargeTime,
            inTimeRange(getTestInstant(), getTestInstant().plusSeconds(1500))
        )
    }

    @Test
    fun `responds with scheduler intensities data`() {
        scheduler.sendIntensities(Intensities(List(96) { 212 }, getTestInstant()))

        val intensitiesData = scheduler.getIntensitiesData()

        assertThat(intensitiesData, isSuccess())
        assertThat(intensitiesData.valueOrNull()!!.intensities, equalTo(List(96) { 212 }))
        assertThat(intensitiesData.valueOrNull()!!.date, equalTo(getTestInstant()))
    }

    @Test
    fun `responds with error when no intensities data in scheduler`() {
        scheduler.deleteData()

        val response = scheduler.getIntensitiesData()

        assertThat(response.failureOrNull()!!, equalTo("No intensity data for scheduler"))
    }
}

fun getTestInstant(): Instant = Instant.ofEpochSecond(1727727696L)

class FakeSchedulerTest : SchedulerContractTest() {
    override val scheduler =
        PythonScheduler(
            FakeScheduler()
        )
}

//@Disabled
class SchedulerTest : SchedulerContractTest() {
    override val scheduler = PythonScheduler(schedulerClient())
}

class FakeScheduler : HttpHandler {
    private val chargeTimes = mutableMapOf<Instant, Instant>()
    private val errorChargeTime = mutableListOf<Instant>()
    private val trainedDurations = mutableSetOf<Int>()
    var data: Intensities? = null
    var trainedCalled = 0
    val routes = routes(
        "/charge-time" bind GET to { request ->
            val current = Query.map(
                BiDiMapping(
                    { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                    { instant -> formatWith(schedulerPattern).format(instant) }
                )
            ).defaulted("current", null)(request)
            val end = Query.map(
                BiDiMapping(
                    { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                    { instant -> formatWith(schedulerPattern).format(instant) }
                )
            ).defaulted("end", null)(request)
            val duration = Query.int().defaulted("duration", 30)(request)
            if (end != null && end.isBefore(current.plusSeconds(duration * 60L))) {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("End must be after current plus duration"))
            } else if (!trainedDurations.contains(duration)) {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("Duration has not been trained"))
            } else if (
                current.isBefore(data!!.date)
                || current.isAfter(data!!.date.plusSeconds(2 * SECONDS_IN_DAY))
                || errorChargeTime.contains(current)
            ) {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("No data for time slot"))
            } else {
                val response = chargeTimes[current]?.let { chargeTime -> getChargeTime(chargeTime, end, duration) }
                    ?: getChargeTime(current.plusSeconds(duration * 60L), end, duration)
                Response(OK).with(chargeTimeLens of response)
            }
        },
        "/intensities" bind POST to { request ->
            trainedDurations.clear()
            val requestBody = intensitiesLens(request)
            if (requestBody.intensities.size == 96) {
                data = requestBody
                Response(NO_CONTENT)
            } else {
                val errorMessage = if (requestBody.intensities.size > 96) {
                    "${requestBody.intensities} is too long"
                } else {
                    "${requestBody.intensities} is too short"
                }
                Response(BAD_REQUEST).with(
                    errorResponseLens of ErrorResponse(errorMessage)
                )
            }
        },
        "/intensities" bind GET to {
            if (data != null) {
                Response(OK).with(intensitiesLens of data!!)
            } else {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("No intensity data for scheduler"))
            }
        },
        "/intensities/train" bind PATCH to { request ->
            trainedCalled++
            if (data != null) {
                trainedDurations.add(Query.int().required("duration")(request))
                Response(NO_CONTENT)
            } else {
                Response(NOT_FOUND).with(errorResponseLens of ErrorResponse("No intensity data for scheduler"))
            }
        }
    )

    fun hasTrainedForDuration(duration: Int) {
        trainedDurations.add(duration)
    }

    fun hasBestChargeTimeForStart(startTimeToBestTime: Pair<Instant, Instant>) {
        chargeTimes[startTimeToBestTime.first] = startTimeToBestTime.second
    }

    fun canNotGetChargeTimeFor(badTime: Instant) {
        errorChargeTime.add(badTime)
    }

    private fun getChargeTime(chargeTime: Instant, end: Instant?, duration: Int): ChargeTime {
        val actionTime = if (end != null && end.minusSeconds(duration * 60L) < chargeTime) {
            end.minusSeconds(duration * 60L)
        } else {
            chargeTime
        }
        return ChargeTime(actionTime)
    }

    fun hasIntensityData(intensities: Intensities) {
        data = intensities
    }

    override fun invoke(request: Request): Response = routes(request)
}
