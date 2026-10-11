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
import androidx.compose.material.icons.outlined.NotificationsActive
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
import com.apexlap.domain.model.Race
import com.apexlap.domain.usecase.UpcomingSession
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.ui.components.shortDay
import com.apexlap.ui.theme.ApexColors
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onRaceClick: (Race) -> Unit,
    onRemindersClick: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val races = state.races

    PullToRefreshBox(isRefreshing = state.isRefreshing && !races.isNullOrEmpty(), onRefresh = viewModel::refresh) {
        when {
            races.isNullOrEmpty() && state.isRefreshing -> LoadingState()
            races.isNullOrEmpty() -> EmptyState(state.error ?: "Aucun Grand Prix pour ${state.season}", onRetry = viewModel::refresh)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Row(
                        Modifier.statusBarsPadding().padding(start = 16.dp, top = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("SAISON ${state.season}", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = onRemindersClick) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = "Rappels de séance")
                        }
                    }
                }
                state.error?.let { item { OfflineBanner(it, viewModel::refresh) } }
                state.nextRace?.let { race ->
                    item {
                        NextRaceHero(race, state.nextSession, state.countdown, onClick = { onRaceClick(race) })
                    }
                }
                item { SectionHeader("Calendrier") }
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
                Text("PROCHAIN GRAND PRIX · MANCHE ${race.round}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text("${flagOf(race.circuit.country)}  ${race.name}", style = MaterialTheme.typography.headlineMedium)
                Text(race.circuit.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (next != null && countdown != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${next.session.type.label.uppercase()} · ${next.session.startsAt?.localDay()} ${next.session.startsAt?.localTime()}",
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
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountdownUnit(countdown.days, "JOURS")
        CountdownUnit(countdown.hours, "H")
        CountdownUnit(countdown.minutes, "MIN")
        CountdownUnit(countdown.seconds, "SEC")
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
                Text("TERMINÉ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (isNext) {
                Text("PROCHAIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
