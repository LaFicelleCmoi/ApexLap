package com.apexlap.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apexlap.domain.usecase.ReminderScope
import com.apexlap.domain.usecase.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

@Singleton
class ReminderSettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    val settings: Flow<ReminderSettings> = context.reminderStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[ENABLED] ?: false,
            leadMinutes = prefs[LEAD_MINUTES] ?: ReminderSettings().leadMinutes,
            scope = prefs[SCOPE]?.let { runCatching { ReminderScope.valueOf(it) }.getOrNull() } ?: ReminderSettings().scope,
        )
    }

    suspend fun update(transform: (ReminderSettings) -> ReminderSettings) {
        val next = transform(settings.first())
        context.reminderStore.edit { prefs ->
            prefs[ENABLED] = next.enabled
            prefs[LEAD_MINUTES] = next.leadMinutes
            prefs[SCOPE] = next.scope.name
        }
    }

    /** Alarm request codes currently registered, so stale ones can be cancelled on the next plan. */
    suspend fun scheduledIds(): Set<Int> =
        context.reminderStore.data.first()[SCHEDULED_IDS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    suspend fun setScheduledIds(ids: Set<Int>) {
        context.reminderStore.edit { it[SCHEDULED_IDS] = ids.map(Int::toString).toSet() }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val LEAD_MINUTES = intPreferencesKey("lead_minutes")
        val SCOPE = stringPreferencesKey("scope")
        val SCHEDULED_IDS = stringSetPreferencesKey("scheduled_ids")
    }
}
