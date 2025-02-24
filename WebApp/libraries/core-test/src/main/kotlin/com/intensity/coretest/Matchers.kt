package com.intensity.coretest

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
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

fun <T, E> isFailure() = object : Matcher<Result<T, E>> {
    override fun invoke(actual: Result<T, E>) =
        when (actual) {
            is Failure -> MatchResult.Match
            else -> MatchResult.Mismatch("Result is a Success")
        }

    override val description: String get() = "Result is a Failure"
    override val negatedDescription: String get() = "Result is a Success"
}
