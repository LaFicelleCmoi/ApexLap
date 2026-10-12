package com.apexlap.ui.components

import com.apexlap.i18n.AppLanguage
import com.apexlap.i18n.I18n
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formatters follow the app language (read at call time, so a language change applies on the next draw).
private fun formatter(pattern: String) = DateTimeFormatter.ofPattern(pattern, I18n.locale)

private fun dayFormatter() = formatter("EEE d MMM")
private fun shortDayFormatter() = formatter("d MMM")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

fun Instant.localDay(zone: ZoneId = ZoneId.systemDefault()): String =
    dayFormatter().format(atZone(zone)).replaceFirstChar { it.uppercase() }

fun Instant.localTime(zone: ZoneId = ZoneId.systemDefault()): String = timeFormatter.format(atZone(zone))

fun LocalDate.shortDay(): String = shortDayFormatter().format(this)

fun LocalDate.longDay(): String = dayFormatter().format(this).replaceFirstChar { it.uppercase() }

/** "d MMMM yyyy" in the app language, e.g. for birth dates. */
fun LocalDate.fullDate(): String =
    formatter(if (I18n.strings.language == AppLanguage.ES) "d 'de' MMMM 'de' yyyy" else "d MMMM yyyy").format(this)

fun Double.formatPoints(): String =
    if (this % 1.0 == 0.0) toLong().toString() else String.format(I18n.locale, "%.1f", this)

/** Converts an ISO country name to its flag emoji for the countries that host or race in F1. */
fun flagOf(countryOrNationality: String): String {
    val code = countryCodes[countryOrNationality] ?: return "🏁"
    return code.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')) }.joinToString("") { String(it) }
}

private val countryCodes = mapOf(
    "Australia" to "AU", "Australian" to "AU", "China" to "CN", "Chinese" to "CN", "Japan" to "JP",
    "Japanese" to "JP", "Bahrain" to "BH", "Saudi Arabia" to "SA", "USA" to "US", "United States" to "US",
    "American" to "US", "Italy" to "IT", "Italian" to "IT", "Monaco" to "MC", "Monegasque" to "MC",
    "Spain" to "ES", "Spanish" to "ES", "Canada" to "CA", "Canadian" to "CA", "Austria" to "AT",
    "Austrian" to "AT", "UK" to "GB", "United Kingdom" to "GB", "British" to "GB", "Belgium" to "BE",
    "Belgian" to "BE", "Hungary" to "HU", "Hungarian" to "HU", "Netherlands" to "NL", "Dutch" to "NL",
    "Azerbaijan" to "AZ", "Singapore" to "SG", "Mexico" to "MX", "Mexican" to "MX", "Brazil" to "BR",
    "Brazilian" to "BR", "Qatar" to "QA", "UAE" to "AE", "Abu Dhabi" to "AE", "France" to "FR",
    "French" to "FR", "Germany" to "DE", "German" to "DE", "Finland" to "FI", "Finnish" to "FI",
    "Thailand" to "TH", "Thai" to "TH", "Argentina" to "AR", "Argentine" to "AR", "Argentinian" to "AR",
    "New Zealand" to "NZ", "New Zealander" to "NZ", "Danish" to "DK", "Denmark" to "DK", "Swiss" to "CH",
    "Switzerland" to "CH", "Portugal" to "PT", "Polish" to "PL", "Russian" to "RU", "Swedish" to "SE",
)

/** "in 2 d 4 h", "in 3 h 20", "in 45 min", "now" — for the widget and reminders, in the app language. */
fun relativeStart(from: Instant, to: Instant): String {
    val strings = I18n.strings
    val minutes = java.time.Duration.between(from, to).toMinutes()
    return when {
        minutes <= 0 -> strings.now
        minutes < 60 -> strings.inMinutes(minutes)
        minutes < 24 * 60 -> strings.inHours(minutes / 60, minutes % 60)
        else -> strings.inDays(minutes / (24 * 60), (minutes / 60) % 24)
    }
}

/** 97.558 → "1:37.558". */
fun formatLapTime(seconds: Double): String {
    val minutes = (seconds / 60).toInt()
    val rest = seconds - minutes * 60
    return if (minutes > 0) String.format(Locale.US, "%d:%06.3f", minutes, rest) else String.format(Locale.US, "%.3f", rest)
}
