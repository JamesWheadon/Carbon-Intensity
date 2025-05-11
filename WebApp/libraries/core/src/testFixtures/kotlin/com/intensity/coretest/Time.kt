package com.intensity.coretest

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun ZonedDateTime.formatted(): String = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
