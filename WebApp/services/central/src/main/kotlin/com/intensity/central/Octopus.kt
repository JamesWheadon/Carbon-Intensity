package com.intensity.central

import com.intensity.core.errorResponseLens
import com.intensity.octopus.Octopus
import dev.forkhandles.result4k.fold
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.partition
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.bind

fun octopusProducts(
    octopus: Octopus
) = "tariffs/octopus" bind POST to { _: Request ->
    octopus.products().map { products ->
        products.results.map { product ->
            octopus.product(product.code)
                .map {
                    OctopusProduct(product.code, it.tariffs.values.map { it.monthly.code })
                }
        }.partition().first
    }.fold(
        { products ->
            Response(OK).with(octopusProductsLens of OctopusProducts(products))
        },
        { failed ->
            Response(INTERNAL_SERVER_ERROR).with(errorResponseLens of failed.toErrorResponse())
        }
    )
}

val octopusProductsLens = Jackson.autoBody<OctopusProducts>().toLens()

data class OctopusProducts(val products: List<OctopusProduct>)
data class OctopusProduct(val name: String, val tariffs: List<String>)
