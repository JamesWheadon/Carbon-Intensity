package com.intensity.core

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun formatWith(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    .withZone(ZoneOffset.UTC)
