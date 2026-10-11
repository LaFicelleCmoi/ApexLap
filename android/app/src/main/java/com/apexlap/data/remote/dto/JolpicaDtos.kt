package com.apexlap.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Jolpica keeps the Ergast response format: every numeric value is a string.

@Serializable
data class JolpicaResponse<T>(@SerialName("MRData") val data: T)

@Serializable
data class RaceTableData(
    val total: String,
    @SerialName("RaceTable") val raceTable: RaceTableDto,
)

@Serializable
data class RaceTableDto(@SerialName("Races") val races: List<RaceDto> = emptyList())

@Serializable
data class StandingsData(@SerialName("StandingsTable") val standingsTable: StandingsTableDto)

@Serializable
data class StandingsTableDto(
    @SerialName("StandingsLists") val lists: List<StandingsListDto> = emptyList(),
)

@Serializable
data class StandingsListDto(
    val season: String,
    val round: String,
    @SerialName("DriverStandings") val driverStandings: List<DriverStandingDto> = emptyList(),
    @SerialName("ConstructorStandings") val constructorStandings: List<ConstructorStandingDto> = emptyList(),
)

@Serializable
data class DriverTableData(@SerialName("DriverTable") val driverTable: DriverTableDto)

@Serializable
data class DriverTableDto(@SerialName("Drivers") val drivers: List<DriverDto> = emptyList())

@Serializable
data class ConstructorTableData(@SerialName("ConstructorTable") val constructorTable: ConstructorTableDto)

@Serializable
data class ConstructorTableDto(
    @SerialName("Constructors") val constructors: List<ConstructorDto> = emptyList(),
)

/** Any MRData payload; used when only the `total` count matters (career statistics). */
@Serializable
data class TotalData(val total: String)

@Serializable
data class RaceDto(
    val season: String,
    val round: String,
    val raceName: String,
    @SerialName("Circuit") val circuit: CircuitDto,
    val date: String,
    val time: String? = null,
    @SerialName("FirstPractice") val firstPractice: SessionDto? = null,
    @SerialName("SecondPractice") val secondPractice: SessionDto? = null,
    @SerialName("ThirdPractice") val thirdPractice: SessionDto? = null,
    @SerialName("SprintQualifying") val sprintQualifying: SessionDto? = null,
    @SerialName("SprintShootout") val sprintShootout: SessionDto? = null,
    @SerialName("Sprint") val sprint: SessionDto? = null,
    @SerialName("Qualifying") val qualifying: SessionDto? = null,
    @SerialName("Results") val results: List<ResultDto> = emptyList(),
    @SerialName("SprintResults") val sprintResults: List<ResultDto> = emptyList(),
    @SerialName("QualifyingResults") val qualifyingResults: List<QualifyingDto> = emptyList(),
)

@Serializable
data class QualifyingDto(
    val position: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructor") val constructor: ConstructorDto,
    @SerialName("Q1") val q1: String? = null,
    @SerialName("Q2") val q2: String? = null,
    @SerialName("Q3") val q3: String? = null,
)

@Serializable
data class SessionDto(val date: String, val time: String? = null)

@Serializable
data class CircuitDto(
    val circuitId: String,
    val circuitName: String,
    @SerialName("Location") val location: LocationDto,
)

@Serializable
data class LocationDto(val locality: String, val country: String)

@Serializable
data class DriverDto(
    val driverId: String,
    val permanentNumber: String? = null,
    val code: String? = null,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String? = null,
    val nationality: String,
)

@Serializable
data class ConstructorDto(
    val constructorId: String,
    val name: String,
    val nationality: String,
)

@Serializable
data class DriverStandingDto(
    val position: String? = null,
    val positionText: String,
    val points: String,
    val wins: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructors") val constructors: List<ConstructorDto> = emptyList(),
)

@Serializable
data class ConstructorStandingDto(
    val position: String? = null,
    val positionText: String,
    val points: String,
    val wins: String,
    @SerialName("Constructor") val constructor: ConstructorDto,
)

@Serializable
data class ResultDto(
    val position: String,
    val positionText: String,
    val points: String,
    val grid: String = "0",
    val laps: String = "0",
    val status: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructor") val constructor: ConstructorDto,
    @SerialName("Time") val time: ResultTimeDto? = null,
    @SerialName("FastestLap") val fastestLap: FastestLapDto? = null,
)

@Serializable
data class ResultTimeDto(val time: String)

@Serializable
data class FastestLapDto(
    val rank: String? = null,
    @SerialName("Time") val time: ResultTimeDto? = null,
)
