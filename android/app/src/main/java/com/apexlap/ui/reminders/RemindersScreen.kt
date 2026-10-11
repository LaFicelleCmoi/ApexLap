package com.apexlap.ui.reminders

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.domain.usecase.PlannedReminder
import com.apexlap.domain.usecase.ReminderScope
import com.apexlap.domain.usecase.ReminderSettings
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.localDay
import com.apexlap.ui.components.localTime
import com.apexlap.widget.NextSessionWidgetReceiver

@Composable
fun RemindersScreen(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(context.notificationsAllowed()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = context.notificationsAllowed()
        viewModel.onResume()
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        permissionDenied = !granted
        if (granted) viewModel.setEnabled(true)
    }

    fun onToggle(enabled: Boolean) {
        if (enabled && !context.notificationsAllowed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setEnabled(enabled)
        }
    }

    DetailScaffold(title = "Rappels & widget", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Me prévenir avant les séances", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Une notification avant chaque séance choisie, même app fermée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.settings.enabled, onCheckedChange = ::onToggle)
                }
                if (permissionDenied || (state.settings.enabled && !notificationsAllowed)) {
                    Warning(
                        "Les notifications sont désactivées pour Apex Lap.",
                        action = "Réglages",
                        onAction = { context.openNotificationSettings() },
                    )
                }
                if (state.settings.enabled && !state.exactAlarmsAllowed) {
                    Warning(
                        "Sans l'autorisation « alarmes exactes », un rappel peut arriver quelques minutes en retard.",
                        action = "Autoriser",
                        onAction = { context.openExactAlarmSettings() },
                    )
                }
            }

            if (state.settings.enabled) {
                SectionHeader("Combien de temps avant")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderSettings.LEAD_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = state.settings.leadMinutes == minutes,
                            onClick = { viewModel.setLeadMinutes(minutes) },
                            label = { Text(if (minutes < 60) "$minutes min" else "${minutes / 60} h") },
                        )
                    }
                }

                SectionHeader("Quelles séances")
                Card {
                    ReminderScope.entries.forEach { scope ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.settings.scope == scope,
                                    onClick = { viewModel.setScope(scope) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.settings.scope == scope, onClick = null)
                            Text(scope.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }

                SectionHeader("Prochains rappels")
                if (state.upcoming.isEmpty()) {
                    Text(
                        "Aucune séance dans les 14 prochains jours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                state.upcoming.take(8).forEach { UpcomingRow(it) }
            }

            SectionHeader("Widget d'écran d'accueil")
            Card {
                Text(
                    "Affiche la prochaine séance et le temps restant sur votre écran d'accueil.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                val widgetManager = remember { context.getSystemService(AppWidgetManager::class.java) }
                if (widgetManager.isRequestPinAppWidgetSupported) {
                    TextButton(onClick = {
                        widgetManager.requestPinAppWidget(ComponentName(context, NextSessionWidgetReceiver::class.java), null, null)
                    }) { Text("AJOUTER LE WIDGET") }
                } else {
                    Text(
                        "Appui long sur l'écran d'accueil → Widgets → Apex Lap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Warning(message: String, action: String, onAction: () -> Unit) {
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction) { Text(action.uppercase()) }
    }
}

@Composable
private fun UpcomingRow(reminder: PlannedReminder) {
    val start = reminder.session.startsAt ?: return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(flagOf(reminder.race.circuit.country), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("${reminder.session.type.label} · ${reminder.race.name}", style = MaterialTheme.typography.titleSmall)
            Text(
                "${start.localDay()} à ${start.localTime()} · rappel à ${reminder.triggerAt.localTime()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Context.notificationsAllowed(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
