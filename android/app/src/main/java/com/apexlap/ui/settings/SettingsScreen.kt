package com.apexlap.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexlap.BuildConfig
import com.apexlap.i18n.LanguagePreference
import com.apexlap.i18n.LocalStrings
import com.apexlap.ui.components.DetailScaffold
import com.apexlap.ui.components.OfflineBanner
import com.apexlap.ui.components.SectionHeader
import com.apexlap.ui.components.TeamStripe
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.theme.TeamColors

@Composable
fun SettingsScreen(onBack: () -> Unit, onRemindersClick: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    DetailScaffold(title = strings.settings, onBack = onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(strings.language)
            Card {
                LanguagePreference.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.settings.language == option,
                                onClick = { viewModel.setLanguage(option) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.settings.language == option, onClick = null)
                        Text(
                            option.language?.nativeName ?: strings.systemLanguage,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            SectionHeader(strings.myFavorites)
            state.error?.takeIf { state.drivers.isNullOrEmpty() }?.let { OfflineBanner(it, onRetry = viewModel::refresh) }
            Card {
                val drivers = state.drivers.orEmpty()
                val favoriteDriver = drivers.firstOrNull { it.driver.id == state.settings.favoriteDriverId }
                Picker(
                    label = strings.favoriteDriver,
                    selected = favoriteDriver?.let { "${flagOf(it.driver.nationality)}  ${it.driver.fullName}" }
                        ?: state.settings.favoriteDriverId ?: strings.none,
                    teamId = favoriteDriver?.constructor?.id,
                    placeholder = if (state.drivers == null && state.isRefreshing) strings.loadingDrivers else null,
                    options = drivers.map { it.driver.id to "${it.driver.fullName} · ${it.constructor?.name.orEmpty()}" },
                    onSelect = viewModel::setFavoriteDriver,
                    noneLabel = strings.none,
                )
                Spacer(Modifier.height(12.dp))
                val favoriteTeam = state.constructors.firstOrNull { it.constructor.id == state.settings.favoriteConstructorId }
                Picker(
                    label = strings.favoriteTeam,
                    selected = favoriteTeam?.constructor?.name ?: state.settings.favoriteConstructorId ?: strings.none,
                    teamId = favoriteTeam?.constructor?.id,
                    placeholder = null,
                    options = state.constructors.map { it.constructor.id to it.constructor.name },
                    onSelect = viewModel::setFavoriteConstructor,
                    noneLabel = strings.none,
                )
                Text(
                    strings.favoriteHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            SectionHeader(strings.notifications)
            Card(onClick = onRemindersClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.remindersAndWidget, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.remindersAndWidgetHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("›", style = MaterialTheme.typography.headlineSmall)
                }
            }

            SectionHeader(strings.about)
            Card {
                Text("Apex Lap · ${strings.version(BuildConfig.VERSION_NAME)}", style = MaterialTheme.typography.titleSmall)
                Text(
                    strings.dataSources,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Card(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

/** A labelled dropdown; picking "none" clears the favourite. */
@Composable
private fun Picker(
    label: String,
    selected: String,
    teamId: String?,
    placeholder: String?,
    options: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
    noneLabel: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable(enabled = options.isNotEmpty()) { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (teamId != null) TeamStripe(TeamColors.of(teamId), height = 20.dp)
                Text(
                    placeholder ?: selected,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = label)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(noneLabel) }, onClick = { onSelect(null); expanded = false })
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
