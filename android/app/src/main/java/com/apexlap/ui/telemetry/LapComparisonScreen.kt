package com.apexlap.ui.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.live.LiveDriver
import com.apexlap.domain.telemetry.LapComparison
import com.apexlap.domain.telemetry.LapRef
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.formatLapTime
import com.apexlap.ui.components.localDay
import com.apexlap.ui.live.frenchSessionName
import com.apexlap.ui.stats.ChartLegend
import com.apexlap.ui.stats.ChartSeries
import com.apexlap.ui.theme.ChartPalette
import java.util.Locale

@Composable
fun LapComparisonScreen(onBack: () -> Unit, viewModel: LapComparisonViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val comparison = state.comparison
    val colors = listOf(ChartPalette[0], ChartPalette[1])
    val driverA = state.driver(state.lapA?.driver)
    val driverB = state.driver(state.lapB?.driver)

    DetailScaffold(title = "Comparateur de tours", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { OfflineBanner(it, onRetry = viewModel::retry) }

            Picker(
                label = state.session?.let { "${it.circuit} · ${frenchSessionName(it.name)} — ${it.start.localDay()}" } ?: "Séance",
                options = state.sessions.take(40).map { it.key to "${it.circuit} · ${frenchSessionName(it.name)} — ${it.start.localDay()}" },
                onSelect = { viewModel.selectSession(it); selected = null },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LapSelector(0, colors[0], state.drivers, driverA, state.lapA, state.laps, viewModel, Modifier.weight(1f))
                LapSelector(1, colors[1], state.drivers, driverB, state.lapB, state.laps, viewModel, Modifier.weight(1f))
            }

            if (comparison == null) {
                if (!state.isLoading && state.sessions.isEmpty()) EmptyState(state.error ?: "Aucune séance disponible", onRetry = viewModel::retry)
                return@Column
            }
            val codeA = driverA?.acronym ?: "A"
            val codeB = driverB?.acronym ?: "B"

            SectionHeader("Temps au tour")
            SectorTable(state.lapA, state.lapB, codeA, codeB, colors)

            SectionHeader("Lecture")
            Readout(comparison, selected, codeA, codeB, colors)

            ChartCard("Vitesse (km/h)") {
                TraceChart(
                    comparison.distance,
                    listOf(ChartSeries(codeA, colors[0], comparison.speedA), ChartSeries(codeB, colors[1], comparison.speedB)),
                    selected, { selected = it },
                    yMin = 50.0, yMax = 360.0, yTicks = listOf(100.0, 200.0, 300.0), yLabel = { "${it.toInt()}" },
                    description = "Vitesse de $codeA et $codeB sur le tour",
                    height = 170.dp,
                )
                Spacer(Modifier.height(6.dp))
                ChartLegend(
                    listOf(ChartSeries(codeA, colors[0], emptyList()), ChartSeries(codeB, colors[1], emptyList())),
                    showValues = false,
                )
            }
            ChartCard("Écart cumulé (s) — au-dessus de 0 : $codeA devant") {
                val maxDelta = maxOf(0.2, comparison.delta.maxOf { kotlin.math.abs(it) })
                val bound = kotlin.math.ceil(maxDelta * 10) / 10
                TraceChart(
                    comparison.distance,
                    listOf(ChartSeries("Δ", MaterialTheme.colorScheme.onSurface, comparison.delta)),
                    selected, { selected = it },
                    yMin = -bound, yMax = bound, yTicks = listOf(-bound, 0.0, bound),
                    yLabel = { String.format(Locale.US, "%+.1f", it) },
                    description = "Écart de temps cumulé entre $codeA et $codeB",
                    zeroLine = true,
                )
            }
            ChartCard("Accélérateur (%)") {
                TraceChart(
                    comparison.distance,
                    listOf(ChartSeries(codeA, colors[0], comparison.throttleA), ChartSeries(codeB, colors[1], comparison.throttleB)),
                    selected, { selected = it },
                    yMin = 0.0, yMax = 100.0, yTicks = listOf(0.0, 50.0, 100.0), yLabel = { "${it.toInt()}" },
                    description = "Pression sur l'accélérateur",
                    height = 110.dp,
                )
            }
            ChartCard("Freinage") {
                TraceChart(
                    comparison.distance,
                    listOf(ChartSeries(codeA, colors[0], comparison.brakeA), ChartSeries(codeB, colors[1], comparison.brakeB.map { it * 0.85 })),
                    selected, { selected = it },
                    yMin = 0.0, yMax = 100.0, yTicks = listOf(0.0, 100.0), yLabel = { if (it > 0) "ON" else "OFF" },
                    description = "Phases de freinage",
                    height = 80.dp,
                )
            }

            SectionHeader("Domination par mini-secteur")
            ChartCard(null) {
                DominanceMap(
                    state.track, comparison.miniSectorWinners, colors,
                    selectedFraction = selected?.let { (comparison.distance[it] / comparison.distance.last()).toFloat() },
                )
                val winsA = comparison.miniSectorWinners.count { it == 0 }
                ChartLegend(
                    listOf(
                        ChartSeries("$codeA · $winsA mini-secteurs", colors[0], listOf()),
                        ChartSeries("$codeB · ${comparison.miniSectorWinners.size - winsA} mini-secteurs", colors[1], listOf()),
                    ),
                    showValues = false,
                )
            }
            Text(
                "Distance intégrée depuis la vitesse (OpenF1 ne fournit pas de canal distance) : les positions sont " +
                    "précises à quelques mètres près. Freinage de $codeB légèrement décalé pour rester lisible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ChartCard(title: String?, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
private fun Readout(comparison: LapComparison, selected: Int?, codeA: String, codeB: String, colors: List<Color>) {
    val i = selected ?: run {
        Text(
            "Touchez ou glissez sur un graphique pour lire les valeurs à un point du tour.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        return
    }
    val delta = comparison.delta[i]
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${comparison.distance[i].toInt()} m", style = MaterialTheme.typography.titleSmall)
        ReadoutValue(codeA, "${comparison.speedA[i].toInt()} km/h", colors[0])
        ReadoutValue(codeB, "${comparison.speedB[i].toInt()} km/h", colors[1])
        Text(
            String.format(Locale.US, "Δ %+.3f s", delta),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun ReadoutValue(code: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(" $code $value", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectorTable(a: LapRef?, b: LapRef?, codeA: String, codeB: String, colors: List<Color>) {
    if (a == null || b == null) return
    val columns = listOf("S1", "S2", "S3", "TOUR")
    val valuesA = a.sectors + a.duration
    val valuesB = b.sectors + b.duration
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Box(Modifier.width(64.dp))
            columns.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        }
        listOf(Triple(codeA + " T${a.lapNumber}", valuesA, valuesB), Triple(codeB + " T${b.lapNumber}", valuesB, valuesA)).forEachIndexed { row, (label, mine, other) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.width(64.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(16.dp).background(colors[row], RoundedCornerShape(2.dp)))
                    Text(" $label", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                mine.forEachIndexed { i, value ->
                    val better = value != null && other[i] != null && value < other[i]!!
                    Text(
                        value?.let { if (i == 3) formatLapTime(it) else String.format(Locale.US, "%.3f", it) } ?: "—",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (better) FontWeight.Black else FontWeight.Normal,
                        ),
                        color = if (better) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LapSelector(
    slot: Int,
    color: Color,
    drivers: List<LiveDriver>,
    driver: LiveDriver?,
    lap: LapRef?,
    laps: Map<Int, List<LapRef>>,
    viewModel: LapComparisonViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Picker(
            label = driver?.let { "${it.acronym} · ${it.team}" } ?: "Pilote",
            options = drivers.map { it.number to "${it.acronym} — ${it.fullName}" },
            onSelect = { viewModel.selectDriver(slot, it) },
            accent = color,
        )
        val driverLaps = driver?.let { laps[it.number] }.orEmpty()
        val best = driverLaps.minByOrNull { it.duration }
        Picker(
            label = lap?.let { "T${it.lapNumber} · ${formatLapTime(it.duration)}" + if (it == best) " ★" else "" } ?: "Tour",
            options = driverLaps.sortedBy { it.duration }.take(30).map {
                it.lapNumber to "Tour ${it.lapNumber} · ${formatLapTime(it.duration)}" + if (it == best) " (meilleur)" else ""
            },
            onSelect = { number -> driverLaps.firstOrNull { it.lapNumber == number }?.let { viewModel.selectLap(slot, it) } },
            accent = color,
        )
    }
}

@Composable
private fun Picker(
    label: String,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = options.isNotEmpty()) { expanded = true }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (accent != null) Box(Modifier.width(4.dp).height(18.dp).background(accent, RoundedCornerShape(2.dp)))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = if (accent != null) 8.dp else 0.dp),
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(key) })
            }
        }
    }
}
