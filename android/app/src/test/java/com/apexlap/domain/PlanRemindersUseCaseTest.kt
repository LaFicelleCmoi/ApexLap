package com.apexlap.domain

import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import com.apexlap.domain.usecase.PlanRemindersUseCase
import com.apexlap.domain.usecase.ReminderScope
import com.apexlap.domain.usecase.ReminderSettings
import com.apexlap.ui.components.relativeStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PlanRemindersUseCaseTest {

    private fun session(type: SessionType, at: String) =
        Instant.parse(at).let { Session(type, it, LocalDate.ofInstant(it, ZoneOffset.UTC)) }

    private val circuit = Circuit("cota", "Circuit of the Americas", "Austin", "USA")
    private val austin = Race(2026, 18, "United States Grand Prix", circuit, listOf(
        session(SessionType.PRACTICE_1, "2026-10-23T17:30:00Z"),
        session(SessionType.SPRINT_QUALIFYING, "2026-10-23T21:30:00Z"),
        session(SessionType.SPRINT, "2026-10-24T17:00:00Z"),
        session(SessionType.QUALIFYING, "2026-10-24T21:00:00Z"),
        session(SessionType.RACE, "2026-10-25T19:00:00Z"),
    ))
    private val farAway = Race(2026, 22, "Abu Dhabi Grand Prix", circuit, listOf(
        session(SessionType.RACE, "2026-12-06T13:00:00Z"),
    ))
    private val now = Instant.parse("2026-10-20T12:00:00Z")
    private val plan = PlanRemindersUseCase()

    @Test
    fun `disabled settings plan nothing`() {
        assertTrue(plan(listOf(austin), now, ReminderSettings(enabled = false)).isEmpty())
    }

    @Test
    fun `competitive scope skips practice and fires lead minutes before`() {
        val reminders = plan(listOf(austin, farAway), now, ReminderSettings(enabled = true, leadMinutes = 15))
        assertEquals(
            listOf(SessionType.SPRINT_QUALIFYING, SessionType.SPRINT, SessionType.QUALIFYING, SessionType.RACE),
            reminders.map { it.session.type },
        )
        assertEquals(Instant.parse("2026-10-25T18:45:00Z"), reminders.last().triggerAt)
    }

    @Test
    fun `sessions beyond the horizon are left for a later plan`() {
        val reminders = plan(listOf(farAway), now, ReminderSettings(enabled = true, scope = ReminderScope.ALL_SESSIONS))
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `a reminder whose time has passed is dropped, not fired late`() {
        val tenMinutesBeforeRace = Instant.parse("2026-10-25T18:50:00Z")
        val reminders = plan(listOf(austin), tenMinutesBeforeRace, ReminderSettings(enabled = true, leadMinutes = 15))
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `ids are stable per session and distinct`() {
        val settings = ReminderSettings(enabled = true, scope = ReminderScope.ALL_SESSIONS)
        val first = plan(listOf(austin), now, settings).map { it.id }
        val second = plan(listOf(austin), now.plusSeconds(60), settings).map { it.id }
        assertEquals(first, second)
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun `relative start reads naturally`() {
        assertEquals("dans 45 min", relativeStart(now, now.plusSeconds(45 * 60)))
        assertEquals("dans 3 h 05", relativeStart(now, now.plusSeconds(3 * 3600 + 5 * 60)))
        assertEquals("dans 2 h", relativeStart(now, now.plusSeconds(2 * 3600)))
        assertEquals("dans 2 j 4 h", relativeStart(now, now.plusSeconds(2 * 86400 + 4 * 3600 + 59)))
        assertEquals("maintenant", relativeStart(now, now.minusSeconds(5)))
    }
}
