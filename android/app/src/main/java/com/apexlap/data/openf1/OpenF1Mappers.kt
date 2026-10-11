package com.apexlap.data.openf1

import com.apexlap.domain.live.Gap
import com.apexlap.domain.live.LiveDriver
import com.apexlap.domain.live.LiveSession
import com.apexlap.domain.live.Stint
import com.apexlap.domain.live.TimingEvent
import com.apexlap.domain.live.TyreCompound
import com.apexlap.domain.telemetry.CarSample
import com.apexlap.domain.telemetry.TrackPoint
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.OffsetDateTime

/** OpenF1 dates: ISO-8601 with offset, sometimes with microseconds, sometimes without fraction. */
internal fun parseOpenF1Date(value: String): Instant = OffsetDateTime.parse(value).toInstant()

fun OpenF1SessionDto.toDomain() = LiveSession(
    key = sessionKey,
    name = sessionName,
    type = sessionType,
    start = parseOpenF1Date(dateStart),
    end = parseOpenF1Date(dateEnd),
    circuit = circuitShortName,
    country = countryName,
)

fun OpenF1DriverDto.toDomain() = LiveDriver(
    number = driverNumber,
    acronym = acronym ?: "#$driverNumber",
    fullName = fullName ?: "Pilote $driverNumber",
    team = teamName.orEmpty(),
    teamColour = teamColour?.toLongOrNull(16),
)

internal fun JsonElement?.toGap(): Gap? = when {
    this == null || this is JsonNull -> null
    this is JsonPrimitive && doubleOrNull != null -> Gap.Seconds(doubleOrNull!!)
    this is JsonPrimitive -> Gap.Text(content)
    else -> null
}

fun OpenF1PositionDto.toEvent() = TimingEvent.Position(parseOpenF1Date(date), driverNumber, position)

fun OpenF1IntervalDto.toEvent() =
    TimingEvent.Interval(parseOpenF1Date(date), driverNumber, gapToLeader.toGap(), interval.toGap())

/** Stamped at lap completion (start + duration) so a replay reveals the time when it was set. */
fun OpenF1LapDto.toEvent(fallback: Instant): TimingEvent.Lap? {
    val start = dateStart?.let(::parseOpenF1Date) ?: return null
    val completedAt = lapDuration?.let { start.plusMillis((it * 1000).toLong()) } ?: start
    return TimingEvent.Lap(
        date = if (completedAt.isBefore(fallback)) fallback else completedAt,
        driver = driverNumber,
        lapNumber = lapNumber,
        duration = lapDuration,
        sectors = listOf(sector1, sector2, sector3),
        isPitOutLap = isPitOutLap,
    )
}

fun OpenF1StintDto.toEvent(date: Instant) = TimingEvent.StintUpdate(
    date = date,
    driver = driverNumber,
    stint = Stint(stintNumber, TyreCompound.of(compound), lapStart ?: 1, lapEnd, tyreAgeAtStart ?: 0),
)

fun OpenF1RaceControlDto.toEvent() =
    TimingEvent.RaceControl(parseOpenF1Date(date), category, flag, scope, message, lapNumber)

fun OpenF1PitDto.toEvent() = TimingEvent.PitStop(parseOpenF1Date(date), driverNumber, lapNumber, pitDuration)

fun OpenF1CarDataDto.toSample(): CarSample? {
    val speed = speed ?: return null
    return CarSample(parseOpenF1Date(date), speed.toDouble(), (throttle ?: 0).toDouble(), (brake ?: 0).toDouble(), gear ?: 0)
}

fun OpenF1LocationDto.toPoint() = TrackPoint(parseOpenF1Date(date), x.toDouble(), y.toDouble())
