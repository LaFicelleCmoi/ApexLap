package com.apexlap.ui.driver

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.Titles
import com.apexlap.domain.encyclopedia.driverBio
import com.apexlap.domain.model.DriverProfile
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import com.apexlap.ui.navigation.DriverRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DriverUiState(
    val season: Int,
    val profile: DriverProfile? = null,
    /** Current-season standing, also gives the driver's team (and therefore colours). */
    val standing: DriverStanding? = null,
    val titles: Titles = Titles.NONE,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val bio: String? = null,
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: F1Repository,
    encyclopedia: EncyclopediaRepository,
    clock: Clock,
) : ViewModel() {

    private val driverId = savedStateHandle.toRoute<DriverRoute>().driverId
    private val season = clock.currentSeason()

    private val profile = OfflineFirstLoader(
        scope = viewModelScope,
        source = repository.observeDriverProfile(driverId),
        refreshAction = { force -> repository.refreshDriverProfile(driverId, force) },
    )

    private val standing = repository.observeDriverStandings(season)
        .map { rows -> rows.firstOrNull { it.driver.id == driverId } }

    private val titles = flow { emit(encyclopedia.champions().driverTitles(driverId)) }

    val uiState: StateFlow<DriverUiState> = combine(profile.state, standing, titles) { profile, standing, titles ->
        val today = clock.now().atZone(java.time.ZoneOffset.UTC).toLocalDate()
        DriverUiState(
            season, profile.data, standing, titles, profile.isRefreshing, profile.error,
            bio = profile.data?.let { driverBio(it, titles, standing, season, today) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverUiState(season, isRefreshing = true))

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) = profile.refresh(force)
}
