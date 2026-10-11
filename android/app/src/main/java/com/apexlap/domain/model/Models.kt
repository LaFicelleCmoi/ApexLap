package com.apexlap.domain.model

import java.time.Instant
import java.time.LocalDate

enum class SessionType(val label: String, val shortLabel: String) {
    PRACTICE_1("Essais libres 1", "EL1"),
    PRACTICE_2("Essais libres 2", "EL2"),
    PRACTICE_3("Essais libres 3", "EL3"),
    SPRINT_QUALIFYING("Qualifications sprint", "SQ"),
    SPRINT("Sprint", "SPR"),
    QUALIFYING("Qualifications", "Q"),
    RACE("Course", "GP"),
}

data class Session(
    val type: SessionType,
    /** Null when the official time is not yet published (only the date is known). */
    val startsAt: Instant?,
    val date: LocalDate,
)

data class Circuit(
    val id: String,
    val name: String,
    val locality: String,
    val country: String,
)

data class Race(
    val season: Int,
    val round: Int,
    val name: String,
    val circuit: Circuit,
    val sessions: List<Session>,
) {
    val raceSession: Session? get() = sessions.firstOrNull { it.type == SessionType.RACE }
    val isSprintWeekend: Boolean get() = sessions.any { it.type == SessionType.SPRINT }

    fun isFinished(now: Instant): Boolean {
        val race = raceSession ?: return false
        val start = race.startsAt ?: race.date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
        return now.isAfter(start.plusSeconds(RACE_DURATION_SECONDS))
    }

    companion object {
        const val RACE_DURATION_SECONDS = 2L * 60 * 60
    }
}

data class Driver(
    val id: String,
    val code: String,
    val number: String?,
    val givenName: String,
    val familyName: String,
    val nationality: String,
    val dateOfBirth: LocalDate?,
) {
    val fullName: String get() = "$givenName $familyName"
}

data class Constructor(
    val id: String,
    val name: String,
    val nationality: String,
)

data class DriverStanding(
    val position: Int,
    val points: Double,
    val wins: Int,
    val driver: Driver,
    val constructor: Constructor?,
)

data class ConstructorStanding(
    val position: Int,
    val points: Double,
    val wins: Int,
    val constructor: Constructor,
)

data class RaceResult(
    val position: Int,
    val positionText: String,
    val points: Double,
    val grid: Int,
    val laps: Int,
    val status: String,
    val time: String?,
    val fastestLapRank: Int?,
    val fastestLapTime: String?,
    val driver: Driver,
    val constructor: Constructor,
) {
    val hasFastestLap: Boolean get() = fastestLapRank == 1
    /** Positive = places gained from the grid. Pit-lane starts (grid 0) are ignored. */
    val positionsGained: Int? get() = if (grid > 0 && position > 0) grid - position else null
}

/** A race or sprint result tagged with its round, as used for season-wide statistics. */
data class ScoredResult(
    val round: Int,
    val isSprint: Boolean,
    val result: RaceResult,
)

data class QualifyingResult(
    val position: Int,
    val driver: Driver,
    val constructor: Constructor,
    val q1: String?,
    val q2: String?,
    val q3: String?,
) {
    /** Best lap of the deepest session reached. */
    val bestTime: String? get() = q3 ?: q2 ?: q1
    val eliminatedIn: String? get() = when {
        q3 != null -> null
        q2 != null -> "Q2"
        else -> "Q1"
    }
}

data class CareerStats(
    val seasons: Int,
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
)

data class DriverProfile(
    val driver: Driver,
    val stats: CareerStats,
)

data class ConstructorProfile(
    val constructor: Constructor,
    val stats: CareerStats,
)

data class Countdown(val days: Long, val hours: Long, val minutes: Long, val seconds: Long) {
    val isOver: Boolean get() = days == 0L && hours == 0L && minutes == 0L && seconds == 0L
}
