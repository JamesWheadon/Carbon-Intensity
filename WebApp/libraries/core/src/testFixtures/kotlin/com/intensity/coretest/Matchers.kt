package com.intensity.coretest

import com.fasterxml.jackson.databind.ObjectMapper
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.describe
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.core.Response
import java.time.ZonedDateTime

const val TIME_DIFFERENCE_TOLERANCE = 5L

fun inTimeRange(expectedStart: ZonedDateTime, expectedEnd: ZonedDateTime): Matcher<ZonedDateTime> {
    return Matcher(
        ZonedDateTime::isAfter, expectedStart.minusSeconds(TIME_DIFFERENCE_TOLERANCE)
    ) and Matcher(
        ZonedDateTime::isBefore, expectedEnd.plusSeconds(TIME_DIFFERENCE_TOLERANCE)
    )
}

fun <T, E> isSuccess() = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Success -> Match
            else -> Mismatch("Result is a Failure")
        }

    override val description: String get() = "Result is a Success"
    override val negatedDescription: String get() = "Result is a Failure"
}

fun <T, E> isSuccess(expected: T) = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Success -> matchExpected(actual.get())
            else -> Mismatch("Result is a Failure")
        }

    fun matchExpected(actual: T): MatchResult =
        if (actual == expected) {
            Match
        } else {
            Mismatch("was: ${describe(actual)}")
        }

    override val description: String get() = "is equal to ${describe(expected)}"
    override val negatedDescription: String get() = "is not equal to ${describe(expected)}"
}

fun <T, E> isFailure(expected: E) = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Failure -> matchExpected(actual.get())
            else -> Mismatch("Result is a Success")
        }

    fun matchExpected(actual: E): MatchResult =
        if (actual == expected) {
            Match
        } else {
            Mismatch("was: ${describe(actual)}")
        }

    override val description: String get() = "is equal to ${describe(expected)}"
    override val negatedDescription: String get() = "is not equal to ${describe(expected)}"
}

fun hasBody(expected: String) = object : Matcher<Response> {
    override fun invoke(actual: Response): MatchResult {
        val mapper = ObjectMapper()
        return if (mapper.readTree(actual.bodyString()) == mapper.readTree(expected)) {
            Match
        } else {
            Mismatch("was: ${describe(actual.bodyString())}")
        }
    }

    override val description: String get() = "is equal to ${describe(expected)}"
    override val negatedDescription: String get() = "is not equal to ${describe(expected)}"
}
