package com.apexlap.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.domain.model.Countdown
import com.apexlap.domain.model.Race
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.ComputeCountdownUseCase
import com.apexlap.domain.usecase.FindNextRaceUseCase
import com.apexlap.domain.usecase.FindNextSessionUseCase
import com.apexlap.domain.usecase.UpcomingSession
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.OfflineFirstLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject

data class CalendarUiState(
    val season: Int,
    val races: List<Race>? = null,
    val nextRace: Race? = null,
    val nextSession: UpcomingSession? = null,
    val countdown: Countdown? = null,
    val now: Instant = Instant.EPOCH,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: F1Repository,
    private val clock: Clock,
    private val findNextRace: FindNextRaceUseCase,
    private val findNextSession: FindNextSessionUseCase,
    private val computeCountdown: ComputeCountdownUseCase,
) : ViewModel() {

    private val season = clock.currentSeason()

    private val loader = OfflineFirstLoader(
        scope = viewModelScope,
        source = repository.observeCalendar(season),
        refreshAction = { repository.refreshCalendar(season) },
    )

    private val ticker = flow {
        while (true) {
            emit(clock.now())
            delay(1_000)
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(loader.state, ticker) { state, now ->
        val races = state.data
        val next = races?.let { findNextSession(it, now) }
        CalendarUiState(
            season = season,
            races = races,
            nextRace = races?.let { findNextRace(it, now) },
            nextSession = next,
            countdown = next?.session?.startsAt?.let { computeCountdown(it, now) },
            now = now,
            isRefreshing = state.isRefreshing,
            error = state.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(season, isRefreshing = true))

    init {
        refresh()
    }

    fun refresh() = loader.refresh()
}
