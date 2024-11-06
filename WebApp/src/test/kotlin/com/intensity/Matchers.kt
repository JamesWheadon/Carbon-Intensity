package com.intensity

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
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
    override fun invoke(actual: T?): MatchResult = if (actual != null) MatchResult.Match else MatchResult.Mismatch("value is null")
    override val description: String get() = "is not null"
    override val negatedDescription: String get() = "is null"
}
