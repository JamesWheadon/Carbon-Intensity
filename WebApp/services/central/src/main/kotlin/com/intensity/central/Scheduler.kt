package com.intensity.central

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intensity.core.errorResponseLens
import com.intensity.core.formatWith
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.lens.BiDiMapping
import java.time.Instant

interface Scheduler {
    fun sendIntensities(intensities: Intensities): Result<Nothing?, String>
    fun trainDuration(duration: Int): Result<Nothing?, String>
    fun getBestChargeTime(chargeDetails: ChargeDetails): Result<ChargeTime, String>
    fun getIntensitiesData(): Result<SchedulerIntensitiesData, String>
    fun deleteData()
}

class PythonScheduler(val httpHandler: HttpHandler) : Scheduler {
    override fun sendIntensities(intensities: Intensities): Result<Nothing?, String> {
        val response = httpHandler(Request(Method.POST, "/intensities").with(intensitiesLens of intensities))
        return if (response.status == Status.NO_CONTENT) {
            Success(null)
        } else {
            Failure(errorResponseLens(response).error)
        }
    }

    override fun trainDuration(duration: Int): Result<Nothing?, String> {
        val response = httpHandler(Request(Method.PATCH, "/intensities/train?duration=$duration"))
        return if (response.status == Status.NO_CONTENT) {
            Success(null)
        } else {
            Failure(errorResponseLens(response).error)
        }
    }

    override fun getBestChargeTime(chargeDetails: ChargeDetails): Result<ChargeTime, String> {
        val timestamp = formatWith(schedulerPattern).format(chargeDetails.startTime)
        var query = "current=$timestamp"
        if (chargeDetails.endTime != null) {
            val endTimestamp = formatWith(schedulerPattern).format(chargeDetails.endTime)
            query += "&end=$endTimestamp"
        }
        if (chargeDetails.duration != null) {
            query += "&duration=${chargeDetails.duration}"
        }
        val response = httpHandler(Request(Method.GET, "/charge-time?$query"))
        return if (response.status == Status.OK) {
            Success(chargeTimeLens(response))
        } else {
            Failure(errorResponseLens(response).error)
        }
    }

    override fun getIntensitiesData(): Result<SchedulerIntensitiesData, String> {
        val response = httpHandler(Request(Method.GET, "/intensities"))
        return if (response.status.successful) {
            Success(schedulerIntensitiesDataLens(response))
        } else {
            Failure(errorResponseLens(response).error)
        }
    }

    override fun deleteData() {
        httpHandler(Request(Method.DELETE, "/intensities"))
    }
}

fun schedulerClient(schedulerUrl: String) = ClientFilters.SetHostFrom(Uri.of(schedulerUrl)).then(JavaHttpClient())

data class Intensities(val intensities: List<Int>, val date: Instant)
data class SchedulerIntensitiesData(val intensities: List<Int>, val date: Instant)
data class ChargeTime(val chargeTime: Instant)

val intensitiesLens = SchedulerJackson.autoBody<Intensities>().toLens()
val chargeTimeLens = SchedulerJackson.autoBody<ChargeTime>().toLens()
val schedulerIntensitiesDataLens = SchedulerJackson.autoBody<SchedulerIntensitiesData>().toLens()

const val schedulerPattern: String = "yyyy-MM-dd'T'HH:mm:ss"

object SchedulerJackson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .text(
            BiDiMapping(
                { timestamp -> Instant.from(formatWith(schedulerPattern).parse(timestamp)) },
                { instant -> formatWith(schedulerPattern).format(instant) }
            )
        )
        .done()
        .deactivateDefaultTyping()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
)
