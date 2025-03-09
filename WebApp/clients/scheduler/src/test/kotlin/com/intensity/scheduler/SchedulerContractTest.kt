package com.intensity.scheduler

import com.intensity.coretest.inTimeRange
import com.intensity.coretest.isFailure
import com.intensity.coretest.isSuccess
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.failureOrNull
import dev.forkhandles.result4k.valueOrNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant

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

        assertThat(errorResponse, isFailure(SchedulerUpdateFailed))
    }

    @Test
    fun `responds with bad request when too many intensities sent`() {
        val errorResponse = scheduler.sendIntensities(Intensities(List(97) { 212 }, getTestInstant()))

        assertThat(errorResponse, isFailure(SchedulerUpdateFailed))
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

@Disabled
class SchedulerTest : SchedulerContractTest() {
    override val scheduler = PythonScheduler(schedulerClient("http://localhost:8000"))
}
