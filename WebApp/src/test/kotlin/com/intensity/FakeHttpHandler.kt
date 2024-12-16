package com.intensity

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.routing.RoutingHttpHandler

interface FakeHttpHandler : HttpHandler {
    val routes: RoutingHttpHandler
}

class TracedHttpHandler(private val handler: FakeHttpHandler, private val events: Filter) : HttpHandler {
    override fun invoke(request: Request): Response = events.then(handler.routes)(request)
}
