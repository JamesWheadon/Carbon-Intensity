@file:Suppress("MayBeConstant")

package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Electricity
import com.intensity.core.Failed
import com.intensity.core.chargeTimeLens
import com.intensity.observability.Observability
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import java.time.ZonedDateTime

class WeightsCalculator(private val httpHandler: HttpHandler, private val observability: Observability) {
    companion object {
        val pathSegment = "weights"
    }

    fun chargeTime(
        electricity: Electricity,
        weights: Weights,
        time: Long,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): Result<ChargeTime, Failed> {
        val response = observability.outboundHttp("Weighted calculation", "Weights")
            .then(httpHandler)(
            Request(
                POST,
                "_://weights/calculate"
            ).with(
                ScheduleRequest.lens of ScheduleRequest(
                    time,
                    electricity,
                    start,
                    end,
                    priceWeight = weights.priceWeight,
                    intensityWeight = weights.intensityWeight
                )
            )
        )
        return if (response.status == OK) {
            Success(chargeTimeLens(response))
        } else {
            Failure(UnableToCalculateChargeTime)
        }
    }
}
