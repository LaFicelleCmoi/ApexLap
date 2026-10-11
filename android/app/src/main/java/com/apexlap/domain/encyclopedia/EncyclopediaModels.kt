package com.apexlap.domain.encyclopedia

import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.Driver
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
}

/** Case- and accent-insensitive search ("perez" finds "Pérez"). */
fun String.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    fun String.fold() = java.text.Normalizer.normalize(lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    return fold().contains(query.trim().fold())
}
