package com.apexlap.ui.constructor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.Helmet
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.StatTile
import com.apexlap.ui.components.TitlesRow
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors

@Composable
fun ConstructorScreen(onBack: () -> Unit, onDriverClick: (String) -> Unit, viewModel: ConstructorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile

    DetailScaffold(title = profile?.constructor?.name ?: "Écurie", onBack = onBack) { padding ->
        when {
            profile == null && state.isRefreshing -> LoadingState(Modifier.padding(padding))
            profile == null -> EmptyState(state.error ?: "Écurie introuvable", Modifier.padding(padding), { viewModel.refresh(force = true) })
            else -> {
                val color = TeamColors.of(profile.constructor.id)
                Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                    state.error?.let { OfflineBanner(it, { viewModel.refresh(force = true) }) }
                    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp), color = ApexColors.Carbon) {
                        Column(
                            Modifier
                                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), ApexColors.Carbon)))
                                .padding(20.dp),
                        ) {
                            Text(profile.constructor.name.uppercase(), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "${flagOf(profile.constructor.nationality)} ${profile.constructor.nationality}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    state.standing?.let { standing ->
                        SectionHeader("Saison ${state.season}")
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("Position", "P${standing.position}", color, Modifier.weight(1f))
                            StatTile("Points", standing.points.formatPoints(), color, Modifier.weight(1f))
                            StatTile("Victoires", standing.wins.toString(), color, Modifier.weight(1f))
                        }
                    }

                    if (state.drivers.isNotEmpty()) {
                        SectionHeader("Pilotes")
                        state.drivers.forEach { driver ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onDriverClick(driver.driver.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Helmet(color, size = 44.dp)
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(driver.driver.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text("P${driver.position} · ${driver.points.formatPoints()} pts", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(driver.driver.number ?: "", style = MaterialTheme.typography.headlineSmall, color = color)
                            }
                        }
                    }

                    if (state.titles.count > 0) {
                        SectionHeader("Titres constructeurs")
                        TitlesRow(state.titles)
                    }

                    SectionHeader("Historique")
                    val stats = profile.stats
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("Saisons", stats.seasons.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                            StatTile("Départs", stats.races.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("Victoires", stats.wins.toString(), ApexColors.Gold, Modifier.weight(1f))
                            StatTile("Podiums", stats.podiums.toString(), ApexColors.Silver, Modifier.weight(1f))
                            StatTile("Poles", stats.poles.toString(), ApexColors.PurpleSector, Modifier.weight(1f))
                        }
                    }
                    Text(
                        "Départs, victoires et podiums comptent chaque voiture classée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
