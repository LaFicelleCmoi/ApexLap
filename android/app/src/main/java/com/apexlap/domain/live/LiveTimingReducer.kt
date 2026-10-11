package com.apexlap.domain.live

/** Pure state machine shared by the replay and the live WebSocket stream. */
object LiveTimingReducer {

    private const val RACE_CONTROL_HISTORY = 60

    fun initial(session: LiveSession?, drivers: List<LiveDriver>) = LiveTimingState(
        session = session,
        timings = drivers.associate { it.number to DriverTiming(it) },
    )

    fun reduce(state: LiveTimingState, event: TimingEvent): LiveTimingState {
        val clock = if (state.clock == null || event.date.isAfter(state.clock)) event.date else state.clock
        return when (event) {
            is TimingEvent.Position -> state.update(event.driver) { it.copy(position = event.position) }
            is TimingEvent.Interval -> state.update(event.driver) { it.copy(gapToLeader = event.gapToLeader, interval = event.interval) }
            is TimingEvent.Lap -> reduceLap(state, event)
            is TimingEvent.StintUpdate -> state.update(event.driver) { timing ->
                timing.copy(stints = (timing.stints.filterNot { it.number == event.stint.number } + event.stint).sortedBy { it.number })
            }
            is TimingEvent.PitStop -> state.update(event.driver) { it.copy(pitStops = it.pitStops + 1) }
            is TimingEvent.RaceControl -> applyRaceControl(state, event).copy(
                raceControl = (listOf(RaceControlEntry(event.date, event.message, event.flag, event.lap)) + state.raceControl)
                    .take(RACE_CONTROL_HISTORY),
            )
        }.copy(clock = clock)
    }

    private fun reduceLap(state: LiveTimingState, event: TimingEvent.Lap): LiveTimingState {
        val next = state.update(event.driver) { timing ->
            val valid = event.duration?.takeIf { it > 0 }
            timing.copy(
                lapsCompleted = if (valid != null) maxOf(timing.lapsCompleted, event.lapNumber) else timing.lapsCompleted,
                lastLap = valid ?: timing.lastLap,
                bestLap = listOfNotNull(timing.bestLap, valid).minOrNull(),
                lastSectors = event.sectors,
            )
        }
        val best = next.timings[event.driver]?.bestLap ?: return next
        val fastest = next.fastestLap
        return if (fastest == null || best < fastest.second) next.copy(fastestLap = event.driver to best) else next
    }

    /**
     * Track status from race control. Wording varies by season ("VSC DEPLOYED" in 2026, "VIRTUAL SAFETY CAR
     * DEPLOYED" in 2025); sector yellows are tracked individually so clearing one sector keeps the others.
     */
    internal fun applyRaceControl(state: LiveTimingState, event: TimingEvent.RaceControl): LiveTimingState {
        val message = event.message.uppercase()
        val flag = event.flag?.uppercase()
        val sector = Regex("SECTOR (\\d+)").find(message)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            flag == "RED" -> state.copy(incident = TrackStatus.RED)
            flag == "CHEQUERED" -> state.copy(incident = TrackStatus.CHEQUERED, yellowSectors = emptySet())
            message.contains("VSC DEPLOYED") || message.contains("VIRTUAL SAFETY CAR DEPLOYED") ->
                state.copy(incident = TrackStatus.VSC)
            message.contains("SAFETY CAR DEPLOYED") -> state.copy(incident = TrackStatus.SAFETY_CAR)
            (flag == "GREEN" || flag == "CLEAR") && event.scope == "Track" ->
                state.copy(incident = TrackStatus.GREEN, yellowSectors = emptySet())
            (flag == "YELLOW" || flag == "DOUBLE YELLOW") && sector != null -> state.copy(yellowSectors = state.yellowSectors + sector)
            flag == "CLEAR" && sector != null -> state.copy(yellowSectors = state.yellowSectors - sector)
            else -> state
        }
    }

    private fun LiveTimingState.update(driver: Int, transform: (DriverTiming) -> DriverTiming): LiveTimingState {
        val timing = timings[driver] ?: DriverTiming(LiveDriver(driver, "#$driver", "Pilote $driver", "", null))
        return copy(timings = timings + (driver to transform(timing)))
    }
}
