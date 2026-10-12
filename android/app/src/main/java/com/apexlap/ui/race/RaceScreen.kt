package com.apexlap.ui.race

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.encyclopedia.GrandPrixContext
import com.apexlap.domain.encyclopedia.GrandPrixFacts
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.Session
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.PositionText
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.ui.components.longDay
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors

private enum class WeekendTab(val label: String) { RACE("COURSE"), QUALIFYING("QUALIFS"), SPRINT("SPRINT") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceScreen(
    onBack: () -> Unit,
    onDriverClick: (String) -> Unit,
    onCircuitClick: (String) -> Unit,
    viewModel: RaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val race = state.race
    val tabs = if (race?.isSprintWeekend == true) WeekendTab.entries else listOf(WeekendTab.RACE, WeekendTab.QUALIFYING)
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    // Until the user picks a tab, show the most advanced session that has results.
    val selected = selectedName?.let(WeekendTab::valueOf) ?: when {
        state.results.race.isNotEmpty() -> WeekendTab.RACE
        state.results.sprint.isNotEmpty() -> WeekendTab.SPRINT
        state.results.qualifying.isNotEmpty() -> WeekendTab.QUALIFYING
        else -> WeekendTab.RACE
    }

    DetailScaffold(title = race?.name ?: "Grand Prix", onBack = onBack) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (race != null) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("MANCHE ${race.round} · ${race.season}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text("${flagOf(race.circuit.country)}  ${race.circuit.name}", style = MaterialTheme.typography.headlineSmall)
                            Text("${race.circuit.locality}, ${race.circuit.country}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val facts = state.facts
                    if (!facts.isEmpty) {
                        item { FactsCard(facts, onDriverClick) }
                    }
                    item { SectionHeader("Contexte") }
                    item { ContextBlock(state.context, onDriverClick) { onCircuitClick(race.circuit.id) } }
                    item { SectionHeader("Programme (heure locale)") }
                    items(race.sessions, key = { it.type }) { SessionRow(it) }
                }
                state.error?.let { item { OfflineBanner(it, viewModel::refresh) } }

                if (state.hasStarted || state.results != WeekendResults()) {
                    item {
                        PrimaryTabRow(
                            selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
                            containerColor = MaterialTheme.colorScheme.background,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            tabs.forEach { tab ->
                                Tab(selected = tab == selected, onClick = { selectedName = tab.name }, text = { Text(tab.label) })
                            }
                        }
                    }
                    when (selected) {
                        WeekendTab.RACE -> results(state.results.race, state.isRefreshing, onDriverClick)
                        WeekendTab.SPRINT -> results(state.results.sprint, state.isRefreshing, onDriverClick)
                        WeekendTab.QUALIFYING -> qualifying(state.results.qualifying, state.isRefreshing, onDriverClick)
                    }
                } else if (race != null) {
                    item {
                        Text(
                            "Les résultats apparaîtront ici dès le début du week-end.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.results(
    rows: List<RaceResult>,
    isRefreshing: Boolean,
    onDriverClick: (String) -> Unit,
) {
    if (rows.isEmpty()) {
        item { EmptySession(isRefreshing) }
    } else {
        items(rows, key = { "r-" + it.driver.id }) { result -> ResultRow(result, onClick = { onDriverClick(result.driver.id) }) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.qualifying(
    rows: List<QualifyingResult>,
    isRefreshing: Boolean,
    onDriverClick: (String) -> Unit,
) {
    if (rows.isEmpty()) {
        item { EmptySession(isRefreshing) }
    } else {
        items(rows, key = { "q-" + it.driver.id }) { result -> QualifyingRow(result, onClick = { onDriverClick(result.driver.id) }) }
    }
}

@Composable
private fun EmptySession(isRefreshing: Boolean) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        if (isRefreshing) {
            CircularProgressIndicator()
        } else {
            Text("Pas encore de résultats pour cette séance.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
            Text(session.type.shortLabel, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(48.dp).padding(vertical = 6.dp), textAlign = TextAlign.Center)
        }
        Text(session.type.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(session.startsAt?.localDay() ?: session.date.longDay(), style = MaterialTheme.typography.bodySmall)
            Text(session.startsAt?.localTime() ?: "Horaire à venir", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ResultRow(result: RaceResult, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PositionText(result.position, label = result.positionText)
        TeamStripe(TeamColors.of(result.constructor.id))
        Column(Modifier.weight(1f)) {
            Text(result.driver.fullName, style = MaterialTheme.typography.titleSmall)
            Text(result.constructor.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(result.time ?: result.status, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (result.hasFastestLap) {
                    Text("⏱ MT", style = MaterialTheme.typography.labelSmall, color = ApexColors.PurpleSector)
                }
                result.positionsGained?.takeIf { it != 0 }?.let { gained ->
                    Text(
                        if (gained > 0) "▲$gained" else "▼${-gained}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gained > 0) ApexColors.GreenSector else MaterialTheme.colorScheme.error,
                    )
                }
                Text("+${result.points.formatPoints()} pts", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun QualifyingRow(result: QualifyingResult, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PositionText(result.position)
        TeamStripe(TeamColors.of(result.constructor.id))
        Column(Modifier.weight(1f)) {
            Text(result.driver.fullName, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(result.q1?.let { "Q1 $it" }, result.q2?.let { "Q2 $it" }).joinToString("  ·  ")
                    .ifEmpty { result.constructor.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                result.bestTime ?: "—",
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                color = if (result.position == 1) ApexColors.PurpleSector else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                result.eliminatedIn?.let { "ÉLIMINÉ EN $it" } ?: "Q3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FactsCard(facts: GrandPrixFacts, onDriverClick: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(vertical = 8.dp)) {
            facts.winner?.let { FactRow("🏆 Vainqueur", it.driver.fullName, it.constructor.name, ApexColors.Gold) { onDriverClick(it.driver.id) } }
            facts.pole?.let { FactRow("Pole position", it.fullName, facts.poleTime.orEmpty(), ApexColors.PurpleSector) { onDriverClick(it.id) } }
            facts.fastestLap?.let {
                FactRow("⏱ Meilleur tour", it.driver.fullName, it.fastestLapTime.orEmpty(), ApexColors.PurpleSector) { onDriverClick(it.driver.id) }
            }
        }
    }
}

@Composable
private fun FactRow(label: String, name: String, detail: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.width(120.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(detail, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ContextBlock(context: GrandPrixContext?, onDriverClick: (String) -> Unit, onCircuitClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        context?.let {
            Text(
                if (it.edition == 1) "Premier Grand Prix disputé sur ce circuit." else "${it.edition}e Grand Prix disputé sur ce circuit.",
                style = MaterialTheme.typography.bodyMedium,
            )
            it.previousWinner?.let { win ->
                Text(
                    "Vainqueur précédent : ${win.driver.fullName} (${win.constructor.name}, ${win.season})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onDriverClick(win.driver.id) },
                )
            }
            it.mostWins?.let { (driver, wins) ->
                Text(
                    "Recordman des victoires ici : ${driver.fullName} ($wins)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onDriverClick(driver.id) },
                )
            }
        }
        Text(
            "Fiche du circuit ›",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onCircuitClick).padding(vertical = 8.dp),
        )
    }
}
