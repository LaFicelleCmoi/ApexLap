package com.apexlap.domain.usecase

import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.Driver
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.ScoredResult
import javax.inject.Inject

data class DriverProgression(
    val driver: Driver,
    /** Team at the driver's latest round. */
    val constructor: Constructor,
    /** Cumulative points after each round of [ChampionshipProgression.rounds]. */
    val cumulativePoints: List<Double>,
) {
    val total: Double get() = cumulativePoints.lastOrNull() ?: 0.0
}

data class ChampionshipProgression(
    val rounds: List<Int>,
    /** Sorted by total points, leader first. */
    val drivers: List<DriverProgression>,
)

/** Rebuilds the championship round by round from race + sprint points. */
class ComputeChampionshipProgressionUseCase @Inject constructor() {
    operator fun invoke(results: List<ScoredResult>): ChampionshipProgression {
        val rounds = results.map { it.round }.distinct().sorted()
        val drivers = results.groupBy { it.result.driver.id }.values.map { rows ->
            val pointsByRound = rows.groupBy { it.round }.mapValues { (_, r) -> r.sumOf { it.result.points } }
            var total = 0.0
            val cumulative = rounds.map { round ->
                total += pointsByRound[round] ?: 0.0
                total
            }
            val latest = rows.maxBy { it.round }.result
            DriverProgression(latest.driver, latest.constructor, cumulative)
        }
        return ChampionshipProgression(rounds, drivers.sortedByDescending { it.total })
    }
}

data class LeaderEntry(val driver: Driver, val constructor: Constructor, val value: Int)

enum class LeaderCategory(val title: String) {
    WINS("Victoires"),
    PODIUMS("Podiums"),
    POLES("Poles"),
    FASTEST_LAPS("Meilleurs tours"),
    POSITIONS_GAINED("Places gagnées"),
    DNFS("Abandons"),
}

data class LeaderBoard(val category: LeaderCategory, val entries: List<LeaderEntry>)

/** Top 3 of the season for each category, computed from Grand Prix results (sprints excluded). */
class ComputeSeasonLeadersUseCase @Inject constructor() {
    operator fun invoke(results: List<ScoredResult>, size: Int = 3): List<LeaderBoard> {
        val races = results.filterNot { it.isSprint }.map { it.result }
        val byDriver = races.groupBy { it.driver.id }
        return LeaderCategory.entries.map { category ->
            val entries = byDriver.values.map { rows ->
                val latest = rows.last()
                LeaderEntry(latest.driver, latest.constructor, rows.score(category))
            }
                .filter { it.value > 0 }
                .sortedWith(compareByDescending<LeaderEntry> { it.value }.thenBy { it.driver.familyName })
                .take(size)
            LeaderBoard(category, entries)
        }
    }

    private fun List<RaceResult>.score(category: LeaderCategory): Int = when (category) {
        LeaderCategory.WINS -> count { it.position == 1 }
        LeaderCategory.PODIUMS -> count { it.position in 1..3 }
        LeaderCategory.POLES -> count { it.grid == 1 }
        LeaderCategory.FASTEST_LAPS -> count { it.hasFastestLap }
        LeaderCategory.POSITIONS_GAINED -> filter { it.isClassified }.sumOf { it.positionsGained ?: 0 }
        LeaderCategory.DNFS -> count { it.isRetirement }
    }
}

/** Classified finishers have a numeric `positionText`; R/D/W/N/E/F mark retirements and exclusions. */
val RaceResult.isClassified: Boolean get() = positionText.isNotEmpty() && positionText.all(Char::isDigit)

/**
 * A retirement (DNF): not classified (R, N), or classified after covering 90 % of the distance but stopped
 * before the flag (status other than a finish). Non-starters (W), disqualifications (D) and exclusions (E, F) are not.
 */
val RaceResult.isRetirement: Boolean get() = when {
    status.contains("not start", ignoreCase = true) -> false
    positionText == "R" || positionText == "N" -> true
    isClassified -> !(status == "Finished" || status == "Lapped" || status.startsWith("+"))
    else -> false
}

data class DuelSide(
    val driver: Driver,
    val constructor: Constructor,
    val points: Double,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
    val bestFinish: Int?,
    val averageFinish: Double?,
    val dnfs: Int,
    /** Grands Prix (both drivers entered) finished ahead of the rival. */
    val racesAhead: Int,
    /** Grands Prix started ahead of the rival on the grid. */
    val startsAhead: Int,
)

data class HeadToHead(val first: DuelSide, val second: DuelSide, val sharedRaces: Int)

class ComputeHeadToHeadUseCase @Inject constructor() {
    operator fun invoke(results: List<ScoredResult>, firstId: String, secondId: String): HeadToHead? {
        if (firstId == secondId) return null
        val first = results.filter { it.result.driver.id == firstId }
        val second = results.filter { it.result.driver.id == secondId }
        if (first.isEmpty() || second.isEmpty()) return null

        val firstRaces = first.filterNot { it.isSprint }.associateBy { it.round }
        val secondRaces = second.filterNot { it.isSprint }.associateBy { it.round }
        val shared = firstRaces.keys intersect secondRaces.keys
        val pairs = shared.map { firstRaces.getValue(it).result to secondRaces.getValue(it).result }

        fun side(rows: List<ScoredResult>, racesAhead: Int, startsAhead: Int): DuelSide {
            val races = rows.filterNot { it.isSprint }.map { it.result }
            val classified = races.filter { it.isClassified }
            val latest = rows.maxBy { it.round }.result
            return DuelSide(
                driver = latest.driver,
                constructor = latest.constructor,
                points = rows.sumOf { it.result.points },
                wins = races.count { it.position == 1 },
                podiums = races.count { it.position in 1..3 },
                poles = races.count { it.grid == 1 },
                bestFinish = classified.minOfOrNull { it.position },
                averageFinish = classified.map { it.position }.average().takeIf { classified.isNotEmpty() },
                dnfs = races.count { it.isRetirement },
                racesAhead = racesAhead,
                startsAhead = startsAhead,
            )
        }

        val firstAhead = pairs.count { (a, b) -> a.position < b.position }
        val firstStartsAhead = pairs.count { (a, b) -> a.gridOrLast < b.gridOrLast }
        val secondStartsAhead = pairs.count { (a, b) -> b.gridOrLast < a.gridOrLast }
        return HeadToHead(
            first = side(first, firstAhead, firstStartsAhead),
            second = side(second, pairs.size - firstAhead, secondStartsAhead),
            sharedRaces = pairs.size,
        )
    }

    /** Grid 0 means a pit-lane start: behind everyone. */
    private val RaceResult.gridOrLast: Int get() = if (grid > 0) grid else Int.MAX_VALUE
}
