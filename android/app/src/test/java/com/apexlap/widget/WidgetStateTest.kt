package com.apexlap.widget

import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import com.apexlap.i18n.AppLanguage
import com.apexlap.i18n.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class WidgetStateTest {

    private fun session(type: SessionType, at: String) = Instant.parse(at).let { Session(type, it, it.atZone(ZoneOffset.UTC).toLocalDate()) }

    private val singapore = Race(
        2026, 18, "Singapore Grand Prix", Circuit("marina_bay", "Marina Bay", "Singapore", "Singapore"),
        listOf(
            session(SessionType.PRACTICE_1, "2026-10-09T09:30:00Z"),
            session(SessionType.PRACTICE_2, "2026-10-09T13:00:00Z"),
            session(SessionType.PRACTICE_3, "2026-10-10T09:30:00Z"),
            session(SessionType.QUALIFYING, "2026-10-10T13:00:00Z"),
            session(SessionType.RACE, "2026-10-11T12:00:00Z"),
        ),
    )
    private val austin = Race(
        2026, 19, "United States Grand Prix", Circuit("americas", "COTA", "Austin", "USA"),
        listOf(session(SessionType.PRACTICE_1, "2026-10-23T17:30:00Z"), session(SessionType.RACE, "2026-10-25T19:00:00Z")),
    )
    private val calendar = listOf(austin, singapore)

    @Test
    fun `a running session is live and the weekend shows its progress`() {
        val state = buildWidgetState(calendar, Instant.parse("2026-10-10T13:20:00Z"))
        assertEquals(18, state.race?.round)
        assertEquals(SessionType.QUALIFYING, state.live?.type)
        assertEquals(
            listOf(SessionPhase.DONE, SessionPhase.DONE, SessionPhase.DONE, SessionPhase.LIVE, SessionPhase.NEXT),
            state.sessions.map { it.phase },
        )
        assertEquals(Duration.ofMinutes(5), nextRefreshDelay(state))
    }

    @Test
    fun `between sessions the next one is counted down`() {
        val state = buildWidgetState(calendar, Instant.parse("2026-10-11T11:15:00Z"))
        assertNull(state.live)
        assertEquals(SessionType.RACE, state.next?.type)
        assertEquals(45L, state.minutesToNext)
        assertEquals(Duration.ofMinutes(1), nextRefreshDelay(state))
    }

    @Test
    fun `once the race is over the next weekend takes over`() {
        val state = buildWidgetState(calendar, Instant.parse("2026-10-12T08:00:00Z"))
        assertEquals(19, state.race?.round)
        assertEquals(SessionPhase.NEXT, state.sessions.first().phase)
        // More than a day away: hourly redraws.
        assertEquals(Duration.ofHours(1), nextRefreshDelay(state))
    }

    @Test
    fun `refresh tightens on the last day`() {
        val state = buildWidgetState(calendar, Instant.parse("2026-10-23T07:30:00Z"))
        assertEquals(Duration.ofMinutes(15), nextRefreshDelay(state))
        val justBefore = buildWidgetState(calendar, Instant.parse("2026-10-23T16:20:00Z"))
        // 70 min to go: wake up when the last hour starts.
        assertEquals(Duration.ofMinutes(10), nextRefreshDelay(justBefore))
    }

    @Test
    fun `after the last race of the season there is nothing to show`() {
        val state = buildWidgetState(calendar, Instant.parse("2026-12-01T00:00:00Z"))
        assertNull(state.race)
        assertEquals(Duration.ofHours(6), nextRefreshDelay(state))
    }

    @Test
    fun `compact countdown reads in each language`() {
        assertEquals("2 j 4 h", I18n.stringsOf(AppLanguage.FR).compactDuration(2 * 1440 + 4 * 60 + 12))
        assertEquals("3 h 05", I18n.stringsOf(AppLanguage.EN).compactDuration(185))
        assertEquals("45 min", I18n.stringsOf(AppLanguage.ES).compactDuration(45))
    }
}
