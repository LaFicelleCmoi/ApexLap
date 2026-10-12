package com.apexlap.ui.telemetry

import com.apexlap.i18n.I18n
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexlap.data.openf1.OpenF1Repository
import com.apexlap.domain.live.LiveDriver
import com.apexlap.domain.live.LiveSession
import com.apexlap.domain.telemetry.CompareLapsUseCase
import com.apexlap.domain.telemetry.LapComparison
import com.apexlap.domain.telemetry.LapRef
import com.apexlap.domain.telemetry.MapTrackUseCase
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.toUserMessage
import com.apexlap.ui.navigation.TelemetryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TelemetryUiState(
    val sessions: List<LiveSession> = emptyList(),
    val session: LiveSession? = null,
    val drivers: List<LiveDriver> = emptyList(),
    val laps: Map<Int, List<LapRef>> = emptyMap(),
    val lapA: LapRef? = null,
    val lapB: LapRef? = null,
    val comparison: LapComparison? = null,
    val track: List<MapTrackUseCase.MappedPoint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    fun driver(number: Int?): LiveDriver? = drivers.firstOrNull { it.number == number }
}

@HiltViewModel
class LapComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OpenF1Repository,
    private val clock: Clock,
    private val compareLaps: CompareLapsUseCase,
    private val mapTrack: MapTrackUseCase,
) : ViewModel() {

    private val requestedSession = savedStateHandle.toRoute<TelemetryRoute>().sessionKey.takeIf { it > 0 }
    private val state = MutableStateFlow(TelemetryUiState())
    val uiState: StateFlow<TelemetryUiState> = state.asStateFlow()
    private var job: Job? = null

    init {
        load {
            val now = clock.now()
            // Laps with timing exist for every session type; races and qualifying are the interesting ones.
            val sessions = repository.sessions(clock.currentSeason()).filter { it.hasEnded(now) }
            state.update { it.copy(sessions = sessions) }
            val initial = sessions.firstOrNull { it.key == requestedSession }
                ?: sessions.firstOrNull { it.type == "Qualifying" }
                ?: sessions.firstOrNull()
            initial?.let { loadSession(it) }
        }
    }

    fun retry() {
        val session = state.value.session
        load { if (session != null) loadSession(session) else Unit }
    }

    fun selectSession(key: Int) {
        val session = state.value.sessions.firstOrNull { it.key == key } ?: return
        load { loadSession(session) }
    }

    fun selectDriver(slot: Int, number: Int) {
        val lap = fastest(state.value.laps[number]) ?: return
        selectLap(slot, lap)
    }

    fun selectLap(slot: Int, lap: LapRef) {
        state.update { if (slot == 0) it.copy(lapA = lap) else it.copy(lapB = lap) }
        load { compare() }
    }

    private suspend fun loadSession(session: LiveSession) {
        state.update { it.copy(session = session, comparison = null, track = emptyList()) }
        val drivers = repository.drivers(session.key)
        val laps = repository.sessionLaps(session.key)
        // Default duel: the two fastest drivers of the session, on their best laps.
        val ranked = laps.mapNotNull { (_, driverLaps) -> fastest(driverLaps) }.sortedBy { it.duration }
        state.update { it.copy(drivers = drivers.filter { d -> laps.containsKey(d.number) }, laps = laps, lapA = ranked.getOrNull(0), lapB = ranked.getOrNull(1)) }
        compare()
    }

    private suspend fun compare() {
        val session = state.value.session ?: return
        val a = state.value.lapA ?: return
        val b = state.value.lapB ?: return
        val traceA = repository.trace(session.key, a)
        val traceB = repository.trace(session.key, b)
        if (traceA == null || traceB == null) {
            state.update { it.copy(comparison = null, error = I18n.strings.telemetryUnavailable) }
            return
        }
        val comparison = compareLaps(traceA, traceB)
        val track = mapTrack(traceA, repository.location(session.key, a))
        state.update { it.copy(comparison = comparison, track = track) }
    }

    private fun load(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(error = e.toUserMessage()) }
            } finally {
                // A load cancelled by a newer one must not clear the newer one's spinner.
                if (isActive) state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun fastest(laps: List<LapRef>?): LapRef? = laps?.filter { it.duration > 0 }?.minByOrNull { it.duration }
}
