package com.apexlap.ui.encyclopedia

import com.apexlap.i18n.Strings
import com.apexlap.i18n.LocalStrings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.encyclopedia.GlossaryCategory
import com.apexlap.domain.encyclopedia.GlossaryTerm
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.ScreenState
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(
    onBack: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    onDriverClick: (String) -> Unit,
    onConstructorClick: (String) -> Unit,
    onCircuitClick: (String) -> Unit,
    viewModel: EncyclopediaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(EncyclopediaTab.SEASONS) }
    val strings = LocalStrings.current
    LaunchedEffect(tab) { viewModel.onTabShown(tab) }

    DetailScaffold(title = strings.encyclopedia, onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, containerColor = MaterialTheme.colorScheme.background, edgePadding = 8.dp) {
                EncyclopediaTab.entries.forEach {
                    Tab(selected = tab == it, onClick = { tab = it }, text = { Text(it.label(strings)) })
                }
            }
            if (tab != EncyclopediaTab.SEASONS) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text(placeholder(tab, strings)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (tab == EncyclopediaTab.DRIVERS || tab == EncyclopediaTab.CONSTRUCTORS) {
                FilterChip(
                    selected = state.championsOnly,
                    onClick = { viewModel.onChampionsOnlyChange(!state.championsOnly) },
                    label = { Text(strings.worldChampionsFilter) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when (tab) {
                EncyclopediaTab.SEASONS -> Seasons(state, onSeasonClick)
                EncyclopediaTab.DRIVERS -> RemoteList(state.drivers, strings.noDriverFound, { viewModel.refresh(tab) }) { drivers ->
                    items(drivers, key = { it.id }) { driver ->
                        val titles = state.registry?.driverTitles(driver.id)?.count ?: 0
                        EntryRow(
                            title = "${flagOf(driver.nationality)}  ${driver.fullName}",
                            subtitle = listOfNotNull(driver.nationality, driver.dateOfBirth?.year?.let { strings.bornIn(it) }).joinToString(" · "),
                            badge = titles.takeIf { it > 0 }?.let { "🏆 ×$it" },
                            onClick = { onDriverClick(driver.id) },
                        )
                    }
                }
                EncyclopediaTab.CONSTRUCTORS -> RemoteList(state.constructors, strings.noTeamFound, { viewModel.refresh(tab) }) { teams ->
                    items(teams, key = { it.id }) { team ->
                        val titles = state.registry?.constructorTitles(team.id)?.count ?: 0
                        EntryRow(
                            title = team.name,
                            subtitle = "${flagOf(team.nationality)} ${team.nationality}",
                            badge = titles.takeIf { it > 0 }?.let { "🏆 ×$it" },
                            stripe = TeamColors.of(team.id),
                            onClick = { onConstructorClick(team.id) },
                        )
                    }
                }
                EncyclopediaTab.CIRCUITS -> RemoteList(state.circuits, strings.noCircuitFound, { viewModel.refresh(tab) }) { circuits ->
                    items(circuits, key = { it.id }) { circuit ->
                        EntryRow(
                            title = "${flagOf(circuit.country)}  ${circuit.name}",
                            subtitle = "${circuit.locality}, ${circuit.country}",
                            onClick = { onCircuitClick(circuit.id) },
                        )
                    }
                }
                EncyclopediaTab.GLOSSARY -> GlossaryList(state.glossary)
            }
        }
    }
}

private fun placeholder(tab: EncyclopediaTab, strings: Strings) = when (tab) {
    EncyclopediaTab.DRIVERS -> strings.searchDriver
    EncyclopediaTab.CONSTRUCTORS -> strings.searchTeam
    EncyclopediaTab.CIRCUITS -> strings.searchCircuit
    else -> strings.searchTerm
}

@Composable
private fun Seasons(state: EncyclopediaUiState, onSeasonClick: (Int) -> Unit) {
    val strings = LocalStrings.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.champions.none { it.season == state.currentSeason }) {
            item {
                val leader = state.currentLeader
                SeasonRow(
                    season = state.currentSeason,
                    driverLine = leader?.let { strings.leaderLine(it.driver.fullName, it.points.formatPoints()) } ?: strings.seasonInProgress,
                    teamLine = leader?.constructor?.name,
                    teamId = leader?.constructor?.id,
                    ongoing = true,
                    onClick = { onSeasonClick(state.currentSeason) },
                )
            }
        }
        items(state.champions, key = { it.season }) { champion ->
            SeasonRow(
                season = champion.season,
                driverLine = "${flagOf(champion.driver.nationality)}  ${champion.driver.name}" +
                    (champion.driver.team?.let { " · $it" } ?: ""),
                teamLine = champion.constructor?.let { strings.constructorsChampion(it.name) },
                teamId = champion.driver.teamId,
                ongoing = false,
                onClick = { onSeasonClick(champion.season) },
            )
        }
    }
}

@Composable
private fun SeasonRow(season: Int, driverLine: String, teamLine: String?, teamId: String?, ongoing: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            season.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = if (ongoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(76.dp),
        )
        TeamStripe(TeamColors.of(teamId), height = 34.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(driverLine, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            teamLine?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Text(if (ongoing) LocalStrings.current.inProgress else "›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> RemoteList(
    state: ScreenState<List<T>>,
    emptyMessage: String,
    onRefresh: () -> Unit,
    content: LazyListScope.(List<T>) -> Unit,
) {
    val strings = LocalStrings.current
    val data = state.data
    Column {
        state.error?.let { OfflineBanner(it, onRetry = onRefresh) }
        PullToRefreshBox(isRefreshing = state.isRefreshing && !data.isNullOrEmpty(), onRefresh = onRefresh) {
            when {
                data.isNullOrEmpty() && state.isRefreshing -> LoadingState()
                data.isNullOrEmpty() -> EmptyState(state.error ?: emptyMessage, onRetry = onRefresh)
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    item {
                        Text(
                            strings.results(data.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    content(data)
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badge: String? = null,
    stripe: androidx.compose.ui.graphics.Color? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        stripe?.let { TeamStripe(it) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        badge?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = ApexColors.Gold) }
    }
}

@Composable
private fun GlossaryList(terms: List<GlossaryTerm>) {
    val strings = LocalStrings.current
    if (terms.isEmpty()) {
        EmptyState(LocalStrings.current.noTermFound)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        GlossaryCategory.entries.forEach { category ->
            val inCategory = terms.filter { it.category == category }
            if (inCategory.isNotEmpty()) {
                item(key = category.name) { SectionHeader(strings.glossaryCategory(category)) }
                items(inCategory, key = { it.term.fr }) { GlossaryCard(it) }
            }
        }
    }
}

@Composable
private fun GlossaryCard(term: GlossaryTerm) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(14.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(term.term[strings.language], style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                Text(
                    term.definition[strings.language],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
