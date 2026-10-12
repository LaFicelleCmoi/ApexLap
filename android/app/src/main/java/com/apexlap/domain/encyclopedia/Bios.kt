package com.apexlap.domain.encyclopedia

import com.apexlap.domain.model.ConstructorProfile
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverProfile
import com.apexlap.domain.model.DriverStanding
import com.apexlap.i18n.AppLanguage
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

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

private fun Double.points(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

/** Short biography generated from the data we already have (no external text source). */
fun driverBio(
    profile: DriverProfile,
    titles: Titles,
    standing: DriverStanding?,
    season: Int,
    today: LocalDate,
    language: AppLanguage,
): String {
    val w = Words(language)
    val driver = profile.driver
    val stats = profile.stats
    val sentences = mutableListOf<String>()
    sentences += driver.dateOfBirth?.let { birth ->
        val date = DateTimeFormatter.ofPattern(w.t("d MMMM yyyy", "d MMMM yyyy", "d 'de' MMMM 'de' yyyy"), language.locale).format(birth)
        val age = Period.between(birth, today).years
        w.t("${driver.fullName}, né le $date ($age ans).", "${driver.fullName}, born $date ($age years old).", "${driver.fullName}, nacido el $date ($age años).")
    } ?: "${driver.fullName}."
    if (titles.count > 0) {
        val seasons = formatSeasons(titles.seasons)
        sentences += if (titles.count == 1) {
            w.t("Champion du monde ($seasons).", "World champion ($seasons).", "Campeón del mundo ($seasons).")
        } else {
            w.t("${titles.count} fois champion du monde ($seasons).", "${titles.count}-time world champion ($seasons).", "${titles.count} veces campeón del mundo ($seasons).")
        }
    }
    if (stats.races > 0) {
        val achievements = listOfNotNull(
            w.count(stats.wins, w.t("victoire", "win", "victoria"), w.t("victoires", "wins", "victorias")).takeIf { stats.wins > 0 },
            w.count(stats.podiums, w.t("podium", "podium", "podio"), w.t("podiums", "podiums", "podios")).takeIf { stats.podiums > 0 },
            w.count(stats.poles, "pole position", w.t("pole positions", "pole positions", "poles")).takeIf { stats.poles > 0 },
        )
        val races = w.count(stats.races, w.t("Grand Prix", "Grand Prix", "Gran Premio"), w.t("Grands Prix", "Grands Prix", "Grandes Premios"))
        val seasons = w.count(stats.seasons, w.t("saison", "season", "temporada"), w.t("saisons", "seasons", "temporadas"))
        sentences += w.t("$races en $seasons", "$races over $seasons", "$races en $seasons") +
            (if (achievements.isEmpty()) "." else w.t(" : ", ": ", ": ") + achievements.joinToString(", ") + ".")
    }
    standing?.let { s ->
        val team = s.constructor?.name
        val number = driver.number
        sentences += when (language) {
            AppLanguage.FR -> "En $season, il court" + (team?.let { " chez $it" } ?: "") + (number?.let { ", numéro $it" } ?: "") +
                " et occupe la ${s.position}e place du championnat."
            AppLanguage.EN -> "In $season, he races" + (team?.let { " for $it" } ?: "") + (number?.let { " with number $it" } ?: "") +
                " and is P${s.position} in the championship."
            AppLanguage.ES -> "En $season corre" + (team?.let { " con $it" } ?: "") + (number?.let { " con el número $it" } ?: "") +
                " y ocupa el ${s.position}.º puesto del campeonato."
        }
    }
    return sentences.joinToString(" ")
}

fun constructorBio(profile: ConstructorProfile, titles: Titles, standing: ConstructorStanding?, season: Int, language: AppLanguage): String {
    val w = Words(language)
    val name = profile.constructor.name
    val stats = profile.stats
    val sentences = mutableListOf<String>()
    sentences += if (stats.seasons > 0) {
        val seasons = w.count(stats.seasons, w.t("saison", "season", "temporada"), w.t("saisons", "seasons", "temporadas"))
        val wins = w.count(stats.wins, w.t("victoire", "win", "victoria"), w.t("victoires", "wins", "victorias"))
        w.t("$name compte $seasons en Formule 1", "$name has raced $seasons in Formula 1", "$name suma $seasons en la Fórmula 1") +
            if (stats.wins > 0) w.t(" et $wins en Grand Prix.", " with $wins in Grands Prix.", " y $wins en Grandes Premios.") else "."
    } else {
        "$name."
    }
    if (titles.count > 0) {
        val seasons = formatSeasons(titles.seasons)
        sentences += if (titles.count == 1) {
            w.t("Championne du monde des constructeurs ($seasons).", "Constructors' world champion ($seasons).", "Campeona del mundo de constructores ($seasons).")
        } else {
            w.t("${titles.count} titres constructeurs ($seasons).", "${titles.count} constructors' titles ($seasons).", "${titles.count} títulos de constructores ($seasons).")
        }
    }
    standing?.let {
        sentences += w.t(
            "En $season, l'écurie est ${it.position}e du championnat avec ${it.points.points()} points.",
            "In $season, the team is P${it.position} in the championship with ${it.points.points()} points.",
            "En $season, la escudería es ${it.position}.ª del campeonato con ${it.points.points()} puntos.",
        )
    }
    return sentences.joinToString(" ")
}

private class Words(private val language: AppLanguage) {
    fun t(fr: String, en: String, es: String) = when (language) {
        AppLanguage.FR -> fr
        AppLanguage.EN -> en
        AppLanguage.ES -> es
    }

    fun count(n: Int, one: String, many: String) = "$n ${if (n > 1) many else one}"
}
