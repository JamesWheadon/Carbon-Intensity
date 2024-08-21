package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

abstract class NationalGridContractTest {
    abstract val httpClient: HttpHandler

    @Test
    fun `responds when pinged on the base path`() {
        assertThat(httpClient(Request(GET, "/")).status, equalTo(OK))
    }
}

class FakeNationalGridTest : NationalGridContractTest() {
    override val httpClient = FakeNationalGrid()
}

class FakeNationalGrid : HttpHandler {
    val routes = routes(
        "/" bind GET to { Response(OK) }
    )

    override fun invoke(request: Request): Response = routes(request)
}
