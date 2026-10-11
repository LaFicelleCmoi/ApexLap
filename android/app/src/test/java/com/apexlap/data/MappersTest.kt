package com.apexlap.data

import com.apexlap.data.remote.dto.JolpicaResponse
import com.apexlap.data.remote.dto.RaceTableData
import com.apexlap.data.remote.dto.StandingsData
import com.apexlap.data.remote.parseInstant
import com.apexlap.data.remote.toDomain
import com.apexlap.domain.model.SessionType
import com.apexlap.testing.decodeFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MappersTest {

    private val calendar = decodeFixture<JolpicaResponse<RaceTableData>>("calendar_2026.json")
        .data.raceTable.races.map { it.toDomain() }

    @Test
    fun `classic weekend maps five sessions in chronological order`() {
        val australia = calendar.first()
        assertEquals("Australian Grand Prix", australia.name)
        assertEquals("Australia", australia.circuit.country)
        assertEquals(
            listOf(SessionType.PRACTICE_1, SessionType.PRACTICE_2, SessionType.PRACTICE_3, SessionType.QUALIFYING, SessionType.RACE),
            australia.sessions.map { it.type },
        )
        assertEquals(Instant.parse("2026-03-08T04:00:00Z"), australia.raceSession?.startsAt)
        assertFalse(australia.isSprintWeekend)
    }

    @Test
    fun `sprint weekend orders sprint qualifying before sprint`() {
        val china = calendar[1]
        assertTrue(china.isSprintWeekend)
        assertEquals(
            listOf(SessionType.PRACTICE_1, SessionType.SPRINT_QUALIFYING, SessionType.SPRINT, SessionType.QUALIFYING, SessionType.RACE),
            china.sessions.map { it.type },
        )
    }

    @Test
    fun `race results map positions, fastest lap and grid delta`() {
        val results = decodeFixture<JolpicaResponse<RaceTableData>>("results_singapore_2026.json")
            .data.raceTable.races.single().results.map { it.toDomain() }

        val winner = results.first()
        assertEquals("max_verstappen", winner.driver.id)
        assertEquals("VER", winner.driver.code)
        assertEquals(25.0, winner.points, 0.0)
        assertEquals("1:42:00.785", winner.time)
        assertFalse(winner.hasFastestLap)

        val second = results[1]
        assertTrue(second.hasFastestLap)

        val third = results[2]
        assertEquals(1, third.positionsGained)
    }

    @Test
    fun `driver standings keep the latest constructor`() {
        val standings = decodeFixture<JolpicaResponse<StandingsData>>("driver_standings_2026.json")
            .data.standingsTable.lists.single().driverStandings
            .mapIndexed { i, dto -> dto.toDomain(i + 1) }

        assertEquals("antonelli", standings.first().driver.id)
        assertEquals(340.0, standings.first().points, 0.0)
        assertEquals("mercedes", standings.first().constructor?.id)
    }

    @Test
    fun `times without offset are read as UTC and missing times yield null`() {
        val day = LocalDate.of(1990, 3, 11)
        assertEquals(Instant.parse("1990-03-11T14:00:00Z"), parseInstant(day, "14:00:00"))
        assertNull(parseInstant(day, null))
        assertNull(parseInstant(day, "not-a-time"))
    }
}
