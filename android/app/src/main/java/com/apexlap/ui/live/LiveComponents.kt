package com.apexlap.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apexlap.domain.live.Gap
import com.apexlap.domain.live.TrackStatus
import com.apexlap.domain.live.TyreCompound

/** Status colours are reserved for track state and always paired with an icon and a label. */
object StatusColors {
    val Good = Color(0xFF0CA30C)
    val Warning = Color(0xFFFAB219)
    val Critical = Color(0xFFD03B3B)
}

@Composable
fun TrackStatusPill(status: TrackStatus, modifier: Modifier = Modifier) {
    val (color, icon) = when (status) {
        TrackStatus.GREEN -> StatusColors.Good to Icons.Outlined.CheckCircle
        TrackStatus.YELLOW, TrackStatus.VSC, TrackStatus.SAFETY_CAR -> StatusColors.Warning to Icons.Outlined.Warning
        TrackStatus.RED -> StatusColors.Critical to Icons.Outlined.Report
        TrackStatus.CHEQUERED -> MaterialTheme.colorScheme.onSurface to Icons.Outlined.Flag
    }
    Row(
        modifier
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(status.label.uppercase(), style = MaterialTheme.typography.labelSmall)
    }
}

/** Official Pirelli compound colours, with the letter so identity never relies on colour alone. */
@Composable
fun TyreBadge(compound: TyreCompound, age: Int?, modifier: Modifier = Modifier) {
    val color = when (compound) {
        TyreCompound.SOFT -> Color(0xFFDA291C)
        TyreCompound.MEDIUM -> Color(0xFFFFD12E)
        TyreCompound.HARD -> Color(0xFFF0F0EC)
        TyreCompound.INTERMEDIATE -> Color(0xFF43B02A)
        TyreCompound.WET -> Color(0xFF0067AD)
        TyreCompound.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(20.dp).border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(compound.letter, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }
        if (age != null) {
            Text(
                "$age",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
    }
}

@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(StatusColors.Critical, CircleShape))
}

fun Gap?.display(): String = when (this) {
    null -> "—"
    is Gap.Seconds -> if (value == 0.0) "Leader" else "+" + String.format(java.util.Locale.US, "%.3f", value)
    is Gap.Text -> value.replace("LAPS", "tours").replace("LAP", "tour")
}
