package com.apexlap.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexlap.ui.theme.ApexColors

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("RÉESSAYER") }
        }
    }
}

/** Shown on top of cached content when the last refresh failed. */
@Composable
fun OfflineBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            TextButton(onClick = onRetry) { Text("RÉESSAYER", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(16.dp).background(MaterialTheme.colorScheme.primary))
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Vertical team-colour stripe used at the start of every standings / results row. */
@Composable
fun TeamStripe(color: Color, modifier: Modifier = Modifier, height: Dp = 28.dp) {
    Box(modifier.width(4.dp).height(height).clip(RoundedCornerShape(2.dp)).background(color))
}

@Composable
fun PositionText(position: Int, modifier: Modifier = Modifier, label: String = position.toString()) {
    val color = when (position) {
        1 -> ApexColors.Gold
        2 -> ApexColors.Silver
        3 -> ApexColors.Bronze
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        label,
        modifier = modifier.width(32.dp),
        color = color,
        style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A stylised racing helmet drawn in the team colours (no licensed artwork needed). */
@Composable
fun Helmet(color: Color, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val shell = Path().apply {
            moveTo(w * 0.08f, h * 0.62f)
            cubicTo(w * 0.05f, h * 0.20f, w * 0.45f, h * 0.05f, w * 0.70f, h * 0.12f)
            cubicTo(w * 0.92f, h * 0.18f, w * 0.98f, h * 0.45f, w * 0.95f, h * 0.70f)
            lineTo(w * 0.90f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.88f)
            close()
        }
        drawPath(shell, Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)), Offset.Zero, Offset(w, h)))
        val visor = Path().apply {
            moveTo(w * 0.38f, h * 0.42f)
            lineTo(w * 0.94f, h * 0.42f)
            lineTo(w * 0.95f, h * 0.60f)
            lineTo(w * 0.45f, h * 0.60f)
            close()
        }
        drawPath(visor, Color(0xFF0B0B10))
        drawRect(Color.White.copy(alpha = 0.85f), topLeft = Offset(w * 0.10f, h * 0.66f), size = Size(w * 0.80f, h * 0.05f))
    }
}

/** World titles: count plus the seasons, most recent first. */
@Composable
fun TitlesRow(titles: com.apexlap.domain.encyclopedia.Titles, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        StatTile("Titre" + if (titles.count > 1) "s" else "", "🏆 ${titles.count}", ApexColors.Gold, Modifier.width(120.dp))
        Text(
            titles.seasons.sortedDescending().joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
}
