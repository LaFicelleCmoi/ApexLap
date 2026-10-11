package com.apexlap.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RaceSummary(
    val race: Race,
    /** First three of the Grand Prix. */
    val podium: List<RaceResult>,
    val sprintWinner: RaceResult?,
)

data class ResultsUiState(
    val season: Int,
    /** Most recent Grand Prix first. */
    val races: List<RaceSummary>? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: F1Repository,
    clock: Clock,
) : ViewModel() {

    private val season = clock.currentSeason()

    private val summaries = combine(
        repository.observeCalendar(season),
        repository.observeSeasonResults(season),
    ) { calendar, results ->
        val byRound = results.groupBy { it.round }
        calendar.mapNotNull { race ->
            val rows = byRound[race.round] ?: return@mapNotNull null
            val podium = rows.filter { !it.isSprint && it.result.position in 1..3 }.map { it.result }.sortedBy { it.position }
            if (podium.isEmpty()) return@mapNotNull null
            RaceSummary(race, podium, rows.firstOrNull { it.isSprint && it.result.position == 1 }?.result)
        }.sortedByDescending { it.race.round }
    }

    private val loader = OfflineFirstLoader(
        scope = viewModelScope,
        source = summaries,
        refreshAction = { force ->
            repository.refreshSeasonResults(season, force)
            // Race names and flags come from the calendar; fetch it if this tab is opened first.
            if (force || repository.observeCalendar(season).first().isEmpty()) repository.refreshCalendar(season)
        },
    )

    val uiState: StateFlow<ResultsUiState> = loader.state
        .map { ResultsUiState(season, it.data, it.isRefreshing, it.error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultsUiState(season, isRefreshing = true))

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) = loader.refresh(force)
}
