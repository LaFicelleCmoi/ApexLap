package com.apexlap.data.local

import androidx.room.Embedded
import androidx.room.Entity
import kotlinx.serialization.Serializable

data class DriverColumns(
    val driverId: String,
    val code: String,
    val number: String?,
    val givenName: String,
    val familyName: String,
    val nationality: String,
    val dateOfBirth: String?,
)

data class ConstructorColumns(
    val constructorId: String,
    val name: String,
    val nationality: String,
)

data class StatsColumns(
    val seasons: Int,
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
)

@Serializable
data class SessionRow(val type: String, val startsAtEpochSecond: Long?, val date: String)

@Entity(tableName = "races", primaryKeys = ["season", "round"])
data class RaceEntity(
    val season: Int,
    val round: Int,
    val name: String,
    val circuitId: String,
    val circuitName: String,
    val locality: String,
    val country: String,
    val sessions: List<SessionRow>,
)

@Entity(tableName = "driver_standings", primaryKeys = ["season", "driverId"])
data class DriverStandingEntity(
    val season: Int,
    val position: Int,
    val points: Double,
    val wins: Int,
    @Embedded val driver: DriverColumns,
    @Embedded(prefix = "team_") val constructor: ConstructorColumns?,
)

@Entity(tableName = "constructor_standings", primaryKeys = ["season", "constructorId"])
data class ConstructorStandingEntity(
    val season: Int,
    val position: Int,
    val points: Double,
    val wins: Int,
    @Embedded val constructor: ConstructorColumns,
)

@Entity(tableName = "race_results", primaryKeys = ["season", "round", "isSprint", "driverId"])
data class RaceResultEntity(
    val season: Int,
    val round: Int,
    val isSprint: Boolean,
    val position: Int,
    val positionText: String,
    val points: Double,
    val grid: Int,
    val laps: Int,
    val status: String,
    val time: String?,
    val fastestLapRank: Int?,
    val fastestLapTime: String?,
    @Embedded val driver: DriverColumns,
    @Embedded(prefix = "team_") val constructor: ConstructorColumns,
)

@Entity(tableName = "qualifying_results", primaryKeys = ["season", "round", "driverId"])
data class QualifyingResultEntity(
    val season: Int,
    val round: Int,
    val position: Int,
    val q1: String?,
    val q2: String?,
    val q3: String?,
    @Embedded val driver: DriverColumns,
    @Embedded(prefix = "team_") val constructor: ConstructorColumns,
)

@Entity(tableName = "driver_profiles", primaryKeys = ["driverId"])
data class DriverProfileEntity(
    @Embedded val driver: DriverColumns,
    @Embedded val stats: StatsColumns,
    val fetchedAtEpochSecond: Long,
)

@Entity(tableName = "constructor_profiles", primaryKeys = ["constructorId"])
data class ConstructorProfileEntity(
    @Embedded val constructor: ConstructorColumns,
    @Embedded val stats: StatsColumns,
    val fetchedAtEpochSecond: Long,
)
