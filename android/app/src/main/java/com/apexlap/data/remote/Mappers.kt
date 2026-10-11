package com.apexlap.data.remote

import com.apexlap.data.remote.dto.CircuitDto
import com.apexlap.data.remote.dto.ConstructorDto
import com.apexlap.data.remote.dto.ConstructorStandingDto
import com.apexlap.data.remote.dto.DriverDto
import com.apexlap.data.remote.dto.DriverStandingDto
import com.apexlap.data.remote.dto.RaceDto
import com.apexlap.data.remote.dto.QualifyingDto
import com.apexlap.data.remote.dto.ResultDto
import com.apexlap.data.remote.dto.SessionDto
import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.Driver
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.Session
import com.apexlap.domain.model.SessionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset

fun RaceDto.toDomain(): Race {
    val sessions = buildList {
        firstPractice?.toSession(SessionType.PRACTICE_1)?.let(::add)
        secondPractice?.toSession(SessionType.PRACTICE_2)?.let(::add)
        thirdPractice?.toSession(SessionType.PRACTICE_3)?.let(::add)
        (sprintQualifying ?: sprintShootout)?.toSession(SessionType.SPRINT_QUALIFYING)?.let(::add)
        sprint?.toSession(SessionType.SPRINT)?.let(::add)
        qualifying?.toSession(SessionType.QUALIFYING)?.let(::add)
        SessionDto(date, time).toSession(SessionType.RACE)?.let(::add)
    }.sortedWith(compareBy({ it.date }, { it.startsAt }))
    return Race(
        season = season.toInt(),
        round = round.toInt(),
        name = raceName,
        circuit = circuit.toDomain(),
        sessions = sessions,
    )
}

private fun SessionDto.toSession(type: SessionType): Session? {
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return Session(type = type, startsAt = parseInstant(day, time), date = day)
}

/** Ergast times look like `05:00:00Z`; some historical entries omit the offset. */
internal fun parseInstant(date: LocalDate, time: String?): Instant? {
    if (time.isNullOrBlank()) return null
    val offsetTime = runCatching { OffsetTime.parse(time) }.getOrNull()
        ?: runCatching { LocalTime.parse(time).atOffset(ZoneOffset.UTC) }.getOrNull()
        ?: return null
    return offsetTime.atDate(date).toInstant()
}

fun CircuitDto.toDomain() = Circuit(
    id = circuitId,
    name = circuitName,
    locality = location.locality,
    country = location.country,
)

fun DriverDto.toDomain() = Driver(
    id = driverId,
    code = code ?: familyName.take(3).uppercase(),
    number = permanentNumber,
    givenName = givenName,
    familyName = familyName,
    nationality = nationality,
    dateOfBirth = dateOfBirth?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
)

fun ConstructorDto.toDomain() = Constructor(id = constructorId, name = name, nationality = nationality)

fun DriverStandingDto.toDomain(fallbackPosition: Int) = DriverStanding(
    position = position?.toIntOrNull() ?: fallbackPosition,
    points = points.toDoubleOrNull() ?: 0.0,
    wins = wins.toIntOrNull() ?: 0,
    driver = driver.toDomain(),
    constructor = constructors.lastOrNull()?.toDomain(),
)

fun ConstructorStandingDto.toDomain(fallbackPosition: Int) = ConstructorStanding(
    position = position?.toIntOrNull() ?: fallbackPosition,
    points = points.toDoubleOrNull() ?: 0.0,
    wins = wins.toIntOrNull() ?: 0,
    constructor = constructor.toDomain(),
)

fun ResultDto.toDomain() = RaceResult(
    position = position.toIntOrNull() ?: 0,
    positionText = positionText,
    points = points.toDoubleOrNull() ?: 0.0,
    grid = grid.toIntOrNull() ?: 0,
    laps = laps.toIntOrNull() ?: 0,
    status = status,
    time = time?.time,
    fastestLapRank = fastestLap?.rank?.toIntOrNull(),
    fastestLapTime = fastestLap?.time?.time,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
)

fun QualifyingDto.toDomain() = QualifyingResult(
    position = position.toIntOrNull() ?: 0,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
    q1 = q1?.takeIf { it.isNotBlank() },
    q2 = q2?.takeIf { it.isNotBlank() },
    q3 = q3?.takeIf { it.isNotBlank() },
)
