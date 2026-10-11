package com.apexlap.data.local

import com.apexlap.domain.model.CareerStats
import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.ConstructorProfile
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.Driver
import com.apexlap.domain.model.DriverProfile
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.ScoredResult
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import java.time.Instant
import java.time.LocalDate

fun Race.toEntity() = RaceEntity(
    season = season,
    round = round,
    name = name,
    circuitId = circuit.id,
    circuitName = circuit.name,
    locality = circuit.locality,
    country = circuit.country,
    sessions = sessions.map { SessionRow(it.type.name, it.startsAt?.epochSecond, it.date.toString()) },
)

fun RaceEntity.toDomain() = Race(
    season = season,
    round = round,
    name = name,
    circuit = Circuit(circuitId, circuitName, locality, country),
    sessions = sessions.mapNotNull { row ->
        val type = runCatching { SessionType.valueOf(row.type) }.getOrNull() ?: return@mapNotNull null
        Session(type, row.startsAtEpochSecond?.let(Instant::ofEpochSecond), LocalDate.parse(row.date))
    },
)

fun Driver.toColumns() = DriverColumns(id, code, number, givenName, familyName, nationality, dateOfBirth?.toString())

fun DriverColumns.toDomain() = Driver(
    id = driverId,
    code = code,
    number = number,
    givenName = givenName,
    familyName = familyName,
    nationality = nationality,
    dateOfBirth = dateOfBirth?.let(LocalDate::parse),
)

fun Constructor.toColumns() = ConstructorColumns(id, name, nationality)

fun ConstructorColumns.toDomain() = Constructor(constructorId, name, nationality)

fun CareerStats.toColumns() = StatsColumns(seasons, races, wins, podiums, poles)

fun StatsColumns.toDomain() = CareerStats(seasons, races, wins, podiums, poles)

fun DriverStanding.toEntity(season: Int) =
    DriverStandingEntity(season, position, points, wins, driver.toColumns(), constructor?.toColumns())

fun DriverStandingEntity.toDomain() =
    DriverStanding(position, points, wins, driver.toDomain(), constructor?.toDomain())

fun ConstructorStanding.toEntity(season: Int) =
    ConstructorStandingEntity(season, position, points, wins, constructor.toColumns())

fun ConstructorStandingEntity.toDomain() = ConstructorStanding(position, points, wins, constructor.toDomain())

fun RaceResult.toEntity(season: Int, round: Int, isSprint: Boolean = false) = RaceResultEntity(
    season = season,
    round = round,
    isSprint = isSprint,
    position = position,
    positionText = positionText,
    points = points,
    grid = grid,
    laps = laps,
    status = status,
    time = time,
    fastestLapRank = fastestLapRank,
    fastestLapTime = fastestLapTime,
    driver = driver.toColumns(),
    constructor = constructor.toColumns(),
)

fun RaceResultEntity.toDomain() = RaceResult(
    position = position,
    positionText = positionText,
    points = points,
    grid = grid,
    laps = laps,
    status = status,
    time = time,
    fastestLapRank = fastestLapRank,
    fastestLapTime = fastestLapTime,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
)

fun RaceResultEntity.toScored() = ScoredResult(round, isSprint, toDomain())

fun QualifyingResult.toEntity(season: Int, round: Int) =
    QualifyingResultEntity(season, round, position, q1, q2, q3, driver.toColumns(), constructor.toColumns())

fun QualifyingResultEntity.toDomain() =
    QualifyingResult(position, driver.toDomain(), constructor.toDomain(), q1, q2, q3)

fun DriverProfile.toEntity(fetchedAt: Instant) =
    DriverProfileEntity(driver.toColumns(), stats.toColumns(), fetchedAt.epochSecond)

fun DriverProfileEntity.toDomain() = DriverProfile(driver.toDomain(), stats.toDomain())

fun ConstructorProfile.toEntity(fetchedAt: Instant) =
    ConstructorProfileEntity(constructor.toColumns(), stats.toColumns(), fetchedAt.epochSecond)

fun ConstructorProfileEntity.toDomain() = ConstructorProfile(constructor.toDomain(), stats.toDomain())
