package com.apexlap.domain.encyclopedia

import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.Driver
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.RaceResult

data class ChampionDriver(
    val id: String,
    val name: String,
    val nationality: String,
    val points: Double,
    val wins: Int,
    val team: String?,
    val teamId: String?,
)

data class ChampionTeam(val id: String, val name: String, val nationality: String, val points: Double, val wins: Int)

/** World champions of a finished season. The constructors' title only exists since 1958. */
data class Champion(val season: Int, val races: Int, val driver: ChampionDriver, val constructor: ChampionTeam?)

/** Titles won by a driver or a constructor, with the seasons. */
data class Titles(val count: Int, val seasons: List<Int>) {
    companion object {
        val NONE = Titles(0, emptyList())
    }
}

class ChampionsRegistry(val champions: List<Champion>) {
    private val driverTitles = champions.groupBy { it.driver.id }.mapValues { (_, c) -> Titles(c.size, c.map { it.season }) }
    private val constructorTitles = champions.filter { it.constructor != null }
        .groupBy { it.constructor!!.id }.mapValues { (_, c) -> Titles(c.size, c.map { it.season }) }

    fun driverTitles(driverId: String): Titles = driverTitles[driverId] ?: Titles.NONE
    fun constructorTitles(constructorId: String): Titles = constructorTitles[constructorId] ?: Titles.NONE
    fun season(season: Int): Champion? = champions.firstOrNull { it.season == season }
}

data class SeasonWinner(val race: Race, val winner: RaceResult)

data class CircuitWin(val season: Int, val round: Int, val raceName: String, val driver: Driver, val constructor: Constructor)

data class CircuitHistory(
    val circuit: Circuit,
    /** Most recent first. */
    val wins: List<CircuitWin>,
) {
    val grandsPrix: Int get() = wins.size
    val firstSeason: Int? get() = wins.minOfOrNull { it.season }
    val lastSeason: Int? get() = wins.maxOfOrNull { it.season }

    fun topDrivers(size: Int = 3): List<Pair<Driver, Int>> =
        wins.groupBy { it.driver.id }.values.map { it.first().driver to it.size }
            .sortedWith(compareByDescending<Pair<Driver, Int>> { it.second }.thenBy { it.first.familyName }).take(size)

    fun topConstructors(size: Int = 3): List<Pair<Constructor, Int>> =
        wins.groupBy { it.constructor.id }.values.map { it.first().constructor to it.size }
            .sortedWith(compareByDescending<Pair<Constructor, Int>> { it.second }.thenBy { it.first.name }).take(size)

    /** Where the Grand Prix of [season] sits in the history of this circuit. */
    fun contextFor(season: Int): GrandPrixContext {
        val before = wins.filter { it.season < season }
        return GrandPrixContext(
            edition = before.size + 1,
            previousWinner = before.maxByOrNull { it.season },
            mostWins = topDrivers(1).firstOrNull()?.takeIf { it.second > 1 },
        )
    }
}

data class GrandPrixContext(
    /** 1 for the first Grand Prix held at the circuit. */
    val edition: Int,
    val previousWinner: CircuitWin?,
    val mostWins: Pair<Driver, Int>?,
)

data class LapRecord(val time: String, val season: Int, val driver: Driver, val constructor: Constructor)

data class CircuitRecords(
    /** Fastest race lap (timing data exists since 2004; layouts may have changed since). */
    val raceLap: LapRecord?,
    /** Drivers who started the most races here from pole. */
    val poleSitters: List<Pair<Driver, Int>>,
)

/** "1:21.046" or "58.123" → milliseconds, null when unparseable. */
fun parseLapTime(time: String): Long? {
    val parts = time.trim().split(':')
    if (parts.size > 2) return null
    val seconds = parts.last().toDoubleOrNull() ?: return null
    val minutes = (if (parts.size == 2) parts.first().toLongOrNull() else 0L) ?: return null
    return minutes * 60_000 + Math.round(seconds * 1000)
}

/** Key facts of a weekend: winner, pole and fastest lap. */
data class GrandPrixFacts(val winner: RaceResult?, val pole: Driver?, val poleTime: String?, val fastestLap: RaceResult?) {
    val isEmpty: Boolean get() = winner == null && pole == null && fastestLap == null

    companion object {
        /** Qualifying data only exists since the 1990s: the pole falls back to the grid of the race. */
        fun of(race: List<RaceResult>, qualifying: List<QualifyingResult>): GrandPrixFacts {
            val qualifyingPole = qualifying.firstOrNull { it.position == 1 }
            return GrandPrixFacts(
                winner = race.firstOrNull { it.position == 1 },
                pole = qualifyingPole?.driver ?: race.firstOrNull { it.grid == 1 }?.driver,
                poleTime = qualifyingPole?.bestTime,
                fastestLap = race.firstOrNull { it.hasFastestLap },
            )
        }
    }
}

/** Highlights of a season, computed from the Grand Prix winners and the final standings. */
data class SeasonHighlights(
    val differentWinners: Int,
    val topWinner: Pair<Driver, Int>?,
    val topTeam: Pair<Constructor, Int>?,
    val winsFromPole: Int,
    /** Lowest starting position a race was won from. */
    val deepestWin: SeasonWinner?,
    val runnerUp: DriverStanding?,
    /** Points between the leader/champion and the runner-up. */
    val titleMargin: Double?,
) {
    companion object {
        fun of(winners: List<SeasonWinner>, standings: List<DriverStanding>): SeasonHighlights {
            val byDriver = winners.groupBy { it.winner.driver.id }.values
            val byTeam = winners.groupBy { it.winner.constructor.id }.values
            val sorted = standings.sortedBy { it.position }
            return SeasonHighlights(
                differentWinners = byDriver.size,
                topWinner = byDriver.maxByOrNull { it.size }?.let { it.first().winner.driver to it.size },
                topTeam = byTeam.maxByOrNull { it.size }?.let { it.first().winner.constructor to it.size },
                winsFromPole = winners.count { it.winner.grid == 1 },
                deepestWin = winners.filter { it.winner.grid > 1 }.maxByOrNull { it.winner.grid },
                runnerUp = sorted.getOrNull(1),
                titleMargin = if (sorted.size >= 2) sorted[0].points - sorted[1].points else null,
            )
        }
    }
}

/** Case- and accent-insensitive search ("perez" finds "Pérez"). */
fun String.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    fun String.fold() = java.text.Normalizer.normalize(lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    return fold().contains(query.trim().fold())
}
