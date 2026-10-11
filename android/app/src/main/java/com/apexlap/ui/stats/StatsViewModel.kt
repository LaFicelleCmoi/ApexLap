package com.apexlap.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.model.ScoredResult
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.ChampionshipProgression
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.ComputeChampionshipProgressionUseCase
import com.apexlap.domain.usecase.ComputeHeadToHeadUseCase
import com.apexlap.domain.usecase.ComputeSeasonLeadersUseCase
import com.apexlap.domain.usecase.DriverProgression
import com.apexlap.domain.usecase.HeadToHead
import com.apexlap.domain.usecase.LeaderBoard
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val season: Int,
    val progression: ChampionshipProgression? = null,
    val leaders: List<LeaderBoard> = emptyList(),
    /** Drivers in championship order, for the duel pickers. */
    val drivers: List<DriverProgression> = emptyList(),
    val firstId: String? = null,
    val secondId: String? = null,
    val duel: HeadToHead? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: F1Repository,
    clock: Clock,
    private val computeProgression: ComputeChampionshipProgressionUseCase,
    private val computeLeaders: ComputeSeasonLeadersUseCase,
    private val computeHeadToHead: ComputeHeadToHeadUseCase,
) : ViewModel() {

    private val season = clock.currentSeason()
    private val selection = MutableStateFlow<Pair<String?, String?>>(null to null)

    private val loader = OfflineFirstLoader(
        scope = viewModelScope,
        source = repository.observeSeasonResults(season),
        refreshAction = { force -> repository.refreshSeasonResults(season, force) },
    )

    val uiState: StateFlow<StatsUiState> = combine(loader.state, selection) { state, (first, second) ->
        val results = state.data.orEmpty()
        val progression = computeProgression(results)
        // Default duel: the two championship leaders.
        val firstId = first ?: progression.drivers.getOrNull(0)?.driver?.id
        val secondId = second ?: progression.drivers.firstOrNull { it.driver.id != firstId }?.driver?.id
        StatsUiState(
            season = season,
            progression = progression.takeIf { results.isNotEmpty() },
            leaders = computeLeaders(results),
            drivers = progression.drivers,
            firstId = firstId,
            secondId = secondId,
            duel = duelOf(results, firstId, secondId),
            isRefreshing = state.isRefreshing,
            error = state.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState(season, isRefreshing = true))

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) = loader.refresh(force)

    fun selectFirst(driverId: String) {
        selection.value = driverId to (uiState.value.secondId.takeIf { it != driverId })
    }

    fun selectSecond(driverId: String) {
        selection.value = (uiState.value.firstId.takeIf { it != driverId }) to driverId
    }

    private fun duelOf(results: List<ScoredResult>, first: String?, second: String?): HeadToHead? =
        if (first == null || second == null) null else computeHeadToHead(results, first, second)
}
