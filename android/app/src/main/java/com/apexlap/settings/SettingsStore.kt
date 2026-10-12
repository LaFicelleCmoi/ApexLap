package com.apexlap.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apexlap.i18n.LanguagePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val language: LanguagePreference = LanguagePreference.SYSTEM,
    /** Jolpica ids of the driver and team the user follows. */
    val favoriteDriverId: String? = null,
    val favoriteConstructorId: String? = null,
)

/** App-wide settings: language and favourites. Reminders keep their own store. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            language = prefs[LANGUAGE]?.let { runCatching { LanguagePreference.valueOf(it) }.getOrNull() } ?: LanguagePreference.SYSTEM,
            favoriteDriverId = prefs[FAVORITE_DRIVER],
            favoriteConstructorId = prefs[FAVORITE_CONSTRUCTOR],
        )
    }.distinctUntilChanged()

    suspend fun setLanguage(language: LanguagePreference) {
        context.settingsStore.edit { it[LANGUAGE] = language.name }
    }

    suspend fun setFavoriteDriver(driverId: String?) {
        context.settingsStore.edit { if (driverId == null) it.remove(FAVORITE_DRIVER) else it[FAVORITE_DRIVER] = driverId }
    }

    suspend fun setFavoriteConstructor(constructorId: String?) {
        context.settingsStore.edit { if (constructorId == null) it.remove(FAVORITE_CONSTRUCTOR) else it[FAVORITE_CONSTRUCTOR] = constructorId }
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val FAVORITE_DRIVER = stringPreferencesKey("favorite_driver")
        val FAVORITE_CONSTRUCTOR = stringPreferencesKey("favorite_constructor")
    }
}
