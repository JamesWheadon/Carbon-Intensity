package com.intensity.core

import org.http4k.format.Jackson

interface Failed {
    fun toErrorResponse(): ErrorResponse
}

data class ErrorResponse(val error: String)

val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
