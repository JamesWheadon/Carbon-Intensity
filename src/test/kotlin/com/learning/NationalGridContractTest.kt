package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import java.time.Instant

abstract class NationalGridContractTest {
    abstract val httpClient: HttpHandler

    @Test
    fun `responds when pinged on the base path`() {
        assertThat(httpClient(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `responds with forecast for the current half hour`() {
        val currentIntensity = httpClient(Request(GET, "/intensity"))
        val halfHourResponses = halfHourDataListLens(currentIntensity)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.size, equalTo(1))
    }
}

class FakeNationalGridTest : NationalGridContractTest() {
    override val httpClient = FakeNationalGrid()
}

class FakeNationalGrid : HttpHandler {
    val routes = routes(
        "/" bind GET to { Response(OK) },
        "intensity" bind GET to {
            val currentIntensity = Intensity(60, 60, "moderate")
            val currentHalfHour = HalfHourData(Instant.now().toString(), Instant.now().toString(), currentIntensity)
            Response(OK).with(halfHourDataListLens of listOf(currentHalfHour))
        }
    )

    override fun invoke(request: Request): Response = routes(request)
}

data class Intensity(
    val forecast: Long,
    val actual: Long?,
    val index: String
)

data class HalfHourData(
    val from: String,
    val to: String,
    val intensity: Intensity
)

val halfHourDataListLens = Jackson.autoBody<List<HalfHourData>>().toLens()
