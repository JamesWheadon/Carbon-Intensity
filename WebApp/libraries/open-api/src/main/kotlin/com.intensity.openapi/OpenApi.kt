package com.intensity.openapi

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.intensity.core.ErrorResponse
import com.intensity.core.errorResponseLens
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.contract.jsonschema.v3.FieldHolder
import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.jsonschema.v3.FieldRetrieval
import org.http4k.contract.jsonschema.v3.SimpleLookup
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.contract.openapi.OpenApiVersion
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.with
import org.http4k.lens.LensFailure

fun openApi3() = OpenApi3(
    apiInfo = ApiInfo("Carbon Intensity Calculator", "v1.0"),
    json = OpenAPIJackson,
    extensions = emptyList(),
    apiRenderer = ApiRenderer.Auto(OpenAPIJackson,
        AutoJsonToJsonSchema(OpenAPIJackson, FieldRetrieval.compose(
            SimpleLookup(
                metadataRetrievalStrategy = { a, b ->
                    when (a) {
                        is ContractSchema -> a.schemas()[b] ?: FieldMetadata()
                        is FieldHolder -> FieldMetadata("format" to "int32")
                        else -> FieldMetadata()
                    }
                }
            )
        ))
    ),
    errorResponseRenderer = object : ErrorResponseRenderer {
        override fun badRequest(lensFailure: LensFailure) = failedToParseRequest(lensFailure)
    },
    servers = emptyList(),
    version = OpenApiVersion._3_0_0
)

private fun failedToParseRequest(failure: LensFailure): Response {
    return when (failure.cause) {
        is MismatchedInputException -> Response(BAD_REQUEST).with(errorResponseLens of ErrorResponse("incorrect request body or headers"))
        else -> Response(UNSUPPORTED_MEDIA_TYPE).with(errorResponseLens of ErrorResponse("invalid content type"))
    }
}

interface ContractSchema {
    fun schemas(): Map<String, FieldMetadata>
}
