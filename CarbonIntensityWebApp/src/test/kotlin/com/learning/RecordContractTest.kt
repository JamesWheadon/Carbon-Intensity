package com.learning

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Path
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

abstract class RecordContractTest {
    abstract val client: HttpHandler

    @Test
    fun `records answer`() {
        Recorder(client).record(123)
        checkAnswerRecorded()
    }

    open fun checkAnswerRecorded() {
    }
}

class FakeRecorderTest : RecordContractTest() {
    override val client = FakeRecorderHttp()

    override fun checkAnswerRecorded() {
        assertThat(client.calls, equalTo(listOf(123)))
    }
}

class FakeRecorderHttp : HttpHandler {
    val calls = mutableListOf<Int>()

    private val app = CatchLensFailure.then(
        routes(
            "/{answer}" bind POST to { request ->
                calls.add(Path.int().of("answer")(request))
                Response(ACCEPTED)
            }
        )
    )

    override fun invoke(request: Request): Response = app(request)
}
