package com.apexlap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.i18n.LanguagePreference
import com.apexlap.settings.AppSettings
import com.apexlap.settings.SettingsStore
import com.apexlap.ui.components.OfflineFirstLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    /** Current-season standings, used as the lists to pick favourites from. */
    val drivers: List<DriverStanding>? = null,
    val constructors: List<ConstructorStanding> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    repository: F1Repository,
    clock: Clock,
) : ViewModel() {

    private val season = clock.currentSeason()

    private val drivers = OfflineFirstLoader(
        scope = viewModelScope,
        source = repository.observeDriverStandings(season),
        refreshAction = { repository.refreshStandings(season) },
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        store.settings,
        drivers.state,
        repository.observeConstructorStandings(season),
    ) { settings, driverState, constructors ->
        SettingsUiState(settings, driverState.data, constructors, driverState.isRefreshing, driverState.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(isRefreshing = true))

    init {
        refresh()
    }

    fun refresh() = drivers.refresh()

    fun setLanguage(language: LanguagePreference) {
        viewModelScope.launch { store.setLanguage(language) }
    }

    fun setFavoriteDriver(driverId: String?) {
        viewModelScope.launch { store.setFavoriteDriver(driverId) }
    }

    fun setFavoriteConstructor(constructorId: String?) {
        viewModelScope.launch { store.setFavoriteConstructor(constructorId) }
    }
}
