package com.apexlap.ui.live

import com.apexlap.i18n.LocalStrings
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.live.DriverTiming
import com.apexlap.domain.live.LiveSession
import com.apexlap.domain.live.LiveTimingState
import com.apexlap.ui.components.EmptyState
import com.apexlap.ui.components.LoadingState
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.formatLapTime
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.ui.theme.ApexColors
import kotlinx.coroutines.launch

private val SPEEDS = listOf(1, 5, 10, 30, 60)

@Composable
fun LiveTimingScreen(onCompareLaps: (Int?) -> Unit, viewModel: LiveTimingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account by viewModel.accountName.collectAsStateWithLifecycle()
    var showAccount by remember { mutableStateOf(false) }
    var showGap by remember { mutableStateOf(true) }
    val timing = state.timing
    val strings = LocalStrings.current

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.statusBarsPadding().padding(start = 16.dp, top = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.liveTiming, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { onCompareLaps(state.session?.key) }) {
                Icon(Icons.Outlined.Timeline, contentDescription = strings.compareLaps)
            }
            IconButton(onClick = { showAccount = true }) {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = strings.openF1Account,
                    tint = if (account != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionPicker(state.sessions, state.session, viewModel::selectSession, Modifier.weight(1f))
            val running = state.liveSession
            if (running != null && state.mode != LiveMode.LIVE) {
                Button(onClick = { if (account != null) viewModel.goLive() else showAccount = true }) {
                    LiveDot()
                    Text(strings.liveBadge)
                }
            }
        }

        state.error?.let { OfflineBanner(it, onRetry = viewModel::retry) }

        when {
            timing == null && state.isLoading -> LoadingState()
            timing == null -> EmptyState(state.error ?: strings.noSessionAvailable, onRetry = viewModel::retry)
            else -> {
                StatusBar(state.mode, timing)
                state.replay?.let { progress ->
                    ReplayControls(progress, timing, viewModel::togglePlay, viewModel::setSpeed, viewModel::seek)
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { TowerHeader(timing.session?.isRace == true, showGap) { showGap = !showGap } }
                    val leaderBest = timing.tower.firstOrNull()?.bestLap
                    items(timing.tower, key = { it.driver.number }) { row ->
                        TowerRow(row, timing, showGap, leaderBest)
                    }
                    if (timing.raceControl.isNotEmpty()) {
                        item { SectionHeader(strings.raceControl) }
                        items(timing.raceControl.take(8), key = { it.date.toEpochMilli().toString() + it.message }) { entry ->
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Text(
                                    entry.date.localTime() + (entry.lap?.let { " · " + strings.lapShort(it) } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(86.dp),
                                )
                                Text(entry.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccount) {
        AccountDialog(
            account = account,
            onSignIn = viewModel::signIn,
            onSignOut = { viewModel.signOut(); showAccount = false },
            onDismiss = { showAccount = false },
            onSignedIn = {
                showAccount = false
                if (state.liveSession != null) viewModel.goLive()
            },
        )
    }
}

@Composable
private fun SessionPicker(
    sessions: List<LiveSession>,
    selected: LiveSession?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable(enabled = sessions.isNotEmpty()) { expanded = true },
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selected?.let { "${it.circuit} · ${LocalStrings.current.openF1Session(it.name)}" } ?: LocalStrings.current.chooseSession,
                        style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    selected?.let {
                        Text(it.start.localDay(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = LocalStrings.current.changeSession)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sessions.take(40).forEach { session ->
                DropdownMenuItem(
                    text = { Text("${session.circuit} · ${LocalStrings.current.openF1Session(session.name)} — ${session.start.localDay()}") },
                    onClick = {
                        expanded = false
                        onSelect(session.key)
                    },
                )
            }
        }
    }
}


@Composable
private fun StatusBar(mode: LiveMode, timing: LiveTimingState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (mode == LiveMode.LIVE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Text(LocalStrings.current.onAir, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Text(LocalStrings.current.replay, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TrackStatusPill(timing.trackStatus)
        Box(Modifier.weight(1f))
        if (timing.session?.isRace == true && timing.leaderLap > 0) {
            Text(LocalStrings.current.lap(timing.leaderLap), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReplayControls(
    progress: ReplayProgress,
    timing: LiveTimingState,
    onToggle: () -> Unit,
    onSpeed: (Int) -> Unit,
    onSeek: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledIconButton(onClick = onToggle) {
                Icon(if (progress.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (progress.playing) LocalStrings.current.pause else LocalStrings.current.play)
            }
            SPEEDS.forEach { speed ->
                FilterChip(selected = progress.speed == speed, onClick = { onSpeed(speed) }, label = { Text("×$speed") })
            }
        }
        Slider(value = progress.fraction, onValueChange = onSeek)
        Text(
            (timing.clock ?: progress.current).localTime() + LocalStrings.current.localTimeSuffix,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TowerHeader(isRace: Boolean, showGap: Boolean, onToggle: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(strings.colPos, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp))
        Text(strings.colDriver, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(
            if (!isRace) strings.colGap else if (showGap) strings.colGapSwitch else strings.colIntervalSwitch,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(92.dp).clickable(enabled = isRace, onClick = onToggle),
        )
        Text(strings.colLast, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
        Text(strings.colTyre, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun TowerRow(row: DriverTiming, timing: LiveTimingState, showGap: Boolean, leaderBest: Double?) {
    val teamColor = row.driver.teamColour?.let { Color(0xFF000000 or it) } ?: ApexColors.Telemetry
    val isRace = timing.session?.isRace == true
    val hasFastest = timing.fastestLap?.first == row.driver.number
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.position?.toString() ?: "–",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(28.dp),
        )
        Box(Modifier.width(4.dp).fillMaxHeight(0.6f).background(teamColor, RoundedCornerShape(2.dp)))
        Box(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(row.driver.acronym, style = MaterialTheme.typography.titleSmall)
            if (row.pitStops > 0) {
                Text(LocalStrings.current.pitStops(row.pitStops), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val gap = if (isRace) {
            if (showGap) row.gapToLeader.display(LocalStrings.current) else row.interval.display(LocalStrings.current)
        } else {
            val best = row.bestLap
            when {
                best == null -> "—"
                leaderBest == null || best == leaderBest -> formatLapTime(best)
                else -> "+" + String.format(java.util.Locale.US, "%.3f", best - leaderBest)
            }
        }
        Text(gap, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.width(92.dp))
        Text(
            row.lastLap?.let(::formatLapTime) ?: "—",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (hasFastest && row.lastLap == row.bestLap) ApexColors.PurpleSector else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(72.dp),
        )
        val stint = row.currentStint
        if (stint != null) TyreBadge(stint.compound, row.tyreAge, Modifier.width(44.dp)) else Box(Modifier.width(44.dp))
    }
}

@Composable
private fun AccountDialog(
    account: String?,
    onSignIn: suspend (String, String) -> String?,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.openF1Account) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (account != null) {
                    Text(strings.connectedAs(account), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        strings.liveAvailableDuringSessions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        strings.openF1Explanation,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = username, onValueChange = { username = it }, label = { Text(strings.email) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text(strings.password) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openf1.org/#sponsor")))
                    }) { Text(strings.becomeSponsor) }
                }
            }
        },
        confirmButton = {
            if (account != null) {
                OutlinedButton(onClick = onSignOut) { Text(strings.signOut) }
            } else {
                Button(
                    enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        busy = true
                        scope.launch {
                            error = onSignIn(username, password)
                            busy = false
                            if (error == null) onSignedIn()
                        }
                    },
                ) { Text(if (busy) strings.signingIn else strings.signIn) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
    )
}

