package com.apexlap.domain.encyclopedia

import com.apexlap.domain.model.ConstructorProfile
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverProfile
import com.apexlap.domain.model.DriverStanding
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

private val birthFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

private fun plural(count: Int, word: String, plural: String = word + "s") = "$count ${if (count > 1) plural else word}"

/** "2008, 2014–2020": consecutive seasons collapsed into ranges. */
fun formatSeasons(seasons: List<Int>): String {
    val sorted = seasons.distinct().sorted()
    val ranges = mutableListOf<IntRange>()
    for (season in sorted) {
        val last = ranges.lastOrNull()
        if (last != null && last.last + 1 == season) ranges[ranges.lastIndex] = last.first..season else ranges += season..season
    }
    return ranges.joinToString(", ") { if (it.first == it.last) "${it.first}" else "${it.first}–${it.last}" }
}

/** Short biography generated from the data we already have (no external text source). */
fun driverBio(profile: DriverProfile, titles: Titles, standing: DriverStanding?, season: Int, today: LocalDate): String {
    val driver = profile.driver
    val stats = profile.stats
    val sentences = mutableListOf<String>()
    sentences += driver.dateOfBirth?.let { birth ->
        "${driver.fullName}, né le ${birthFormatter.format(birth)} (${Period.between(birth, today).years} ans)."
    } ?: "${driver.fullName}."
    if (titles.count > 0) {
        sentences += (if (titles.count == 1) "Champion du monde" else "${titles.count} fois champion du monde") +
            " (${formatSeasons(titles.seasons)})."
    }
    if (stats.races > 0) {
        val achievements = listOfNotNull(
            plural(stats.wins, "victoire").takeIf { stats.wins > 0 },
            plural(stats.podiums, "podium").takeIf { stats.podiums > 0 },
            plural(stats.poles, "pole position", "pole positions").takeIf { stats.poles > 0 },
        )
        sentences += "${plural(stats.races, "Grand Prix", "Grands Prix")} en ${plural(stats.seasons, "saison")}" +
            (if (achievements.isEmpty()) "." else " : ${achievements.joinToString(", ")}.")
    }
    standing?.let { s ->
        val team = s.constructor?.name?.let { " chez $it" }.orEmpty()
        val number = driver.number?.let { ", numéro $it" }.orEmpty()
        sentences += "En $season, il court$team$number et occupe la ${s.position}e place du championnat."
    }
    return sentences.joinToString(" ")
}

fun constructorBio(profile: ConstructorProfile, titles: Titles, standing: ConstructorStanding?, season: Int): String {
    val stats = profile.stats
    val sentences = mutableListOf<String>()
    sentences += if (stats.seasons > 0) {
        "${profile.constructor.name} compte ${plural(stats.seasons, "saison")} en Formule 1" +
            (if (stats.wins > 0) " et ${plural(stats.wins, "victoire")} en Grand Prix." else ".")
    } else {
        "${profile.constructor.name}."
    }
    if (titles.count > 0) {
        sentences += (if (titles.count == 1) "Championne du monde des constructeurs" else "${titles.count} titres constructeurs") +
            " (${formatSeasons(titles.seasons)})."
    }
    standing?.let { sentences += "En $season, l'écurie est ${it.position}e du championnat avec ${it.points.let { p -> if (p % 1.0 == 0.0) p.toLong().toString() else p.toString() }} points." }
    return sentences.joinToString(" ")
}
