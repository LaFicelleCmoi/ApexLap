package com.apexlap.domain.usecase

import com.apexlap.domain.model.Countdown
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.Session
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class UpcomingSession(val race: Race, val session: Session)

/** Finds the next session that has not started yet across the whole calendar. */
class FindNextSessionUseCase @Inject constructor() {
    operator fun invoke(calendar: List<Race>, now: Instant): UpcomingSession? =
        calendar.asSequence()
            .flatMap { race -> race.sessions.asSequence().map { UpcomingSession(race, it) } }
            .filter { it.session.startsAt?.isAfter(now) == true }
            .minByOrNull { it.session.startsAt!! }
}

/** Finds the next race weekend: the first race whose main race has not finished. */
class FindNextRaceUseCase @Inject constructor() {
    operator fun invoke(calendar: List<Race>, now: Instant): Race? =
        calendar.sortedBy { it.round }.firstOrNull { !it.isFinished(now) }
}

class ComputeCountdownUseCase @Inject constructor() {
    operator fun invoke(target: Instant, now: Instant): Countdown {
        // Duration#toXxxPart needs API 31, so split the seconds by hand to stay compatible with minSdk 26.
        val total = Duration.between(now, target).seconds.coerceAtLeast(0)
        return Countdown(
            days = total / 86_400,
            hours = total % 86_400 / 3_600,
            minutes = total % 3_600 / 60,
            seconds = total % 60,
        )
    }
}
