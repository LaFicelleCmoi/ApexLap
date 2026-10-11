package com.apexlap.ui.telemetry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexlap.ui.stats.ChartSeries
import com.apexlap.ui.theme.ApexColors
import kotlin.math.roundToInt

private val AxisWidth = 40.dp

/**
 * Telemetry trace against lap distance. All traces of the comparison share [selected] (a distance index),
 * so the crosshair moves in sync across speed, throttle, brake and delta.
 */
@Composable
fun TraceChart(
    distance: List<Double>,
    series: List<ChartSeries>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    yMin: Double,
    yMax: Double,
    yTicks: List<Double>,
    yLabel: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    zeroLine: Boolean = false,
) {
    if (distance.size < 2 || series.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = ApexColors.Telemetry)

    fun indexAt(x: Float, width: Float, density: Float): Int {
        val left = AxisWidth.value * density
        return (((x - left) / (width - left)) * (distance.size - 1)).roundToInt().coerceIn(0, distance.size - 1)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(distance) {
                detectTapGestures { onSelect(indexAt(it.x, size.width.toFloat(), density)) }
            }
            .pointerInput(distance) {
                detectHorizontalDragGestures { change, _ -> onSelect(indexAt(change.position.x, size.width.toFloat(), density)) }
            }
            .semantics { contentDescription = description },
    ) {
        val left = AxisWidth.toPx()
        val top = 4.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        fun x(i: Int) = left + (size.width - left) * i / (distance.size - 1)
        fun y(v: Double) = bottom - ((v - yMin) / (yMax - yMin) * (bottom - top)).toFloat()

        yTicks.forEach { tick ->
            val ty = y(tick)
            val isZero = zeroLine && tick == 0.0
            drawLine(if (isZero) ApexColors.Telemetry else ApexColors.Kerb, Offset(left, ty), Offset(size.width, ty), strokeWidth = 1f)
            val label = measurer.measure(yLabel(tick), axisStyle)
            drawText(label, topLeft = Offset(left - 6.dp.toPx() - label.size.width, ty - label.size.height / 2f))
        }
        series.forEach { s ->
            val path = Path()
            s.values.forEachIndexed { i, v ->
                val py = y(v.coerceIn(yMin, yMax))
                if (i == 0) path.moveTo(x(i), py) else path.lineTo(x(i), py)
            }
            drawPath(path, s.color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        selected?.let { i ->
            drawLine(ApexColors.Floodlight.copy(alpha = 0.6f), Offset(x(i), top), Offset(x(i), bottom), strokeWidth = 1f)
            series.forEach { s ->
                val center = Offset(x(i), y(s.values[i].coerceIn(yMin, yMax)))
                drawCircle(ApexColors.Carbon, 6.dp.toPx(), center)
                drawCircle(s.color, 4.dp.toPx(), center)
            }
        }
    }
}

/** Track outline from driver A's location samples, coloured by who was quicker in each mini-sector. */
@Composable
fun DominanceMap(
    points: List<com.apexlap.domain.telemetry.MapTrackUseCase.MappedPoint>,
    winners: List<Int>,
    colors: List<Color>,
    selectedFraction: Float?,
    modifier: Modifier = Modifier,
) {
    if (points.size < 10 || winners.isEmpty()) return
    Canvas(modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = "Carte du circuit colorée par pilote le plus rapide par mini-secteur" }) {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val pad = 12.dp.toPx()
        val scale = minOf((size.width - 2 * pad) / (maxX - minX), (size.height - 2 * pad) / (maxY - minY)).toFloat()
        val offsetX = (size.width - (maxX - minX).toFloat() * scale) / 2
        val offsetY = (size.height - (maxY - minY).toFloat() * scale) / 2
        // Track y grows upwards; screen y grows downwards.
        fun at(p: com.apexlap.domain.telemetry.MapTrackUseCase.MappedPoint) =
            Offset(offsetX + (p.x - minX).toFloat() * scale, size.height - offsetY - (p.y - minY).toFloat() * scale)

        for (i in 1 until points.size) {
            val segment = (points[i].fraction * winners.size).toInt().coerceIn(0, winners.size - 1)
            drawLine(colors[winners[segment]], at(points[i - 1]), at(points[i]), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(ApexColors.Floodlight, 5.dp.toPx(), at(points.first()))
        selectedFraction?.let { f ->
            val p = points.minBy { kotlin.math.abs(it.fraction - f) }
            drawCircle(ApexColors.Carbon, 8.dp.toPx(), at(p))
            drawCircle(ApexColors.Floodlight, 5.dp.toPx(), at(p))
        }
    }
}
