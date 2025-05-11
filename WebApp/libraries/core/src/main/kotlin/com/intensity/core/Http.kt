package com.intensity.core

import org.http4k.lens.Query
import org.http4k.lens.zonedDateTime

val startTimeLens = Query.zonedDateTime().optional("start")
val endTimeLens = Query.zonedDateTime().optional("end")
