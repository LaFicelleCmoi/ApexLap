package com.apexlap.domain

import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import com.apexlap.domain.usecase.ComputeCountdownUseCase
import com.apexlap.domain.usecase.FindNextRaceUseCase
import com.apexlap.domain.usecase.FindNextSessionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class NextSessionUseCaseTest {

    private fun session(type: SessionType, at: String) =
        Instant.parse(at).let { Session(type, it, LocalDate.ofInstant(it, ZoneOffset.UTC)) }

    private val circuit = Circuit("c", "Circuit", "City", "Country")
    private val round1 = Race(2026, 1, "GP 1", circuit, listOf(
        session(SessionType.QUALIFYING, "2026-03-07T05:00:00Z"),
        session(SessionType.RACE, "2026-03-08T04:00:00Z"),
    ))
    private val round2 = Race(2026, 2, "GP 2", circuit, listOf(
        session(SessionType.PRACTICE_1, "2026-03-13T03:30:00Z"),
        session(SessionType.RACE, "2026-03-15T07:00:00Z"),
    ))
    private val calendar = listOf(round2, round1)

    @Test
    fun `next session is the earliest one strictly after now`() {
        val next = FindNextSessionUseCase()(calendar, Instant.parse("2026-03-07T06:00:00Z"))
        assertEquals(1, next?.race?.round)
        assertEquals(SessionType.RACE, next?.session?.type)
    }

    @Test
    fun `no next session once the season is over`() {
        assertNull(FindNextSessionUseCase()(calendar, Instant.parse("2026-12-31T00:00:00Z")))
    }

    @Test
    fun `next race stays the current one while its race is running`() {
        val duringRace = Instant.parse("2026-03-08T05:00:00Z")
        assertEquals(1, FindNextRaceUseCase()(calendar, duringRace)?.round)
        val afterRace = Instant.parse("2026-03-08T07:00:00Z")
        assertEquals(2, FindNextRaceUseCase()(calendar, afterRace)?.round)
    }

    @Test
    fun `countdown splits the remaining duration and never goes negative`() {
        val now = Instant.parse("2026-03-06T00:00:00Z")
        val countdown = ComputeCountdownUseCase()(Instant.parse("2026-03-08T04:05:06Z"), now)
        assertEquals(2, countdown.days)
        assertEquals(4, countdown.hours)
        assertEquals(5, countdown.minutes)
        assertEquals(6, countdown.seconds)

        assertTrue(ComputeCountdownUseCase()(now, now.plusSeconds(60)).isOver)
    }
}
