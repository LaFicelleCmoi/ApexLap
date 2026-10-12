package com.apexlap.ui.encyclopedia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.StatTile
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.toUserMessage
import com.apexlap.ui.navigation.CircuitRoute
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors
import com.apexlap.domain.encyclopedia.CircuitGuides
import com.apexlap.domain.encyclopedia.CircuitHistory
import com.apexlap.domain.encyclopedia.CircuitRecords
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CircuitUiState(
    val history: CircuitHistory? = null,
    /** Loaded after the history; missing records never block the page. */
    val records: CircuitRecords? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class CircuitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val encyclopedia: EncyclopediaRepository,
) : ViewModel() {
    private val circuitId = savedStateHandle.toRoute<CircuitRoute>().circuitId
    private val state = MutableStateFlow(CircuitUiState())
    val uiState: StateFlow<CircuitUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, error = null)
            state.value = try {
                CircuitUiState(history = encyclopedia.circuitHistory(circuitId), isLoading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.value.copy(isLoading = false, error = e.toUserMessage())
            }
            try {
                val records = encyclopedia.circuitRecords(circuitId)
                state.value = state.value.copy(records = records)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Records are a bonus: the history is still shown without them.
            }
        }
    }
}

@Composable
fun CircuitScreen(
    onBack: () -> Unit,
    onRaceClick: (season: Int, round: Int) -> Unit,
    onDriverClick: (String) -> Unit,
    viewModel: CircuitViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history = state.history

    DetailScaffold(title = history?.circuit?.name ?: "Circuit", onBack = onBack) { padding ->
        when {
            history == null && state.isLoading -> LoadingState(Modifier.padding(padding))
            history == null -> EmptyState(state.error ?: "Circuit introuvable", Modifier.padding(padding), viewModel::load)
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("${flagOf(history.circuit.country)}  ${history.circuit.name}", style = MaterialTheme.typography.headlineSmall)
                        Text("${history.circuit.locality}, ${history.circuit.country}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("Grands Prix", history.grandsPrix.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        StatTile("Premier", history.firstSeason?.toString() ?: "—", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        StatTile("Dernier", history.lastSeason?.toString() ?: "—", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    }
                }
                CircuitGuides.of(history.circuit.id)?.let { guide ->
                    item {
                        Text(guide.summary, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    item { SectionHeader("Virages clés") }
                    items(guide.corners, key = { "corner-${it.name}" }) { corner ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(corner.name, style = MaterialTheme.typography.titleSmall)
                            Text(corner.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                state.records?.takeIf { it.raceLap != null || it.poleSitters.isNotEmpty() }?.let { records ->
                    item { SectionHeader("Records") }
                    records.raceLap?.let { lap ->
                        item {
                            RecordRow(
                                label = "Record du tour en course",
                                value = lap.time,
                                detail = "${lap.driver.fullName} · ${lap.constructor.name} · ${lap.season}",
                                onClick = { onDriverClick(lap.driver.id) },
                            )
                        }
                    }
                    records.poleSitters.firstOrNull()?.let { (driver, poles) ->
                        item {
                            RecordRow(
                                label = "Le plus de départs en pole",
                                value = poles.toString(),
                                detail = records.poleSitters.joinToString("  ·  ") { (d, n) -> "${d.familyName} $n" },
                                onClick = { onDriverClick(driver.id) },
                            )
                        }
                    }
                    item {
                        Text(
                            "Temps au tour connus depuis 2004, toutes configurations du circuit confondues.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                if (history.wins.isEmpty()) {
                    item { EmptyState("Aucun Grand Prix disputé ici pour l'instant") }
                } else {
                    item { SectionHeader("Rois du circuit") }
                    items(history.topDrivers(), key = { "top-${it.first.id}" }) { (driver, wins) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onDriverClick(driver.id) }.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${flagOf(driver.nationality)}  ${driver.fullName}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("$wins victoire" + if (wins > 1) "s" else "", style = MaterialTheme.typography.labelMedium, color = ApexColors.Gold)
                        }
                    }
                    item {
                        Text(
                            history.topConstructors().joinToString("  ·  ") { (team, wins) -> "${team.name} $wins" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    item { SectionHeader("Palmarès") }
                    items(history.wins, key = { "${it.season}-${it.round}" }) { win ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onRaceClick(win.season, win.round) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(win.season.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(52.dp))
                            TeamStripe(TeamColors.of(win.constructor.id), height = 24.dp)
                            Column(Modifier.weight(1f)) {
                                Text(win.driver.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(win.constructor.name, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = ApexColors.PurpleSector)
    }
}
