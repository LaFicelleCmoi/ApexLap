package com.apexlap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** "F1 night race" palette: asphalt blacks, floodlight whites, racing red. */
object ApexColors {
    val Asphalt = Color(0xFF0B0B10)
    val Carbon = Color(0xFF15151E)
    val CarbonRaised = Color(0xFF1F1F2B)
    val Kerb = Color(0xFF2C2C3A)
    val RacingRed = Color(0xFFE10600)
    val Floodlight = Color(0xFFF5F5F7)
    val Telemetry = Color(0xFF9A9AAD)
    val Gold = Color(0xFFFFD24C)
    val Silver = Color(0xFFC9CCD6)
    val Bronze = Color(0xFFCD7F4E)
    val PurpleSector = Color(0xFFB138DD)
    val GreenSector = Color(0xFF2ED573)
}

/**
 * Categorical series colours for charts, validated (lightness, chroma, colour-blind separation, 3:1 contrast)
 * against the Carbon card surface. Team colours are not used for series: several pairs are too close and
 * team-mates would share a colour.
 */
val ChartPalette = listOf(
    Color(0xFF3987E5),
    Color(0xFFD95926),
    Color(0xFF199E70),
    Color(0xFFC98500),
    Color(0xFFD55181),
)

private val ColorScheme = darkColorScheme(
    primary = ApexColors.RacingRed,
    onPrimary = Color.White,
    secondary = ApexColors.Floodlight,
    onSecondary = ApexColors.Asphalt,
    tertiary = ApexColors.PurpleSector,
    background = ApexColors.Asphalt,
    onBackground = ApexColors.Floodlight,
    surface = ApexColors.Carbon,
    onSurface = ApexColors.Floodlight,
    surfaceVariant = ApexColors.CarbonRaised,
    onSurfaceVariant = ApexColors.Telemetry,
    surfaceContainer = ApexColors.Carbon,
    surfaceContainerHigh = ApexColors.CarbonRaised,
    outline = ApexColors.Kerb,
    error = Color(0xFFFF5A5F),
)

private val Racing = FontFamily.SansSerif

private val ApexTypography = Typography(
    displayLarge = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 48.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Racing, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = Racing, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Racing, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Racing, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Racing, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Racing, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Racing, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp),
)

@Composable
fun ApexLapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = ApexTypography, content = content)
}
