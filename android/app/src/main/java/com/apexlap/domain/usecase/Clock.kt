package com.apexlap.domain.usecase

import java.time.Instant
import java.time.ZoneOffset

/** Abstraction over the system clock so time-dependent logic stays testable. */
fun interface Clock {
    fun now(): Instant
}

fun Clock.currentSeason(): Int = now().atZone(ZoneOffset.UTC).year
