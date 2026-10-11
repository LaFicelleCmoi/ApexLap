package com.apexlap.ui.encyclopedia

import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.encyclopedia.Champion
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.PositionText
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.components.shortDay
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors

private const val STANDINGS_SHOWN = 10

@Composable
fun SeasonScreen(
    onBack: () -> Unit,
    onRaceClick: (season: Int, round: Int) -> Unit,
    onDriverClick: (String) -> Unit,
    onConstructorClick: (String) -> Unit,
    viewModel: SeasonViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DetailScaffold(title = "Saison ${state.season}", onBack = onBack) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { OfflineBanner(it, onRetry = viewModel::load) } }
            state.champion?.let { champion -> item { ChampionBanner(champion, onDriverClick, onConstructorClick) } }

            if (state.winners.isNotEmpty()) {
                item { SectionHeader("Grands Prix · ${state.winners.size}") }
                items(state.winners, key = { "gp-${it.race.round}" }) { (race, winner) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onRaceClick(race.season, race.round) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(race.round.toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                        Text(flagOf(race.circuit.country), style = MaterialTheme.typography.titleLarge)
                        Column(Modifier.weight(1f)) {
                            Text(race.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(race.raceSession?.date?.shortDay().orEmpty(), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TeamStripe(TeamColors.of(winner.constructor.id), height = 24.dp)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(winner.driver.fullName, style = MaterialTheme.typography.bodySmall)
                            Text(winner.constructor.name, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (state.drivers.isNotEmpty()) {
                item { SectionHeader("Championnat pilotes") }
                items(state.drivers.take(STANDINGS_SHOWN), key = { "d-${it.driver.id}" }) { standing ->
                    StandingLine(standing.position, standing.constructor?.id, standing.driver.fullName,
                        standing.constructor?.name.orEmpty(), standing.points, standing.wins) { onDriverClick(standing.driver.id) }
                }
            }
            if (state.constructors.isNotEmpty()) {
                item { SectionHeader("Championnat constructeurs") }
                items(state.constructors.take(STANDINGS_SHOWN), key = { "c-${it.constructor.id}" }) { standing ->
                    StandingLine(standing.position, standing.constructor.id, standing.constructor.name,
                        standing.constructor.nationality, standing.points, standing.wins) { onConstructorClick(standing.constructor.id) }
                }
            } else if (state.season < 1958 && !state.isLoading) {
                item {
                    Text(
                        "Le championnat des constructeurs n'existe que depuis 1958.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChampionBanner(champion: Champion, onDriverClick: (String) -> Unit, onConstructorClick: (String) -> Unit) {
    val color = TeamColors.of(champion.driver.teamId)
    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp), color = ApexColors.Carbon) {
        Column(
            Modifier
                .background(Brush.horizontalGradient(listOf(ApexColors.Gold.copy(alpha = 0.35f), ApexColors.Carbon)))
                .padding(20.dp),
        ) {
            Text("🏆 CHAMPION DU MONDE", style = MaterialTheme.typography.labelMedium, color = ApexColors.Gold)
            Text(
                champion.driver.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clickable { onDriverClick(champion.driver.id) },
            )
            Text(
                "${flagOf(champion.driver.nationality)} ${champion.driver.team.orEmpty()} · ${champion.driver.points.formatPoints()} pts · " +
                    "${champion.driver.wins} victoire" + (if (champion.driver.wins > 1) "s" else "") + " en ${champion.races} GP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            champion.constructor?.let { team ->
                Row(
                    Modifier.padding(top = 12.dp).clickable { onConstructorClick(team.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(4.dp).padding(vertical = 2.dp).background(TeamColors.of(team.id), RoundedCornerShape(2.dp)))
                    Text("Constructeurs : ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${team.name} · ${team.points.formatPoints()} pts", style = MaterialTheme.typography.titleSmall)
                }
            }
            Box(Modifier.padding(top = 2.dp).width(48.dp).background(color, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun StandingLine(position: Int, teamId: String?, title: String, subtitle: String, points: Double, wins: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PositionText(position)
        TeamStripe(TeamColors.of(teamId))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(points.formatPoints(), style = MaterialTheme.typography.titleSmall)
            if (wins > 0) Text("$wins VICT.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
