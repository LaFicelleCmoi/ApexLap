package com.apexlap.widget

import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import java.time.Duration
import java.time.Instant

enum class SessionPhase { DONE, LIVE, NEXT, LATER }

data class WeekendSession(val session: Session, val phase: SessionPhase)

data class WidgetFavorites(
    val driver: DriverStanding?,
    val constructor: ConstructorStanding?,
    /** Championship leader, to show the followed driver's gap. */
    val leader: DriverStanding?,
)

/** Everything the widget draws, computed from the cached calendar (pure, so it can be unit-tested). */
data class WidgetState(
    val now: Instant,
    val hasCalendar: Boolean,
    /** Weekend on show: the one in progress, else the next one. Null once the season is over. */
    val race: Race? = null,
    val sessions: List<WeekendSession> = emptyList(),
    /** Session running right now. */
    val live: Session? = null,
    /** Next session to start, and its weekend (the following one when the current weekend is over). */
    val next: Session? = null,
    val nextRace: Race? = null,
    val favorites: WidgetFavorites? = null,
) {
    val minutesToNext: Long? get() = next?.startsAt?.let { Duration.between(now, it).toMinutes().coerceAtLeast(0) }
}

/** Typical length of each session, to know when one is live and when it is over. */
fun SessionType.typicalDuration(): Duration = when (this) {
    SessionType.RACE -> Duration.ofMinutes(120)
    SessionType.SPRINT_QUALIFYING -> Duration.ofMinutes(45)
    else -> Duration.ofMinutes(60)
}

private fun Session.endsAt(): Instant? = startsAt?.plus(type.typicalDuration())

fun buildWidgetState(calendar: List<Race>, now: Instant, favorites: WidgetFavorites? = null): WidgetState {
    val all = calendar.sortedBy { it.round }.flatMap { race -> race.sessions.map { race to it } }
    val live = all.firstOrNull { (_, s) -> val start = s.startsAt; start != null && !now.isBefore(start) && now.isBefore(s.endsAt()!!) }
    val next = all.filter { (_, s) -> s.startsAt?.isAfter(now) == true }.minByOrNull { (_, s) -> s.startsAt!! }
    val race = live?.first ?: next?.first ?: return WidgetState(now, calendar.isNotEmpty(), favorites = favorites)

    var nextMarked = false
    val sessions = race.sessions.sortedBy { it.startsAt ?: it.date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC) }.map { session ->
        val phase = when {
            session == live?.second -> SessionPhase.LIVE
            session.endsAt()?.let { !now.isBefore(it) } == true -> SessionPhase.DONE
            !nextMarked && session == next?.second -> SessionPhase.NEXT.also { nextMarked = true }
            else -> SessionPhase.LATER
        }
        WeekendSession(session, phase)
    }
    return WidgetState(
        now = now,
        hasCalendar = true,
        race = race,
        sessions = sessions,
        live = live?.second,
        next = next?.second,
        nextRace = next?.first,
        favorites = favorites,
    )
}

/**
 * When to redraw next, so the countdown stays right without waking the phone for nothing:
 * every minute in the last hour, every 15 minutes on the last day, hourly before that.
 */
fun nextRefreshDelay(state: WidgetState): Duration {
    val live = state.live
    if (live != null) {
        val untilEnd = live.endsAt()?.let { Duration.between(state.now, it) } ?: Duration.ofMinutes(5)
        return untilEnd.coerceIn(Duration.ofMinutes(1), Duration.ofMinutes(5))
    }
    val start = state.next?.startsAt ?: return Duration.ofHours(6)
    val untilStart = Duration.between(state.now, start)
    return when {
        untilStart <= Duration.ofHours(1) -> Duration.ofMinutes(1)
        untilStart <= Duration.ofDays(1) -> Duration.ofMinutes(15).coerceAtMost(untilStart.minusHours(1))
        else -> Duration.ofHours(1).coerceAtMost(untilStart.minusDays(1))
    }.coerceAtLeast(Duration.ofMinutes(1))
}
