package com.apexlap.ui.encyclopedia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.Champion
import com.apexlap.domain.encyclopedia.SeasonHighlights
import com.apexlap.domain.encyclopedia.SeasonWinner
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.toUserMessage
import com.apexlap.ui.navigation.SeasonRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeasonUiState(
    val season: Int,
    val champion: Champion? = null,
    val winners: List<SeasonWinner> = emptyList(),
    val drivers: List<DriverStanding> = emptyList(),
    val constructors: List<ConstructorStanding> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Neighbouring seasons for the timeline, null at either end. */
    val previous: Int? = null,
    val next: Int? = null,
) {
    val highlights: SeasonHighlights? get() = SeasonHighlights.of(winners, drivers).takeIf { winners.isNotEmpty() }
}

const val FIRST_SEASON = 1950

private data class Loaded(val champion: Champion? = null, val winners: List<SeasonWinner> = emptyList(), val loading: Boolean = true, val error: String? = null)

@HiltViewModel
class SeasonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val encyclopedia: EncyclopediaRepository,
    private val repository: F1Repository,
    clock: Clock,
) : ViewModel() {

    private val season = savedStateHandle.toRoute<SeasonRoute>().season
    private val loaded = MutableStateFlow(Loaded())
    private val previous = (season - 1).takeIf { it >= FIRST_SEASON }
    private val next = (season + 1).takeIf { it <= clock.currentSeason() }

    val uiState: StateFlow<SeasonUiState> = combine(
        loaded,
        repository.observeDriverStandings(season),
        repository.observeConstructorStandings(season),
    ) { l, drivers, constructors ->
        SeasonUiState(season, l.champion, l.winners, drivers, constructors, l.loading, l.error, previous, next)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeasonUiState(season, previous = previous, next = next))

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loaded.update { it.copy(loading = true, error = null, champion = encyclopedia.champions().season(season)) }
            try {
                val winners = encyclopedia.seasonWinners(season)
                loaded.update { it.copy(winners = winners) }
                repository.refreshStandings(season)
                // Caches the calendar so the Grand Prix screens of this season have their header and schedule.
                repository.refreshCalendar(season)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loaded.update { it.copy(error = e.toUserMessage()) }
            } finally {
                loaded.update { it.copy(loading = false) }
            }
        }
    }
}
