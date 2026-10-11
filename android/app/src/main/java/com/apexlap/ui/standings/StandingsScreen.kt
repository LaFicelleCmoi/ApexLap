package com.apexlap.ui.standings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.PositionText
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.theme.TeamColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    onDriverClick: (String) -> Unit,
    onConstructorClick: (String) -> Unit,
    viewModel: StandingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "CHAMPIONNAT ${state.season}",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Rechercher un pilote, une écurie…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("PILOTES") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("CONSTRUCTEURS") })
        }
        state.error?.let { OfflineBanner(it, viewModel::refresh) }

        val drivers = state.drivers
        PullToRefreshBox(isRefreshing = state.isRefreshing && !drivers.isNullOrEmpty(), onRefresh = viewModel::refresh) {
            when {
                drivers.isNullOrEmpty() && state.isRefreshing && state.query.isEmpty() -> LoadingState()
                tab == 0 && drivers.isNullOrEmpty() -> EmptyState(emptyMessage(state), onRetry = viewModel::refresh)
                tab == 1 && state.constructors.isNullOrEmpty() -> EmptyState(emptyMessage(state), onRetry = viewModel::refresh)
                tab == 0 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(drivers.orEmpty(), key = { it.driver.id }) { DriverStandingRow(it, onClick = { onDriverClick(it.driver.id) }) }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(state.constructors.orEmpty(), key = { it.constructor.id }) {
                        ConstructorStandingRow(it, onClick = { onConstructorClick(it.constructor.id) })
                    }
                }
            }
        }
    }
}

private fun emptyMessage(state: StandingsUiState) =
    if (state.query.isNotBlank()) "Aucun résultat pour « ${state.query} »" else state.error ?: "Pas encore de classement"

@Composable
private fun DriverStandingRow(standing: DriverStanding, onClick: () -> Unit) {
    StandingRow(
        position = standing.position,
        teamId = standing.constructor?.id,
        title = "${flagOf(standing.driver.nationality)}  ${standing.driver.fullName}",
        subtitle = standing.constructor?.name.orEmpty(),
        points = standing.points,
        wins = standing.wins,
        onClick = onClick,
    )
}

@Composable
private fun ConstructorStandingRow(standing: ConstructorStanding, onClick: () -> Unit) {
    StandingRow(
        position = standing.position,
        teamId = standing.constructor.id,
        title = standing.constructor.name,
        subtitle = standing.constructor.nationality,
        points = standing.points,
        wins = standing.wins,
        onClick = onClick,
    )
}

@Composable
private fun StandingRow(
    position: Int,
    teamId: String?,
    title: String,
    subtitle: String,
    points: Double,
    wins: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PositionText(position)
        TeamStripe(TeamColors.of(teamId), height = 34.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(points.formatPoints(), style = MaterialTheme.typography.titleMedium)
            Text(if (wins > 0) "$wins VICT." else "PTS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
