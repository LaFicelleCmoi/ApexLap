package com.apexlap.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.theme.ApexColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class ChartSeries(val label: String, val color: Color, val values: List<Double>)

private val PlotStart = 36.dp
private val PlotEnd = 44.dp
private val PlotTop = 8.dp
private val PlotBottom = 22.dp

/**
 * Cumulative-points line chart. Tap or drag horizontally to inspect a round (crosshair + tooltip);
 * each line is direct-labelled at its end and a legend sits below, so identity never relies on colour alone.
 */
@Composable
fun ProgressionChart(
    rounds: List<Int>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
) {
    if (rounds.isEmpty() || series.isEmpty()) return
    val measurer = rememberTextMeasurer()
    var selected by remember(rounds, series) { mutableStateOf<Int?>(null) }
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = ApexColors.Telemetry)
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val leader = series.maxBy { it.values.last() }

    fun indexAt(x: Float, width: Float, density: Float): Int {
        val start = PlotStart.value * density
        val end = width - PlotEnd.value * density
        if (rounds.size == 1) return 0
        return (((x - start) / (end - start)) * (rounds.size - 1)).roundToInt().coerceIn(0, rounds.size - 1)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(rounds, series) {
                detectTapGestures { offset ->
                    val index = indexAt(offset.x, size.width.toFloat(), density)
                    selected = if (selected == index) null else index
                }
            }
            .pointerInput(rounds, series) {
                detectHorizontalDragGestures(onDragEnd = {}) { change, _ ->
                    selected = indexAt(change.position.x, size.width.toFloat(), density)
                }
            }
            .semantics {
                contentDescription = "Évolution des points sur ${rounds.size} manches. " +
                    "En tête : ${leader.label} avec ${leader.values.last().formatPoints()} points."
            },
    ) {
        val left = PlotStart.toPx()
        val right = size.width - PlotEnd.toPx()
        val top = PlotTop.toPx()
        val bottom = size.height - PlotBottom.toPx()
        val (maxY, step) = niceScale(series.maxOf { it.values.max() })
        fun x(i: Int) = if (rounds.size == 1) (left + right) / 2 else left + (right - left) * i / (rounds.size - 1)
        fun y(v: Double) = bottom - ((v / maxY) * (bottom - top)).toFloat()

        // Recessive hairline grid + axis labels.
        var tick = 0.0
        while (tick <= maxY + 1e-6) {
            val ty = y(tick)
            drawLine(ApexColors.Kerb, Offset(left, ty), Offset(right, ty), strokeWidth = 1f)
            drawLabel(measurer, tick.formatPoints(), axisStyle, Offset(left - 6.dp.toPx(), ty), alignEnd = true)
            tick += step
        }
        val labelEvery = ceil(rounds.size / 8.0).toInt().coerceAtLeast(1)
        rounds.forEachIndexed { i, round ->
            if (i % labelEvery == 0 || i == rounds.lastIndex) {
                drawLabel(measurer, "M$round", axisStyle, Offset(x(i), bottom + 12.dp.toPx()), center = true)
            }
        }

        // Lines, leader drawn last so it sits on top.
        series.sortedBy { it.values.last() }.forEach { s ->
            val path = Path()
            s.values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
            drawPath(path, s.color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        val markerIndex = selected ?: rounds.lastIndex
        series.forEach { s -> drawMarker(Offset(x(markerIndex), y(s.values[markerIndex])), s.color) }

        if (selected == null) {
            // Direct end labels, nudged apart so they never overlap.
            val minGap = 13.dp.toPx()
            val placed = series.map { it to y(it.values.last()) }.sortedBy { it.second }.toMutableList()
            for (i in 1 until placed.size) {
                if (placed[i].second - placed[i - 1].second < minGap) placed[i] = placed[i].first to placed[i - 1].second + minGap
            }
            val overflow = (placed.lastOrNull()?.second ?: 0f) - bottom
            placed.forEach { (s, ly) ->
                drawLabel(measurer, s.label, labelStyle, Offset(right + 8.dp.toPx(), ly - overflow.coerceAtLeast(0f)))
            }
        } else {
            drawTooltip(measurer, markerIndex, rounds[markerIndex], series, x(markerIndex), left, right, top, labelStyle, axisStyle)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(series: List<ChartSeries>, modifier: Modifier = Modifier, showValues: Boolean = true) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(s.color))
                Text(
                    if (showValues) "${s.label} ${s.values.last().formatPoints()}" else s.label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** Rounds the axis maximum up to 1/2/2.5/5 × 10ⁿ so there are about four gridlines. */
internal fun niceScale(max: Double, targetTicks: Int = 4): Pair<Double, Double> {
    if (max <= 0) return 10.0 to 5.0
    val rough = max / targetTicks
    val magnitude = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rough }
    return ceil(max / step) * step to step
}

private fun DrawScope.drawMarker(center: Offset, color: Color) {
    drawCircle(ApexColors.Carbon, radius = 6.dp.toPx(), center = center)
    drawCircle(color, radius = 4.dp.toPx(), center = center)
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    anchor: Offset,
    alignEnd: Boolean = false,
    center: Boolean = false,
) {
    val layout = measurer.measure(text, style)
    val dx = when {
        alignEnd -> -layout.size.width.toFloat()
        center -> -layout.size.width / 2f
        else -> 0f
    }
    drawText(layout, topLeft = Offset(anchor.x + dx, anchor.y - layout.size.height / 2f))
}

private fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    index: Int,
    round: Int,
    series: List<ChartSeries>,
    cx: Float,
    left: Float,
    right: Float,
    top: Float,
    labelStyle: TextStyle,
    titleStyle: TextStyle,
) {
    drawLine(ApexColors.Telemetry.copy(alpha = 0.6f), Offset(cx, top), Offset(cx, size.height - PlotBottom.toPx()), strokeWidth = 1f)
    val title = measurer.measure("APRÈS LA MANCHE $round", titleStyle)
    val rows = series.sortedByDescending { it.values[index] }
        .map { it to measurer.measure("${it.label}  ${it.values[index].formatPoints()} pts", labelStyle) }
    val pad = 8.dp.toPx()
    val swatch = 10.dp.toPx()
    val width = maxOf(title.size.width.toFloat(), rows.maxOf { it.second.size.width } + swatch + pad) + pad * 2
    val height = title.size.height + rows.sumOf { it.second.size.height } + pad * 2.5f
    val boxX = if (cx + 12.dp.toPx() + width <= right + PlotEnd.toPx()) cx + 12.dp.toPx() else maxOf(left, cx - 12.dp.toPx() - width)
    drawRoundRect(ApexColors.CarbonRaised, Offset(boxX, top), Size(width, height), CornerRadius(10.dp.toPx()))
    drawText(title, topLeft = Offset(boxX + pad, top + pad))
    var rowY = top + pad * 1.5f + title.size.height
    rows.forEach { (s, layout) ->
        drawRoundRect(s.color, Offset(boxX + pad, rowY + (layout.size.height - swatch) / 2), Size(swatch, swatch), CornerRadius(2.dp.toPx()))
        drawText(layout, topLeft = Offset(boxX + pad * 2 + swatch, rowY))
        rowY += layout.size.height
    }
}
