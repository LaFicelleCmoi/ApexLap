package com.apexlap.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexlap.data.openf1.OpenF1Account
import com.apexlap.data.openf1.OpenF1AuthException
import com.apexlap.data.openf1.OpenF1Credentials
import com.apexlap.data.openf1.OpenF1Repository
import com.apexlap.domain.live.LiveSession
import com.apexlap.domain.live.LiveTimingReducer
import com.apexlap.domain.live.LiveTimingState
import com.apexlap.domain.live.ReplayTimeline
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.ui.components.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class LiveMode { REPLAY, LIVE }

data class ReplayProgress(val start: Instant, val end: Instant, val current: Instant, val playing: Boolean, val speed: Int) {
    val fraction: Float
        get() = (Duration.between(start, current).toMillis().toFloat() / Duration.between(start, end).toMillis()).coerceIn(0f, 1f)
}

data class LiveUiState(
    val sessions: List<LiveSession> = emptyList(),
    val session: LiveSession? = null,
    val mode: LiveMode = LiveMode.REPLAY,
    val timing: LiveTimingState? = null,
    val replay: ReplayProgress? = null,
    /** A session is running right now (live data needs a sponsor account). */
    val liveSession: LiveSession? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class LiveTimingViewModel @Inject constructor(
    private val repository: OpenF1Repository,
    credentials: OpenF1Credentials,
    private val clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = state.asStateFlow()

    val accountName: StateFlow<String?> = credentials.username
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var timeline: ReplayTimeline? = null
    private var playback: Job? = null
    private var live: Job? = null

    init {
        viewModelScope.launch { loadSessions() }
    }

    private suspend fun loadSessions() {
        state.update { it.copy(isLoading = true, error = null) }
        try {
            val now = clock.now()
            val sessions = repository.sessions(clock.currentSeason())
            val running = sessions.firstOrNull { it.isLive(now) }
            state.update { it.copy(sessions = sessions, liveSession = running) }
            if (running != null && repository.hasLiveAccount()) {
                goLive()
            } else {
                sessions.firstOrNull { it.hasEnded(now) }?.let { selectSession(it.key) }
                    ?: state.update { it.copy(isLoading = false) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.update { it.copy(isLoading = false, error = e.toUserMessage()) }
        }
    }

    fun retry() {
        viewModelScope.launch { loadSessions() }
    }

    fun selectSession(key: Int) {
        val session = state.value.sessions.firstOrNull { it.key == key } ?: return
        stopAll()
        viewModelScope.launch {
            state.update { it.copy(session = session, mode = LiveMode.REPLAY, timing = null, replay = null, isLoading = true, error = null) }
            try {
                val loaded = repository.replay(session)
                timeline = loaded
                val first = loaded.advanceTo(loaded.start)
                state.update {
                    it.copy(timing = first, isLoading = false, replay = ReplayProgress(loaded.start, loaded.end, loaded.start, playing = false, speed = 10))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun togglePlay() {
        val progress = state.value.replay ?: return
        if (progress.playing) pause() else play()
    }

    fun setSpeed(speed: Int) {
        state.update { it.copy(replay = it.replay?.copy(speed = speed)) }
    }

    fun seek(fraction: Float) {
        val timeline = timeline ?: return
        val progress = state.value.replay ?: return
        val total = Duration.between(progress.start, progress.end).toMillis()
        val target = progress.start.plusMillis((total * fraction).toLong())
        state.update { it.copy(timing = timeline.advanceTo(target), replay = progress.copy(current = target)) }
    }

    private fun play() {
        val timeline = timeline ?: return
        state.update { it.copy(replay = it.replay?.copy(playing = true)) }
        playback = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val progress = state.value.replay ?: break
                val next = progress.current.plusMillis(TICK_MS * progress.speed)
                if (!next.isBefore(progress.end)) {
                    state.update { it.copy(timing = timeline.advanceTo(progress.end), replay = progress.copy(current = progress.end, playing = false)) }
                    break
                }
                state.update { it.copy(timing = timeline.advanceTo(next), replay = progress.copy(current = next)) }
            }
        }
    }

    private fun pause() {
        playback?.cancel()
        state.update { it.copy(replay = it.replay?.copy(playing = false)) }
    }

    /** Seeds the tower from the authenticated REST snapshot, then applies the WebSocket feed. */
    fun goLive() {
        stopAll()
        live = viewModelScope.launch {
            state.update { it.copy(mode = LiveMode.LIVE, session = it.liveSession, replay = null, timing = null, isLoading = true, error = null) }
            var attempt = 0
            while (isActive) {
                try {
                    var timing = repository.liveSnapshot()
                    state.update { it.copy(timing = timing, session = timing.session ?: it.session, isLoading = false, error = null) }
                    attempt = 0
                    repository.liveEvents().collect { event ->
                        timing = LiveTimingReducer.reduce(timing, event)
                        state.update { it.copy(timing = timing) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: OpenF1AuthException) {
                    state.update { it.copy(isLoading = false, error = e.message) }
                    return@launch
                } catch (e: Exception) {
                    state.update { it.copy(isLoading = false, error = "Flux interrompu, reconnexion…") }
                }
                attempt++
                delay(minOf(30_000L, 2_000L shl minOf(attempt, 4)))
            }
        }
    }

    suspend fun signIn(username: String, password: String): String? = try {
        repository.signIn(OpenF1Account(username, password))
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: retrofit2.HttpException) {
        if (e.code() == 401 || e.code() == 400) "Identifiants OpenF1 incorrects" else e.toUserMessage()
    } catch (e: Exception) {
        e.toUserMessage()
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            if (state.value.mode == LiveMode.LIVE) {
                stopAll()
                state.value.sessions.firstOrNull { it.hasEnded(clock.now()) }?.let { selectSession(it.key) }
            }
        }
    }

    private fun stopAll() {
        playback?.cancel()
        live?.cancel()
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
