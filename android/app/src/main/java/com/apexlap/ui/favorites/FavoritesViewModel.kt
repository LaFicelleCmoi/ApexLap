package com.apexlap.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val season: Int,
    val driverId: String? = null,
    val constructorId: String? = null,
    /** Current-season standing of the followed driver and team, when they are ranked. */
    val driver: DriverStanding? = null,
    val constructor: ConstructorStanding? = null,
) {
    val hasFavorites: Boolean get() = driverId != null || constructorId != null
}

/** The driver and team the user follows, shared by the home screen, the standings and the detail pages. */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val repository: F1Repository,
    clock: Clock,
) : ViewModel() {

    private val season = clock.currentSeason()

    init {
        // The home screen does not load the standings itself: fetch them once when something is followed.
        viewModelScope.launch {
            if (!settings.settings.first().let { it.favoriteDriverId != null || it.favoriteConstructorId != null }) return@launch
            try {
                repository.refreshStandings(season)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline: the cached standings are shown.
            }
        }
    }

    val uiState: StateFlow<FavoritesUiState> = combine(
        settings.settings,
        repository.observeDriverStandings(season),
        repository.observeConstructorStandings(season),
    ) { prefs, drivers, constructors ->
        FavoritesUiState(
            season = season,
            driverId = prefs.favoriteDriverId,
            constructorId = prefs.favoriteConstructorId,
            driver = drivers.firstOrNull { it.driver.id == prefs.favoriteDriverId },
            constructor = constructors.firstOrNull { it.constructor.id == prefs.favoriteConstructorId },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState(season))

    fun toggleDriver(driverId: String) {
        viewModelScope.launch {
            val current = settings.settings.first().favoriteDriverId
            settings.setFavoriteDriver(driverId.takeIf { it != current })
        }
    }

    fun toggleConstructor(constructorId: String) {
        viewModelScope.launch {
            val current = settings.settings.first().favoriteConstructorId
            settings.setFavoriteConstructor(constructorId.takeIf { it != current })
        }
    }
}
