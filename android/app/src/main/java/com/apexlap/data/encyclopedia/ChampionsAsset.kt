package com.apexlap.data.encyclopedia

import android.content.Context
import com.apexlap.domain.encyclopedia.Champion
import com.apexlap.domain.encyclopedia.ChampionDriver
import com.apexlap.domain.encyclopedia.ChampionTeam
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** World champions 1950–2025, bundled because finished seasons never change (≈150 API calls otherwise). */
@Serializable
internal data class ChampionDto(val season: Int, val races: Int, val driver: DriverDto, val constructor: TeamDto? = null) {
    @Serializable
    data class DriverDto(val id: String, val name: String, val nationality: String, val points: Double, val wins: Int, val team: String? = null, val teamId: String? = null)

    @Serializable
    data class TeamDto(val id: String, val name: String, val nationality: String, val points: Double, val wins: Int)

    fun toDomain() = Champion(
        season = season,
        races = races,
        driver = ChampionDriver(driver.id, driver.name, driver.nationality, driver.points, driver.wins, driver.team, driver.teamId),
        constructor = constructor?.let { ChampionTeam(it.id, it.name, it.nationality, it.points, it.wins) },
    )
}

object ChampionsAsset {
    const val FILE = "champions.json"

    fun parse(json: Json, text: String): List<Champion> =
        json.decodeFromString<List<ChampionDto>>(text).map { it.toDomain() }.sortedByDescending { it.season }

    fun load(context: Context, json: Json): List<Champion> =
        parse(json, context.assets.open(FILE).bufferedReader().use { it.readText() })
}
