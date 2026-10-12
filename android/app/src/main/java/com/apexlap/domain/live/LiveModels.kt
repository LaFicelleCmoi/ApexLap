package com.apexlap.domain.live

import java.time.Instant

data class LiveSession(
    val key: Int,
    val name: String,
    /** OpenF1 `session_type`: Race, Qualifying, Practice. Sprints have type Race / Qualifying. */
    val type: String,
    val start: Instant,
    val end: Instant,
    val circuit: String,
    val country: String,
) {
    val isRace: Boolean get() = type == "Race"
    fun isLive(now: Instant): Boolean = !now.isBefore(start.minusSeconds(15 * 60)) && now.isBefore(end.plusSeconds(30 * 60))
    fun hasEnded(now: Instant): Boolean = now.isAfter(end)
}

data class LiveDriver(
    val number: Int,
    val acronym: String,
    val fullName: String,
    val team: String,
    /** RRGGBB from OpenF1, already a team identity colour. */
    val teamColour: Long?,
)

enum class TyreCompound(val letter: String) {
    SOFT("S"), MEDIUM("M"), HARD("H"), INTERMEDIATE("I"), WET("W"), UNKNOWN("?");

    companion object {
        fun of(value: String?): TyreCompound = entries.firstOrNull { it.name == value?.uppercase() } ?: UNKNOWN
    }
}

data class Stint(
    val number: Int,
    val compound: TyreCompound,
    val lapStart: Int,
    val lapEnd: Int?,
    val tyreAgeAtStart: Int,
)

/** Labels are localised by `Strings.trackStatus`. */
enum class TrackStatus { GREEN, YELLOW, VSC, SAFETY_CAR, RED, CHEQUERED }

sealed interface TimingEvent {
    val date: Instant

    data class Position(override val date: Instant, val driver: Int, val position: Int) : TimingEvent

    /** Gaps are seconds, or a textual gap ("+1 LAP"), or null when unknown. */
    data class Interval(override val date: Instant, val driver: Int, val gapToLeader: Gap?, val interval: Gap?) : TimingEvent

    /** Emitted when a lap is completed (or updated live, sector by sector). */
    data class Lap(
        override val date: Instant,
        val driver: Int,
        val lapNumber: Int,
        val duration: Double?,
        val sectors: List<Double?>,
        val isPitOutLap: Boolean,
    ) : TimingEvent

    data class StintUpdate(override val date: Instant, val driver: Int, val stint: Stint) : TimingEvent

    data class RaceControl(
        override val date: Instant,
        val category: String?,
        val flag: String?,
        val scope: String?,
        val message: String,
        val lap: Int?,
    ) : TimingEvent

    data class PitStop(override val date: Instant, val driver: Int, val lap: Int?, val duration: Double?) : TimingEvent
}

sealed interface Gap {
    data class Seconds(val value: Double) : Gap
    data class Text(val value: String) : Gap
}

data class DriverTiming(
    val driver: LiveDriver,
    val position: Int? = null,
    val gapToLeader: Gap? = null,
    val interval: Gap? = null,
    val lapsCompleted: Int = 0,
    val lastLap: Double? = null,
    val bestLap: Double? = null,
    val lastSectors: List<Double?> = listOf(null, null, null),
    val pitStops: Int = 0,
    val stints: List<Stint> = emptyList(),
) {
    /** Tyre currently fitted, from the latest stint that started on or before the lap being driven. */
    val currentStint: Stint? get() = stints.filter { it.lapStart <= lapsCompleted + 1 }.maxByOrNull { it.number }
    val tyreAge: Int? get() = currentStint?.let { it.tyreAgeAtStart + (lapsCompleted + 1 - it.lapStart).coerceAtLeast(0) }
}

data class RaceControlEntry(val date: Instant, val message: String, val flag: String?, val lap: Int?)

data class LiveTimingState(
    val session: LiveSession? = null,
    val timings: Map<Int, DriverTiming> = emptyMap(),
    val raceControl: List<RaceControlEntry> = emptyList(),
    /** Track-wide condition (green, safety car, VSC, red, chequered), ignoring local yellows. */
    val incident: TrackStatus = TrackStatus.GREEN,
    /** Marshalling sectors currently under yellow. */
    val yellowSectors: Set<Int> = emptySet(),
    val clock: Instant? = null,
    /** Driver number holding the fastest lap, and that lap time. */
    val fastestLap: Pair<Int, Double>? = null,
) {
    val trackStatus: TrackStatus
        get() = if (incident == TrackStatus.GREEN && yellowSectors.isNotEmpty()) TrackStatus.YELLOW else incident

    val leaderLap: Int get() = timings.values.maxOfOrNull { it.lapsCompleted } ?: 0

    /** Tower order: official position first, then best lap (qualifying/practice), then car number. */
    val tower: List<DriverTiming>
        get() = timings.values.sortedWith(
            compareBy<DriverTiming>({ it.position ?: Int.MAX_VALUE }, { it.bestLap ?: Double.MAX_VALUE }, { it.driver.number }),
        )
}
