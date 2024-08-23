package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
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
        println(currentIntensity)
        val halfHourResponses = nationalGridDataLens(currentIntensity)
        println(halfHourResponses)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(1))
    }

    @Test
    fun `responds with forecast for the current day`() {
        val currentIntensity = httpClient(Request(GET, "/intensity/date"))
        val halfHourResponses = nationalGridDataLens(currentIntensity)

        assertThat(currentIntensity.status, equalTo(OK))
        assertThat(halfHourResponses.data.size, equalTo(24))
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
            val currentHalfHour = HalfHourData(Instant.now(), Instant.now(), currentIntensity)
            Response(OK).with(nationalGridDataLens of NationalGridData(listOf(currentHalfHour)))
        },
        "intensity/date" bind GET to {
            val currentIntensity = Intensity(60, 60, "moderate")
            val currentHalfHour = HalfHourData(Instant.now(), Instant.now(), currentIntensity)
            Response(OK).with(nationalGridDataLens of NationalGridData(List(24) { currentHalfHour }))
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
    val from: Instant,
    val to: Instant,
    val intensity: Intensity
)

data class NationalGridData(
    val data: List<HalfHourData>
)

val nationalGridDataLens = Jackson.autoBody<NationalGridData>().toLens()
