package com.apexlap.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StandingsUiState(
    val season: Int,
    val query: String = "",
    val drivers: List<DriverStanding>? = null,
    val constructors: List<ConstructorStanding>? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class StandingsViewModel @Inject constructor(
    repository: F1Repository,
    clock: Clock,
) : ViewModel() {

    private val season = clock.currentSeason()
    private val query = MutableStateFlow("")

    private val drivers = OfflineFirstLoader(
        scope = viewModelScope,
        source = repository.observeDriverStandings(season),
        refreshAction = { repository.refreshStandings(season) },
    )
    private val constructors = repository.observeConstructorStandings(season)

    val uiState: StateFlow<StandingsUiState> = combine(drivers.state, constructors, query) { driverState, teams, q ->
        StandingsUiState(
            season = season,
            query = q,
            drivers = driverState.data?.filter { it.matches(q) },
            constructors = teams.filter { it.constructor.name.contains(q, ignoreCase = true) },
            isRefreshing = driverState.isRefreshing,
            error = driverState.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StandingsUiState(season, isRefreshing = true))

    init {
        refresh()
    }

    fun refresh() = drivers.refresh()

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun DriverStanding.matches(q: String) = q.isBlank() ||
        driver.fullName.contains(q, ignoreCase = true) ||
        driver.code.contains(q, ignoreCase = true) ||
        constructor?.name?.contains(q, ignoreCase = true) == true
}
