package com.intensity.core

import org.http4k.format.Jackson

data class ErrorResponse(val error: String)

val errorResponseLens = Jackson.autoBody<ErrorResponse>().toLens()
