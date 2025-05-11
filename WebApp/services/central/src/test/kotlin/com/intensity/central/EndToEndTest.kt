package com.intensity.central

import com.intensity.nationalgrid.FakeNationalGrid
import com.intensity.nationalgrid.NationalGridCloud
import com.intensity.observability.TestOpenTelemetry
import com.intensity.observability.TestOpenTelemetry.Companion.TestProfile.Local
import com.intensity.octopus.FakeOctopus
import com.intensity.octopus.OctopusCloud
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.events.EventFilters.AddServiceName
import org.http4k.events.EventFilters.AddZipkinTraces
import org.http4k.events.Events
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.events.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.ResetRequestTracing
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ServerFilters
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.junit.TracerBulletEvents
import org.http4k.tracing.persistence.FileSystem
import org.http4k.tracing.renderer.PumlSequenceDiagram
import org.http4k.tracing.tracer.HttpTracer
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.time.ZonedDateTime

abstract class EndToEndTest {
    @RegisterExtension
    val events = TracerBulletEvents(
        listOf(HttpTracer {
            Actor(it.metadata["service"].toString(), ActorType.System)
        }),
        listOf(PumlSequenceDiagram),
        TraceRenderPersistence.FileSystem(File("../../sequences"))
    )

    private val appClientStack = clientStack("App", events)
    val time: ZonedDateTime = ZonedDateTime.parse("2025-03-25T12:00:00Z")

    private val openTelemetry = TestOpenTelemetry(Local)
    val octopus = FakeOctopus()
    val nationalGrid = FakeNationalGrid()
    val limitCalculator = FakeLimitCalculator()
    val weightsCalculator = FakeWeightsCalculator()
    val server = serverStack("App", events).then(
        carbonIntensity(
            NationalGridCloud(
                appClientStack.then(nationalGrid.traced(serverStack("National Grid", events))),
                openTelemetry
            ),
            OctopusCloud(
                appClientStack.then(octopus.traced(serverStack("Octopus", events)))
            ),
            LimitCalculatorCloud(
                appClientStack.then(limitCalculator.traced(serverStack("Limit Calculator", events))),
                openTelemetry
            ),
            WeightsCalculatorCloud(
                appClientStack.then(weightsCalculator.traced(serverStack("Weights Calculator", events)))
            ),
            openTelemetry
        )
    )

    fun getErrorResponse(message: String) = """{"error":"$message"}"""
}

class User(events: Events, rawHttp: HttpHandler) {
    private val http = ResetRequestTracing().then(clientStack("User", events)).then(rawHttp)

    fun call(request: Request) = http(request)
}

private fun traceEvents(actorName: String) = AddZipkinTraces().then(AddServiceName(actorName))

private fun clientStack(actorName: String, events: Events) =
    ClientFilters.RequestTracing()
        .then(ReportHttpTransaction { traceEvents(actorName).then(events)(Outgoing(it)) })

private fun serverStack(actorName: String, events: Events) =
    ServerFilters.RequestTracing()
        .then(ReportHttpTransaction { traceEvents(actorName).then(events)(Incoming(it)) })

private fun HttpHandler.traced(events: Filter) = events.then(this)
