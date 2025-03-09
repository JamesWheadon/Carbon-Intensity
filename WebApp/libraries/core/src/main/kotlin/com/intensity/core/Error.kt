package com.intensity.core

import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.Jackson

interface Failed {
    fun toErrorResponse(): ErrorResponse
}

data class ErrorResponse(val error: String)

val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()

fun handleLensFailures() = CatchLensFailure { _ ->
    Response(Status.BAD_REQUEST).with(errorResponseLens of ErrorResponse("Invalid Request"))
}
