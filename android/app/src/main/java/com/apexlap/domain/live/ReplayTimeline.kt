package com.apexlap.domain.live

import java.time.Instant

/**
 * A recorded session as a time-ordered event list. [advanceTo] applies events incrementally while
 * playing forward; seeking backwards rebuilds from the start (tens of thousands of events take a few ms).
 */
class ReplayTimeline(
    private val initial: LiveTimingState,
    events: List<TimingEvent>,
    val start: Instant,
    val end: Instant,
) {
    private val events = events.sortedBy { it.date }
    private var cursor = 0
    private var position: Instant = Instant.MIN
    var state: LiveTimingState = initial
        private set

    fun advanceTo(time: Instant): LiveTimingState {
        if (time.isBefore(position)) {
            cursor = 0
            state = initial
        }
        var current = state
        while (cursor < events.size && !events[cursor].date.isAfter(time)) {
            current = LiveTimingReducer.reduce(current, events[cursor])
            cursor++
        }
        position = time
        state = current.copy(clock = time)
        return state
    }

    companion object {
        /**
         * Lap events are stamped when the lap is completed. Positions recorded before the start (the grid)
         * are kept so the tower is ordered from the first frame.
         */
        fun build(session: LiveSession, drivers: List<LiveDriver>, events: List<TimingEvent>): ReplayTimeline {
            val initial = LiveTimingReducer.initial(session, drivers)
            val first = events.minOfOrNull { it.date } ?: session.start
            val start = if (first.isBefore(session.start)) session.start.minusSeconds(60) else first
            val end = maxOf(session.end, events.maxOfOrNull { it.date } ?: session.end)
            return ReplayTimeline(initial, events, start, end)
        }
    }
}
