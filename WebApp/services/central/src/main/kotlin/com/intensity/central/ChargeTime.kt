package com.intensity.central

import com.intensity.core.ChargeTime
import com.intensity.core.Failed
import com.intensity.core.chargeTimeLens
import com.intensity.core.errorResponseLens
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.fold
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

fun Result<ChargeTime, Failed>.toChargeTimeResponse() =
    fold(
        { chargeTime ->
            Response(Status.OK).with(chargeTimeLens of chargeTime)
        },
        { failed ->
            val status = when (failed) {
                UnableToCalculateChargeTime -> Status.NOT_FOUND
                else -> Status.INTERNAL_SERVER_ERROR
            }
            Response(status).with(errorResponseLens of failed.toErrorResponse())
        }
    )
