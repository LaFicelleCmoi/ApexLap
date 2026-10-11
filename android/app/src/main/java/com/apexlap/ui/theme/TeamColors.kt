package com.apexlap.ui.theme

import androidx.compose.ui.graphics.Color

object TeamColors {
    private val colors = mapOf(
        "mercedes" to Color(0xFF27F4D2),
        "ferrari" to Color(0xFFE8002D),
        "red_bull" to Color(0xFF3671C6),
        "mclaren" to Color(0xFFFF8000),
        "aston_martin" to Color(0xFF229971),
        "alpine" to Color(0xFF0093CC),
        "williams" to Color(0xFF64C4FF),
        "rb" to Color(0xFF6692FF),
        "audi" to Color(0xFFBB0A30),
        "sauber" to Color(0xFF52E252),
        "haas" to Color(0xFFB6BABD),
        "cadillac" to Color(0xFFC9A961),
    )

    fun of(constructorId: String?): Color = colors[constructorId] ?: ApexColors.Telemetry
}
