package com.apexlap.ui.results

import com.apexlap.i18n.LocalStrings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.RaceResult
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.PositionText
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.shortDay
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(onRaceClick: (Race) -> Unit, viewModel: ResultsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val races = state.races

    PullToRefreshBox(isRefreshing = state.isRefreshing && !races.isNullOrEmpty(), onRefresh = { viewModel.refresh(force = true) }) {
        when {
            races.isNullOrEmpty() && state.isRefreshing -> LoadingState()
            races.isNullOrEmpty() -> EmptyState(
                state.error ?: strings.noFinishedGrandPrix(state.season),
                onRetry = { viewModel.refresh(force = true) },
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Text(
                        strings.resultsTitle(state.season),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.statusBarsPadding().padding(16.dp),
                    )
                }
                state.error?.let { item { OfflineBanner(it, onRetry = { viewModel.refresh(force = true) }) } }
                items(races, key = { it.race.round }) { summary ->
                    RaceSummaryCard(summary, onClick = { onRaceClick(summary.race) })
                }
            }
        }
    }
}

@Composable
private fun RaceSummaryCard(summary: RaceSummary, onClick: () -> Unit) {
    val race = summary.race
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(flagOf(race.circuit.country), style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(race.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        LocalStrings.current.round(race.round) + (race.raceSession?.date?.let { " · ${it.shortDay()}" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            summary.podium.forEach { PodiumRow(it) }
            summary.sprintWinner?.let { winner ->
                Text(
                    "SPRINT · ${winner.driver.fullName} (${winner.constructor.name})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PodiumRow(result: RaceResult) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PositionText(result.position)
        TeamStripe(TeamColors.of(result.constructor.id), height = 20.dp)
        Text(result.driver.fullName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (result.hasFastestLap) {
            Text("⏱", color = ApexColors.PurpleSector, modifier = Modifier.width(20.dp))
        }
        Text(
            if (result.position == 1) result.time.orEmpty() else result.time ?: result.status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
