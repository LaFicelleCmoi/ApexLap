package com.apexlap.ui.stats

import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.Settings
import com.apexlap.domain.usecase.grandPrixName
import com.apexlap.i18n.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.usecase.DriverProgression
import com.apexlap.domain.usecase.DuelSide
import com.apexlap.domain.usecase.HeadToHead
import com.apexlap.domain.encyclopedia.matchesSearch
import com.apexlap.domain.usecase.DriverRetirements
import com.apexlap.domain.usecase.LeaderBoard
import com.apexlap.domain.usecase.SeasonRetirements
import com.apexlap.domain.usecase.SeasonRetirementsRecord
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.theme.ChartPalette
import com.apexlap.ui.theme.TeamColors

private const val CHART_DRIVERS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onDriverClick: (String) -> Unit,
    onCompareLaps: () -> Unit,
    onEncyclopediaClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val progression = state.progression

    PullToRefreshBox(isRefreshing = state.isRefreshing && progression != null, onRefresh = { viewModel.refresh(force = true) }) {
        when {
            progression == null && state.isRefreshing -> LoadingState()
            progression == null -> EmptyState(
                state.error ?: strings.noRaceYet(state.season),
                onRetry = { viewModel.refresh(force = true) },
            )
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.statusBarsPadding().padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings.statsTitle(state.season), style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = strings.settings)
                    }
                }
                state.error?.let { OfflineBanner(it, onRetry = { viewModel.refresh(force = true) }) }

                SectionHeader(strings.championshipProgression)
                StatsCard {
                    val top = progression.drivers.take(CHART_DRIVERS)
                    val series = top.mapIndexed { i, d -> ChartSeries(d.driver.code, ChartPalette[i], d.cumulativePoints) }
                    Text(
                        strings.cumulativePointsHint(top.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    ProgressionChart(progression.rounds, series)
                    Spacer(Modifier.height(8.dp))
                    ChartLegend(series)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clickable(onClick = onCompareLaps),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(strings.lapComparison, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.lapComparisonHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onEncyclopediaClick),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(strings.encyclopedia, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.encyclopediaHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                SectionHeader(strings.seasonLeaders)
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.leaders.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { LeaderCard(it, onDriverClick, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                SectionHeader(strings.duel)
                StatsCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        DriverPicker(state.drivers, state.firstId, ChartPalette[0], viewModel::selectFirst, Modifier.weight(1f))
                        Text("VS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DriverPicker(state.drivers, state.secondId, ChartPalette[1], viewModel::selectSecond, Modifier.weight(1f))
                    }
                    state.duel?.let { duel ->
                        Spacer(Modifier.height(16.dp))
                        DuelTable(duel)
                        val pair = listOf(duel.first.driver.id, duel.second.driver.id)
                            .mapNotNull { id -> progression.drivers.firstOrNull { it.driver.id == id } }
                        if (pair.size == 2) {
                            Spacer(Modifier.height(16.dp))
                            val series = pair.mapIndexed { i, d -> ChartSeries(d.driver.code, ChartPalette[i], d.cumulativePoints) }
                            ProgressionChart(progression.rounds, series, height = 180.dp)
                            Spacer(Modifier.height(8.dp))
                            ChartLegend(series)
                        }
                    } ?: Text(
                        strings.pickTwoDrivers,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                SeasonRetirementsRecord.of(state.season)?.let { record ->
                    SectionHeader("${strings.retirements} · ${record.total}")
                    StatsCard {
                        RetirementsTable(record) { family ->
                            // The record names drivers by family name: resolve the id from this season's results.
                            state.drivers.firstOrNull { it.driver.familyName.matchesSearch(family) }?.let { onDriverClick(it.driver.id) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RetirementsTable(record: SeasonRetirements, onDriverClick: (String) -> Unit) {
    val strings = LocalStrings.current
    val max = record.drivers.maxOfOrNull { it.count } ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        record.drivers.forEach { RetirementRow(it, max) { onDriverClick(it.driver) } }
        if (record.noRetirement.isNotEmpty()) {
            Text(
                strings.noRetirement(record.noRetirement.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (record.nonStarters.isNotEmpty()) {
            Text(
                strings.nonStarters + record.nonStarters.joinToString(", ") { starts ->
                    "${starts.driver} (${starts.grandsPrix.joinToString(", ") { grandPrixName(it, strings.language) }})"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            strings.retirementLegend,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RetirementRow(entry: DriverRetirements, max: Int, onClick: () -> Unit) {
    val language = LocalStrings.current.language
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.driver, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(96.dp))
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(entry.count / max.toFloat())
                        .clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.error),
                )
            }
            Text(
                entry.count.toString(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.width(32.dp),
            )
        }
        Text(
            entry.retirements.joinToString(", ") { it.label(language) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun LeaderCard(board: LeaderBoard, onDriverClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {
            Text(LocalStrings.current.leader(board.category).uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (board.entries.isEmpty()) {
                Text("—", style = MaterialTheme.typography.titleMedium)
            }
            board.entries.forEachIndexed { i, entry ->
                Row(
                    Modifier.fillMaxWidth().clickable { onDriverClick(entry.driver.id) }.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TeamStripe(TeamColors.of(entry.constructor.id), height = 16.dp)
                    Text(
                        entry.driver.code,
                        style = if (i == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    Text(
                        entry.value.toString(),
                        style = if (i == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverPicker(
    drivers: List<DriverProgression>,
    selectedId: String?,
    color: Color,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = drivers.firstOrNull { it.driver.id == selectedId }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Text(
                selected?.driver?.familyName ?: LocalStrings.current.driver,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = LocalStrings.current.pickDriver)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            drivers.forEach { d ->
                DropdownMenuItem(
                    text = { Text("${d.driver.fullName} · ${d.total.formatPoints()} ${LocalStrings.current.pts}") },
                    leadingIcon = { TeamStripe(TeamColors.of(d.constructor.id), height = 18.dp) },
                    onClick = {
                        expanded = false
                        onSelect(d.driver.id)
                    },
                )
            }
        }
    }
}

private data class DuelMetric(
    val label: String,
    val first: Double?,
    val second: Double?,
    val format: (Double) -> String = { it.toInt().toString() },
    val lowerIsBetter: Boolean = false,
    val showBars: Boolean = true,
)

@Composable
private fun DuelTable(duel: HeadToHead) {
    val a = duel.first
    val b = duel.second
    val strings = LocalStrings.current
    val metrics = listOf(
        DuelMetric(strings.statPoints, a.points, b.points, format = Double::formatPoints),
        DuelMetric(strings.aheadInRace, a.racesAhead.toDouble(), b.racesAhead.toDouble()),
        DuelMetric(strings.aheadOnGrid, a.startsAhead.toDouble(), b.startsAhead.toDouble()),
        DuelMetric(strings.statWins, a.wins.toDouble(), b.wins.toDouble()),
        DuelMetric(strings.statPodiums, a.podiums.toDouble(), b.podiums.toDouble()),
        DuelMetric(strings.statPolesShort, a.poles.toDouble(), b.poles.toDouble()),
        DuelMetric(strings.bestFinish, a.bestFinish?.toDouble(), b.bestFinish?.toDouble(),
            format = { "P${it.toInt()}" }, lowerIsBetter = true, showBars = false),
        DuelMetric(strings.averageFinish, a.averageFinish, b.averageFinish,
            format = { String.format(strings.language.locale, "%.1f", it) }, lowerIsBetter = true, showBars = false),
        DuelMetric(strings.retirements, a.dnfs.toDouble(), b.dnfs.toDouble(), lowerIsBetter = true),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DuelName(a, Modifier.weight(1f), TextAlign.Start)
            Text(strings.sharedRaces(duel.sharedRaces), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            DuelName(b, Modifier.weight(1f), TextAlign.End)
        }
        metrics.forEach { DuelRow(it) }
    }
}

@Composable
private fun DuelName(side: DuelSide, modifier: Modifier, align: TextAlign) {
    Column(modifier, horizontalAlignment = if (align == TextAlign.Start) Alignment.Start else Alignment.End) {
        Text(side.driver.code, style = MaterialTheme.typography.headlineSmall)
        Text(side.constructor.name, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DuelRow(metric: DuelMetric) {
    val a = metric.first
    val b = metric.second
    val firstBetter = a != null && (b == null || if (metric.lowerIsBetter) a < b else a > b)
    val secondBetter = b != null && (a == null || if (metric.lowerIsBetter) b < a else b > a)
    val max = maxOf(a ?: 0.0, b ?: 0.0)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DuelValue(a?.let(metric.format) ?: "—", firstBetter, Modifier.weight(1f), TextAlign.Start)
            Text(metric.label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            DuelValue(b?.let(metric.format) ?: "—", secondBetter, Modifier.weight(1f), TextAlign.End)
        }
        if (metric.showBars && max > 0) {
            Row(Modifier.fillMaxWidth().height(6.dp).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DuelBar(((a ?: 0.0) / max).toFloat(), ChartPalette[0], fromEnd = true, Modifier.weight(1f))
                DuelBar(((b ?: 0.0) / max).toFloat(), ChartPalette[1], fromEnd = false, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DuelValue(text: String, better: Boolean, modifier: Modifier, align: TextAlign) {
    Text(
        text,
        modifier = modifier,
        textAlign = align,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (better) FontWeight.Black else FontWeight.Normal),
        color = if (better) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Bars grow outward from the centre line: the first driver's to the left, the second's to the right. */
@Composable
private fun DuelBar(fraction: Float, color: Color, fromEnd: Boolean, modifier: Modifier) {
    Box(modifier.fillMaxHeight(), contentAlignment = if (fromEnd) Alignment.CenterEnd else Alignment.CenterStart) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}
