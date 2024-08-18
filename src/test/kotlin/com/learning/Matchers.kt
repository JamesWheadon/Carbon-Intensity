package com.learning

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus

object Matchers {

    fun Response.assertReturnsString(expected: String) {
        assertThat(this, hasStatus(Status.OK).and(hasBody(expected)))
    }
}
