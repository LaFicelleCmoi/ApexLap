package com.apexlap.data.openf1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// OpenF1 returns flat JSON arrays with snake_case keys. https://openf1.org/docs

@Serializable
data class OpenF1SessionDto(
    @SerialName("session_key") val sessionKey: Int,
    @SerialName("meeting_key") val meetingKey: Int,
    @SerialName("session_name") val sessionName: String,
    @SerialName("session_type") val sessionType: String,
    @SerialName("date_start") val dateStart: String,
    @SerialName("date_end") val dateEnd: String,
    @SerialName("circuit_short_name") val circuitShortName: String,
    @SerialName("country_name") val countryName: String,
    val year: Int,
)

@Serializable
data class OpenF1DriverDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("name_acronym") val acronym: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("team_name") val teamName: String? = null,
    @SerialName("team_colour") val teamColour: String? = null,
)

@Serializable
data class OpenF1PositionDto(
    val date: String,
    @SerialName("driver_number") val driverNumber: Int,
    val position: Int,
)

/** `gap_to_leader` / `interval` are seconds, or strings such as "+1 LAP", or null. */
@Serializable
data class OpenF1IntervalDto(
    val date: String,
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("gap_to_leader") val gapToLeader: JsonElement? = null,
    val interval: JsonElement? = null,
)

@Serializable
data class OpenF1LapDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("lap_number") val lapNumber: Int,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("lap_duration") val lapDuration: Double? = null,
    @SerialName("duration_sector_1") val sector1: Double? = null,
    @SerialName("duration_sector_2") val sector2: Double? = null,
    @SerialName("duration_sector_3") val sector3: Double? = null,
    @SerialName("is_pit_out_lap") val isPitOutLap: Boolean = false,
    @SerialName("st_speed") val speedTrap: Int? = null,
)

@Serializable
data class OpenF1StintDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("stint_number") val stintNumber: Int,
    val compound: String? = null,
    @SerialName("lap_start") val lapStart: Int? = null,
    @SerialName("lap_end") val lapEnd: Int? = null,
    @SerialName("tyre_age_at_start") val tyreAgeAtStart: Int? = null,
)

@Serializable
data class OpenF1RaceControlDto(
    val date: String,
    val category: String? = null,
    val flag: String? = null,
    val scope: String? = null,
    val sector: Int? = null,
    val message: String,
    @SerialName("lap_number") val lapNumber: Int? = null,
    @SerialName("driver_number") val driverNumber: Int? = null,
)

@Serializable
data class OpenF1PitDto(
    val date: String,
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("lap_number") val lapNumber: Int? = null,
    @SerialName("pit_duration") val pitDuration: Double? = null,
)

@Serializable
data class OpenF1CarDataDto(
    val date: String,
    val speed: Int? = null,
    val throttle: Int? = null,
    val brake: Int? = null,
    @SerialName("n_gear") val gear: Int? = null,
    val rpm: Int? = null,
)

@Serializable
data class OpenF1LocationDto(
    val date: String,
    val x: Int,
    val y: Int,
)

@Serializable
data class OpenF1TokenDto(
    @SerialName("access_token") val accessToken: String,
    /** Seconds, sent as a string by the API. */
    @SerialName("expires_in") val expiresIn: JsonElement? = null,
)
