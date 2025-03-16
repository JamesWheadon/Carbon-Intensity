package com.intensity.coretest

import com.fasterxml.jackson.databind.ObjectMapper
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.core.Response
import java.time.Instant

const val TIME_DIFFERENCE_TOLERANCE = 5L

fun inTimeRange(expectedStart: Instant, expectedEnd: Instant): Matcher<Instant> {
    return Matcher(
        Instant::isAfter, expectedStart.minusSeconds(TIME_DIFFERENCE_TOLERANCE)
    ) and Matcher(
        Instant::isBefore, expectedEnd.plusSeconds(TIME_DIFFERENCE_TOLERANCE)
    )
}

fun <T> isNotNull() = object : Matcher<T?> {
    override fun invoke(actual: T?): MatchResult =
        if (actual != null) MatchResult.Match else MatchResult.Mismatch("value is null")

    override val description: String get() = "is not null"
    override val negatedDescription: String get() = "is null"
}

fun <T, E> isSuccess() = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Success -> MatchResult.Match
            else -> MatchResult.Mismatch("Result is a Failure")
        }

    override val description: String get() = "Result is a Success"
    override val negatedDescription: String get() = "Result is a Failure"
}

fun <T, E> isSuccess(expected: T) = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Success -> matchExpected(actual.get())
            else -> MatchResult.Mismatch("Result is a Failure")
        }

    fun matchExpected(get: T): MatchResult =
        if (get == expected) {
            MatchResult.Match
        } else {
            MatchResult.Mismatch("Success does not match expected")
        }

    override val description: String get() = "Result is a Success"
    override val negatedDescription: String get() = "Result is a Failure"
}

fun <T, E> isFailure(expected: E) = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Failure -> matchExpected(actual.get())
            else -> MatchResult.Mismatch("Result is a Success")
        }

    fun matchExpected(get: E): MatchResult =
        if (get == expected) {
            MatchResult.Match
        } else {
            MatchResult.Mismatch("Failure does not match expected")
        }

    override val description: String get() = "Result is a Failure"
    override val negatedDescription: String get() = "Result is a Success"
}

fun hasBody(expected: String) = object : Matcher<Response> {
    override fun invoke(actual: Response): MatchResult {
        val mapper = ObjectMapper()
        return if (mapper.readTree(actual.bodyString()) == mapper.readTree(expected)) {
            MatchResult.Match
        } else {
            MatchResult.Mismatch("Response body is not equal to expected")
        }
    }

    override val description: String get() = "Response body matches expected"
    override val negatedDescription: String get() = "Response body does not match expected"
}
