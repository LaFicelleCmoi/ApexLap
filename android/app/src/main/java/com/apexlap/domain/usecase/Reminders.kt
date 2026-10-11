package com.apexlap.domain.usecase

import com.apexlap.domain.model.Race
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class ReminderScope(val label: String) {
    ALL_SESSIONS("Toutes les séances"),
    COMPETITIVE("Qualifs, sprints et courses"),
    RACES_ONLY("Courses uniquement");

    fun includes(type: SessionType): Boolean = when (this) {
        ALL_SESSIONS -> true
        COMPETITIVE -> type != SessionType.PRACTICE_1 && type != SessionType.PRACTICE_2 && type != SessionType.PRACTICE_3
        RACES_ONLY -> type == SessionType.RACE
    }
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val leadMinutes: Int = 15,
    val scope: ReminderScope = ReminderScope.COMPETITIVE,
) {
    companion object {
        val LEAD_OPTIONS = listOf(5, 15, 30, 60)
    }
}

data class PlannedReminder(
    /** Stable per session, used as the alarm request code so re-planning replaces instead of duplicating. */
    val id: Int,
    val race: Race,
    val session: Session,
    val triggerAt: Instant,
)

/** Picks the sessions to remind about within [horizon] and when to fire each reminder. */
class PlanRemindersUseCase @Inject constructor() {
    operator fun invoke(
        calendar: List<Race>,
        now: Instant,
        settings: ReminderSettings,
        horizon: Duration = DEFAULT_HORIZON,
    ): List<PlannedReminder> {
        if (!settings.enabled) return emptyList()
        val lead = Duration.ofMinutes(settings.leadMinutes.toLong())
        return calendar.flatMap { race ->
            race.sessions.mapNotNull { session ->
                val start = session.startsAt ?: return@mapNotNull null
                val trigger = start.minus(lead)
                // A reminder whose time has passed is dropped rather than fired late.
                if (!settings.scope.includes(session.type) || !trigger.isAfter(now) || start.isAfter(now.plus(horizon))) {
                    return@mapNotNull null
                }
                PlannedReminder(idOf(race, session), race, session, trigger)
            }
        }.sortedBy { it.triggerAt }
    }

    companion object {
        val DEFAULT_HORIZON: Duration = Duration.ofDays(14)

        fun idOf(race: Race, session: Session): Int = race.season * 10_000 + race.round * 10 + session.type.ordinal
    }
}
