package com.apexlap.ui.race

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexlap.data.encyclopedia.EncyclopediaRepository
import com.apexlap.domain.encyclopedia.GrandPrixContext
import com.apexlap.domain.encyclopedia.GrandPrixFacts
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.ui.components.OfflineFirstLoader
import com.apexlap.ui.navigation.RaceRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WeekendResults(
    val race: List<RaceResult> = emptyList(),
    val qualifying: List<QualifyingResult> = emptyList(),
    val sprint: List<RaceResult> = emptyList(),
)

data class RaceUiState(
    val race: Race? = null,
    val results: WeekendResults = WeekendResults(),
    val hasStarted: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** History of the circuit before this Grand Prix; null while loading or offline. */
    val context: GrandPrixContext? = null,
) {
    val facts: GrandPrixFacts get() = GrandPrixFacts.of(results.race, results.qualifying)
}

@HiltViewModel
class RaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: F1Repository,
    private val encyclopedia: EncyclopediaRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RaceRoute>()

    private val race = repository.observeCalendar(route.season)
        .map { races -> races.firstOrNull { it.round == route.round } }

    private val results = OfflineFirstLoader(
        scope = viewModelScope,
        source = combine(
            repository.observeRaceResults(route.season, route.round),
            repository.observeQualifying(route.season, route.round),
            repository.observeSprintResults(route.season, route.round),
            ::WeekendResults,
        ),
        refreshAction = { refreshWeekend() },
    )

    private val context = race.map { it?.circuit?.id }.distinctUntilChanged().map { circuitId ->
        circuitId ?: return@map null
        try {
            encyclopedia.circuitHistory(circuitId).contextFor(route.season)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }.onStart { emit(null) }

    val uiState: StateFlow<RaceUiState> = combine(race, results.state, context) { race, results, context ->
        RaceUiState(
            context = context,
            race = race,
            results = results.data ?: WeekendResults(),
            hasStarted = race?.hasStarted() == true,
            isRefreshing = results.isRefreshing,
            error = results.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RaceUiState(isRefreshing = true))

    init {
        refresh()
    }

    fun refresh() = results.refresh(force = true)

    /** Nothing to download before the first session of the weekend has started. */
    private suspend fun refreshWeekend() {
        val race = race.first()
        if (race != null && !race.hasStarted()) return
        repository.refreshRaceWeekend(route.season, route.round, includeSprint = race?.isSprintWeekend ?: true)
    }

    private fun Race.hasStarted(): Boolean {
        val first = sessions.firstOrNull()?.startsAt ?: return true
        return clock.now().isAfter(first)
    }
}
