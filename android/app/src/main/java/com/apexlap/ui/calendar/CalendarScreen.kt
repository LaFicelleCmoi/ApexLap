package com.apexlap.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.model.Countdown
import com.apexlap.i18n.LocalStrings
import com.apexlap.ui.favorites.FavoritesUiState
import com.apexlap.ui.favorites.FavoritesViewModel
import com.apexlap.domain.model.Race
import com.apexlap.domain.usecase.UpcomingSession
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.formatPoints
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.ui.components.shortDay
import com.apexlap.ui.theme.ApexColors
import com.apexlap.ui.theme.TeamColors
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onRaceClick: (Race) -> Unit,
    onRemindersClick: () -> Unit,
    onEncyclopediaClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDriverClick: (String) -> Unit,
    onConstructorClick: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val races = state.races

    PullToRefreshBox(isRefreshing = state.isRefreshing && !races.isNullOrEmpty(), onRefresh = viewModel::refresh) {
        when {
            races.isNullOrEmpty() && state.isRefreshing -> LoadingState()
            races.isNullOrEmpty() -> EmptyState(state.error ?: strings.noGrandPrix(state.season), onRetry = viewModel::refresh)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Row(
                        Modifier.statusBarsPadding().padding(start = 16.dp, top = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.seasonTitle(state.season), style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = onEncyclopediaClick) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = strings.encyclopedia)
                        }
                        IconButton(onClick = onRemindersClick) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = strings.sessionReminders)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, contentDescription = strings.settings)
                        }
                    }
                }
                state.error?.let { item { OfflineBanner(it, viewModel::refresh) } }
                state.nextRace?.let { race ->
                    item {
                        NextRaceHero(race, state.nextSession, state.countdown, onClick = { onRaceClick(race) })
                    }
                }
                item { FavoritesCard(favorites, onDriverClick, onConstructorClick, onSettingsClick) }
                item { SectionHeader(strings.calendar) }
                items(races, key = { it.round }) { race ->
                    RaceRow(race, now = state.now, isNext = race.round == state.nextRace?.round, onClick = { onRaceClick(race) })
                }
            }
        }
    }
}

@Composable
private fun NextRaceHero(race: Race, next: UpcomingSession?, countdown: Countdown?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ApexColors.Carbon,
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(listOf(ApexColors.RacingRed.copy(alpha = 0.55f), ApexColors.Carbon)),
            ),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(LocalStrings.current.nextGrandPrix(race.round), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text("${flagOf(race.circuit.country)}  ${race.name}", style = MaterialTheme.typography.headlineMedium)
                Text(race.circuit.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (next != null && countdown != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${LocalStrings.current.session(next.session.type).uppercase()} · ${next.session.startsAt?.localDay()} ${next.session.startsAt?.localTime()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    CountdownRow(countdown)
                }
            }
        }
    }
}

@Composable
fun CountdownRow(countdown: Countdown) {
    val strings = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountdownUnit(countdown.days, strings.days)
        CountdownUnit(countdown.hours, strings.hoursShort)
        CountdownUnit(countdown.minutes, strings.minutesShort)
        CountdownUnit(countdown.seconds, strings.secondsShort)
    }
}

@Composable
private fun CountdownUnit(value: Long, label: String) {
    Surface(color = ApexColors.Asphalt.copy(alpha = 0.7f), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RaceRow(race: Race, now: Instant, isNext: Boolean, onClick: () -> Unit) {
    val finished = race.isFinished(now)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (finished) 0.55f else 1f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            race.round.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
        )
        Text(flagOf(race.circuit.country), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(race.name, style = MaterialTheme.typography.titleMedium)
            Text(
                buildString {
                    append(race.circuit.locality)
                    if (race.isSprintWeekend) append(" · Sprint")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val first = race.sessions.firstOrNull()?.date
            val last = race.raceSession?.date
            Text(
                if (first != null && last != null && first != last) "${first.dayOfMonth}–${last.shortDay()}" else last?.shortDay().orEmpty(),
                style = MaterialTheme.typography.labelMedium,
            )
            if (finished) {
                Text(LocalStrings.current.finished, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (isNext) {
                Text(LocalStrings.current.next, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** The followed driver and team at a glance, or an invitation to pick them. */
@Composable
private fun FavoritesCard(
    favorites: FavoritesUiState,
    onDriverClick: (String) -> Unit,
    onConstructorClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (!favorites.hasFavorites) {
            Row(
                Modifier.clickable(onClick = onSettingsClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = ApexColors.Gold)
                Text(strings.chooseFavorites, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                Text("›", style = MaterialTheme.typography.titleLarge)
            }
            return@Surface
        }
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = ApexColors.Gold, modifier = Modifier.padding(end = 6.dp))
                Text(strings.myFavorites.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            favorites.driverId?.let { id ->
                val standing = favorites.driver
                FavoriteRow(
                    teamId = standing?.constructor?.id,
                    title = standing?.let { "${flagOf(it.driver.nationality)}  ${it.driver.fullName}" } ?: id,
                    subtitle = standing?.let { strings.standingLine(it.position, it.points.formatPoints(), it.wins) } ?: strings.notRankedYet,
                    onClick = { onDriverClick(id) },
                )
            }
            favorites.constructorId?.let { id ->
                val standing = favorites.constructor
                FavoriteRow(
                    teamId = id,
                    title = standing?.constructor?.name ?: id,
                    subtitle = standing?.let { strings.standingLine(it.position, it.points.formatPoints(), it.wins) } ?: strings.notRankedYet,
                    onClick = { onConstructorClick(id) },
                )
            }
        }
    }
}

@Composable
private fun FavoriteRow(teamId: String?, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeamStripe(TeamColors.of(teamId), height = 32.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
