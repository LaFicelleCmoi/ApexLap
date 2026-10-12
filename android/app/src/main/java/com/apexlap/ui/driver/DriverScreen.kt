package com.apexlap.ui.driver

import com.apexlap.ui.components.fullDate
import com.apexlap.ui.favorites.FollowButton
import com.apexlap.ui.favorites.FavoritesViewModel
import com.apexlap.i18n.LocalStrings
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
fun DriverScreen(
    onBack: () -> Unit,
    onConstructorClick: (String) -> Unit,
    viewModel: DriverViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val profile = state.profile
    val team = state.standing?.constructor
    val teamColor = TeamColors.of(team?.id)

    DetailScaffold(
        title = profile?.driver?.fullName ?: strings.driver,
        onBack = onBack,
        actions = {
            profile?.let { FollowButton(isFollowed = favorites.driverId == it.driver.id) { favoritesViewModel.toggleDriver(it.driver.id) } }
        },
    ) { padding ->
        when {
            profile == null && state.isRefreshing -> LoadingState(Modifier.padding(padding))
            profile == null -> EmptyState(state.error ?: strings.driverNotFound, Modifier.padding(padding), { viewModel.refresh(force = true) })
            else -> Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                state.error?.let { OfflineBanner(it, { viewModel.refresh(force = true) }) }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = ApexColors.Carbon,
                ) {
                    Row(
                        Modifier
                            .background(Brush.horizontalGradient(listOf(teamColor.copy(alpha = 0.45f), ApexColors.Carbon)))
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.driver.number ?: "—", style = MaterialTheme.typography.displayLarge, color = teamColor)
                            Text(profile.driver.givenName, style = MaterialTheme.typography.titleMedium)
                            Text(profile.driver.familyName.uppercase(), style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${flagOf(profile.driver.nationality)} ${profile.driver.nationality} · ${profile.driver.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Helmet(teamColor)
                    }
                }

                if (team != null) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onConstructorClick(team.id) }.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.padding(end = 8.dp).background(teamColor, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(strings.teamBadge, style = MaterialTheme.typography.labelSmall, color = ApexColors.Asphalt)
                        }
                        Text(team.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("›", style = MaterialTheme.typography.titleLarge)
                    }
                }

                state.bio?.let { bio ->
                    SectionHeader(strings.bio)
                    Text(bio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
                }

                state.standing?.let { standing ->
                    SectionHeader(strings.seasonHeader(state.season))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(strings.statPosition, "P${standing.position}", teamColor, Modifier.weight(1f))
                        StatTile(strings.statPoints, standing.points.formatPoints(), teamColor, Modifier.weight(1f))
                        StatTile(strings.statWins, standing.wins.toString(), teamColor, Modifier.weight(1f))
                    }
                }

                if (state.titles.count > 0) {
                    SectionHeader(strings.worldTitles)
                    TitlesRow(state.titles)
                }

                SectionHeader(strings.career)
                val stats = profile.stats
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(strings.statSeasons, stats.seasons.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        StatTile(strings.statRaces, stats.races.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        StatTile(strings.statPoles, stats.poles.toString(), ApexColors.PurpleSector, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(strings.statWins, stats.wins.toString(), ApexColors.Gold, Modifier.weight(1f))
                        StatTile(strings.statPodiums, stats.podiums.toString(), ApexColors.Silver, Modifier.weight(1f))
                        val rate = if (stats.races > 0) stats.wins * 100 / stats.races else 0
                        StatTile(strings.statWinRate, "$rate%", ApexColors.GreenSector, Modifier.weight(1f))
                    }
                }
                profile.driver.dateOfBirth?.let {
                    Text(
                        strings.born(it.fullDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
