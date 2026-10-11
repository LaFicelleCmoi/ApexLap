package com.apexlap.data.openf1

import com.apexlap.domain.live.LiveDriver
import com.apexlap.domain.live.LiveSession
import com.apexlap.domain.live.LiveTimingReducer
import com.apexlap.domain.live.LiveTimingState
import com.apexlap.domain.live.ReplayTimeline
import com.apexlap.domain.live.TimingEvent
import com.apexlap.domain.telemetry.LapRef
import com.apexlap.domain.telemetry.LapTrace
import com.apexlap.domain.telemetry.TrackPoint
import com.apexlap.domain.usecase.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenF1 access with in-memory caches: the free tier allows only 30 requests a minute, and past sessions
 * never change, so each dataset is downloaded once per app run.
 */
@Singleton
class OpenF1Repository @Inject constructor(
    private val api: OpenF1Api,
    private val tokens: OpenF1TokenProvider,
    private val credentials: OpenF1Credentials,
    private val liveStream: OpenF1LiveStream,
    private val clock: Clock,
) {
    private val sessionsByYear = ConcurrentHashMap<Int, List<LiveSession>>()
    private val drivers = ConcurrentHashMap<Int, List<LiveDriver>>()
    private val replays = ConcurrentHashMap<Int, ReplayTimeline>()
    private val sessionLaps = ConcurrentHashMap<Int, Map<Int, List<LapRef>>>()
    private val traces = ConcurrentHashMap<LapRef, LapTrace>()
    private val locations = ConcurrentHashMap<LapRef, List<TrackPoint>>()
    private val replayMutex = Mutex()

    /** Sessions that have started, most recent first. */
    suspend fun sessions(year: Int): List<LiveSession> {
        val all = sessionsByYear[year] ?: api.sessions(year).map { it.toDomain() }.also { sessionsByYear[year] = it }
        val now = clock.now()
        return all.filter { it.start.isBefore(now) }.sortedByDescending { it.start }
    }

    suspend fun drivers(sessionKey: Int): List<LiveDriver> =
        drivers[sessionKey] ?: api.drivers(sessionKey.toString()).map { it.toDomain() }.distinctBy { it.number }
            .sortedBy { it.number }.also { drivers[sessionKey] = it }

    /** Every timing event of a finished session, ready to be replayed. */
    suspend fun replay(session: LiveSession): ReplayTimeline = replayMutex.withLock {
        replays[session.key]?.let { return it }
        val key = session.key.toString()
        val events = loadEvents(key, auth = null, session = session)
        ReplayTimeline.build(session, drivers(session.key), events).also { replays[session.key] = it }
    }

    suspend fun hasLiveAccount(): Boolean = credentials.account() != null

    /**
     * Current session snapshot through the authenticated REST API (`session_key=latest`), to seed the tower
     * before the push feed takes over.
     */
    suspend fun liveSnapshot(): LiveTimingState {
        val auth = "Bearer " + (tokens.token() ?: throw OpenF1AuthException("Aucun compte OpenF1 configuré"))
        val session = api.session("latest", auth).firstOrNull()?.toDomain() ?: throw OpenF1AuthException("Aucune séance en cours")
        val drivers = api.drivers("latest", auth).map { it.toDomain() }.distinctBy { it.number }
        val events = loadEvents("latest", auth, session)
        return events.sortedBy { it.date }.fold(LiveTimingReducer.initial(session, drivers), LiveTimingReducer::reduce)
    }

    fun liveEvents(): Flow<TimingEvent> = flow {
        val account = credentials.account() ?: throw OpenF1AuthException("Aucun compte OpenF1 configuré")
        val token = tokens.token() ?: throw OpenF1AuthException("Aucun compte OpenF1 configuré")
        liveStream.events(account.username, token).collect { emit(it) }
    }

    /** Validates credentials by requesting a token. */
    suspend fun signIn(account: OpenF1Account) {
        credentials.save(account)
        tokens.invalidate()
        try {
            tokens.token()
        } catch (e: Exception) {
            credentials.clear()
            throw e
        }
    }

    suspend fun signOut() {
        credentials.clear()
        tokens.invalidate()
    }

    /** Timed laps of every driver in one request (cheaper than one call per driver on the free tier). */
    suspend fun sessionLaps(sessionKey: Int): Map<Int, List<LapRef>> =
        sessionLaps[sessionKey] ?: api.laps(sessionKey.toString()).mapNotNull { dto ->
            val start = dto.dateStart?.let(::parseOpenF1Date) ?: return@mapNotNull null
            val duration = dto.lapDuration ?: return@mapNotNull null
            LapRef(dto.driverNumber, dto.lapNumber, start, duration, listOf(dto.sector1, dto.sector2, dto.sector3))
        }.groupBy { it.driver }.mapValues { (_, laps) -> laps.sortedBy { it.lapNumber } }
            .also { sessionLaps[sessionKey] = it }

    suspend fun trace(sessionKey: Int, lap: LapRef): LapTrace? {
        traces[lap]?.let { return it }
        val (from, to) = window(lap)
        val samples = api.carData(sessionKey, lap.driver, from, to).mapNotNull { it.toSample() }
        return LapTrace.build(lap, samples)?.also { traces[lap] = it }
    }

    suspend fun location(sessionKey: Int, lap: LapRef): List<TrackPoint> =
        locations[lap] ?: window(lap).let { (from, to) ->
            api.location(sessionKey, lap.driver, from, to).map { it.toPoint() }
        }.also { locations[lap] = it }

    private fun window(lap: LapRef): Pair<String, String> {
        val from = lap.start.minusSeconds(1)
        val to = lap.start.plusMillis((lap.duration * 1000).toLong()).plusSeconds(1)
        return from.toString() to to.toString()
    }

    private suspend fun loadEvents(key: String, auth: String?, session: LiveSession): List<TimingEvent> {
        val beforeStart = session.start.minusSeconds(3600)
        return buildList {
            addAll(api.positions(key, auth).map { it.toEvent() })
            // Intervals only exist for races.
            if (session.isRace) addAll(api.intervals(key, auth).map { it.toEvent() })
            addAll(api.laps(key, auth).mapNotNull { it.toEvent(fallback = beforeStart) })
            addAll(api.stints(key, auth).map { it.toEvent(session.start.minusSeconds(120)) })
            addAll(api.raceControl(key, auth).map { it.toEvent() })
            addAll(api.pits(key, auth).map { it.toEvent() })
        }
    }
}
