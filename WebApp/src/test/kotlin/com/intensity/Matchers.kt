package com.intensity

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import java.time.Instant
import java.time.LocalDateTime

const val TIME_DIFFERENCE_TOLERANCE = 5L

fun Response.assertReturnsString(expected: String) {
    assertThat(this, hasStatus(Status.OK).and(hasBody(expected)))
}

fun inTimeRange(expectedStart: Instant, expectedEnd: Instant): Matcher<Instant> {
    return Matcher(
        Instant::isAfter, expectedStart.minusSeconds(TIME_DIFFERENCE_TOLERANCE)
    ) and Matcher(
        Instant::isBefore, expectedEnd.plusSeconds(TIME_DIFFERENCE_TOLERANCE)
    )
}

fun inLocalDateTimeRange(expectedStart: LocalDateTime, expectedEnd: LocalDateTime): Matcher<LocalDateTime> {
    return Matcher(
        LocalDateTime::isAfter, expectedStart.minusSeconds(TIME_DIFFERENCE_TOLERANCE)
    ) and Matcher(
        LocalDateTime::isBefore, expectedEnd.plusSeconds(TIME_DIFFERENCE_TOLERANCE)
    )
}
