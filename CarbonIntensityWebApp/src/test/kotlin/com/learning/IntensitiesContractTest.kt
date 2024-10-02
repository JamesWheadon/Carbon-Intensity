package com.learning

import com.learning.Matchers.inTimeRange
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
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
        val errorResponse = scheduler.sendIntensities(Intensities(List(48) { 212 }, getTestInstant()))

        assertThat(errorResponse, equalTo(null))
    }

    @Test
    fun `responds with bad request when too few intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(47) { 212 }, getTestInstant()))

        assertThat(errorResponse!!.error, contains("too short".toRegex()))
    }

    @Test
    fun `responds with bad request when too many intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(49) { 212 }, getTestInstant()))

        assertThat(errorResponse!!.error, contains("too long".toRegex()))
    }

    @Test
    fun `responds with best time to charge when queried with current time`() {
        val chargeTime = scheduler.getBestChargeTime(getTestInstant().plusSeconds(60))

        assertThat(chargeTime.chargeTime!!, inTimeRange(getTestInstant(), getTestInstant().plusSeconds(SECONDS_IN_DAY)))
        assertThat(chargeTime.error, equalTo(null))
    }

    @Test
    fun `responds with not found error when queried with too early time`() {
        val chargeTime = scheduler.getBestChargeTime(getTestInstant().minusSeconds(60))

        assertThat(chargeTime.chargeTime, equalTo(null))
        assertThat(chargeTime.error!!, equalTo("No data for time slot"))
    }

    @Test
    fun `responds with not found error when queried with too late time`() {
        val chargeTime = scheduler.getBestChargeTime(getTestInstant().plusSeconds(3 * SECONDS_IN_DAY))

        assertThat(chargeTime.chargeTime, equalTo(null))
        assertThat(chargeTime.error!!, equalTo("No data for time slot"))
    }

    @Test
    fun `responds with best time in range of current time and end time`() {
        val chargeTime = scheduler.getBestChargeTime(getTestInstant().plusSeconds(60), getTestInstant().plusSeconds(3000))

        assertThat(chargeTime.chargeTime!!, inTimeRange(getTestInstant(), getTestInstant().plusSeconds(3000)))
        assertThat(chargeTime.error, equalTo(null))
    }
}

fun getTestInstant(): Instant = Instant.ofEpochSecond(1727727697L)

class FakeSchedulerTest : IntensitiesContractTest {
    override val scheduler =
        PythonScheduler(FakeScheduler(mapOf(getTestInstant().plusSeconds(60) to getTestInstant().plusSeconds(3600))) {})
}

@Disabled
class IntensitiesTest : IntensitiesContractTest {
    override val scheduler = PythonScheduler(schedulerClient())
}

class FakeScheduler(validChargeTimes: Map<Instant, Instant>, intensitiesUpdated: () -> Unit) : HttpHandler {
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
            val response = validChargeTimes[current]?.let { chargeTime -> getChargeTime(chargeTime, end) } ?: ChargeTime(null, "No data for time slot")
            if (response.error == null) {
                Response(OK).with(chargeTimeLens of response)
            } else {
                Response(NOT_FOUND).with(chargeTimeLens of response)
            }
        },
        "/intensities" bind POST to { request ->
            val requestBody = intensitiesLens(request)
            if (requestBody.intensities.size == 48) {
                intensitiesUpdated()
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

    private fun getChargeTime(chargeTime: Instant, end: Instant?): ChargeTime {
        var actionTime = chargeTime
        if (end != null) {
            while (end < actionTime) {
                actionTime = actionTime.minusSeconds(1800)
            }
        }
        return ChargeTime(actionTime, null)
    }

    override fun invoke(request: Request): Response = routes(request)
}
