package com.apexlap.live

import com.apexlap.data.openf1.OpenF1DriverDto
import com.apexlap.data.openf1.OpenF1IntervalDto
import com.apexlap.data.openf1.OpenF1LapDto
import com.apexlap.data.openf1.OpenF1LiveStream
import com.apexlap.data.openf1.OpenF1RaceControlDto
import com.apexlap.data.openf1.OpenF1SessionDto
import com.apexlap.data.openf1.toDomain
import com.apexlap.data.openf1.toEvent
import com.apexlap.domain.live.Gap
import com.apexlap.domain.live.LiveDriver
import com.apexlap.domain.live.LiveSession
import com.apexlap.domain.live.LiveTimingReducer
import com.apexlap.domain.live.ReplayTimeline
import com.apexlap.domain.live.Stint
import com.apexlap.domain.live.TimingEvent
import com.apexlap.domain.live.TrackStatus
import com.apexlap.domain.live.TyreCompound
import com.apexlap.testing.decodeFixture
import com.apexlap.testing.testJson
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LiveTimingTest {

    private val t0 = Instant.parse("2026-10-11T12:00:00Z")
    private val session = LiveSession(1, "Race", "Race", t0, t0.plusSeconds(7200), "Singapore", "Singapore")
    private val ver = LiveDriver(3, "VER", "Max Verstappen", "Red Bull Racing", 0x4781D7)
    private val lec = LiveDriver(16, "LEC", "Charles Leclerc", "Ferrari", 0xED1131)
    private fun at(s: Long) = t0.plusSeconds(s)

    private fun lap(s: Long, driver: Int, n: Int, time: Double?) =
        TimingEvent.Lap(at(s), driver, n, time, listOf(30.0, 40.0, time?.minus(70)), isPitOutLap = false)

    private fun rc(s: Long, message: String, flag: String? = null, scope: String? = null) =
        TimingEvent.RaceControl(at(s), if (flag != null) "Flag" else "SafetyCar", flag, scope, message, null)

    private fun reduce(vararg events: TimingEvent) =
        events.fold(LiveTimingReducer.initial(session, listOf(ver, lec)), LiveTimingReducer::reduce)

    @Test
    fun `tower follows positions and keeps best and fastest laps`() {
        val state = reduce(
            TimingEvent.Position(at(0), 16, 1), TimingEvent.Position(at(0), 3, 2),
            lap(100, 16, 1, 101.2), lap(101, 3, 1, 100.8),
            lap(200, 16, 2, 99.9), lap(201, 3, 2, 102.0),
            TimingEvent.Position(at(205), 3, 1), TimingEvent.Position(at(205), 16, 2),
        )
        assertEquals(listOf("VER", "LEC"), state.tower.map { it.driver.acronym })
        assertEquals(100.8, state.timings.getValue(3).bestLap!!, 0.0)
        assertEquals(102.0, state.timings.getValue(3).lastLap!!, 0.0)
        assertEquals(16 to 99.9, state.fastestLap)
        assertEquals(2, state.leaderLap)
        assertEquals(at(205), state.clock)
    }

    @Test
    fun `an incomplete lap updates sectors without counting the lap`() {
        val state = reduce(lap(100, 3, 1, 100.0), lap(150, 3, 2, null))
        assertEquals(1, state.timings.getValue(3).lapsCompleted)
        assertEquals(100.0, state.timings.getValue(3).lastLap!!, 0.0)
    }

    @Test
    fun `tyre age counts laps since the stint started`() {
        val state = reduce(
            TimingEvent.StintUpdate(at(0), 3, Stint(1, TyreCompound.MEDIUM, 1, 20, 0)),
            TimingEvent.StintUpdate(at(0), 3, Stint(2, TyreCompound.HARD, 21, null, 2)),
            *(1..24).map { lap(it * 100L, 3, it, 100.0) }.toTypedArray(),
        )
        val timing = state.timings.getValue(3)
        assertEquals(TyreCompound.HARD, timing.currentStint?.compound)
        // Driving lap 25 on a hard fitted at lap 21 with 2 laps of age: 2 + 4.
        assertEquals(6, timing.tyreAge)
    }

    @Test
    fun `VSC, safety car and red flag drive the track status with both wordings`() {
        assertEquals(TrackStatus.VSC, reduce(rc(1, "VSC DEPLOYED")).trackStatus)
        assertEquals(TrackStatus.VSC, reduce(rc(1, "VIRTUAL SAFETY CAR DEPLOYED")).trackStatus)
        assertEquals(TrackStatus.SAFETY_CAR, reduce(rc(1, "SAFETY CAR DEPLOYED")).trackStatus)
        assertEquals(TrackStatus.SAFETY_CAR, reduce(rc(1, "SAFETY CAR DEPLOYED"), rc(2, "SAFETY CAR IN THIS LAP")).trackStatus)
        assertEquals(TrackStatus.GREEN, reduce(rc(1, "VSC DEPLOYED"), rc(2, "VSC ENDING"), rc(3, "TRACK CLEAR", "CLEAR", "Track")).trackStatus)
        assertEquals(TrackStatus.RED, reduce(rc(1, "RED FLAG", "RED", "Track")).trackStatus)
    }

    @Test
    fun `clearing one yellow sector keeps the track yellow while another is still yellow`() {
        val yellow = reduce(
            rc(1, "YELLOW IN TRACK SECTOR 5", "YELLOW", "Sector"),
            rc(1, "YELLOW IN TRACK SECTOR 6", "YELLOW", "Sector"),
            rc(2, "CLEAR IN TRACK SECTOR 6", "CLEAR", "Sector"),
        )
        assertEquals(TrackStatus.YELLOW, yellow.trackStatus)
        val cleared = LiveTimingReducer.reduce(yellow, rc(3, "CLEAR IN TRACK SECTOR 5", "CLEAR", "Sector"))
        assertEquals(TrackStatus.GREEN, cleared.trackStatus)
    }

    @Test
    fun `replay applies grid positions at the start and can seek backwards`() {
        val events = listOf(
            TimingEvent.Position(t0.minusSeconds(3000), 3, 1),
            TimingEvent.Position(t0.minusSeconds(3000), 16, 2),
            TimingEvent.Position(at(600), 16, 1),
            TimingEvent.Position(at(600), 3, 2),
        )
        val timeline = ReplayTimeline.build(session, listOf(ver, lec), events)
        assertEquals(listOf(3, 16), timeline.advanceTo(timeline.start).tower.map { it.driver.number })
        assertEquals(listOf(16, 3), timeline.advanceTo(at(700)).tower.map { it.driver.number })
        assertEquals(listOf(3, 16), timeline.advanceTo(at(100)).tower.map { it.driver.number })
    }

    @Test
    fun `OpenF1 payloads map to events, including textual gaps`() {
        val intervals = decodeFixture<List<OpenF1IntervalDto>>("openf1_intervals.json").map { it.toEvent() }
        assertTrue(intervals[0].gapToLeader is Gap.Seconds)
        assertEquals(Gap.Text("+1 LAP"), intervals[2].gapToLeader)

        val laps = decodeFixture<List<OpenF1LapDto>>("openf1_laps.json")
        val lap2 = laps.first { it.lapNumber == 2 }.toEvent(fallback = Instant.EPOCH)!!
        assertEquals(laps.first { it.lapNumber == 2 }.lapDuration, lap2.duration)

        val session = decodeFixture<List<OpenF1SessionDto>>("openf1_session.json").single().toDomain()
        assertEquals("Singapore", session.circuit)
        assertTrue(session.isRace)

        val drivers = decodeFixture<List<OpenF1DriverDto>>("openf1_drivers.json").map { it.toDomain() }
        assertTrue(drivers.all { it.teamColour != null && it.acronym.length == 3 })

        val state = decodeFixture<List<OpenF1RaceControlDto>>("openf1_race_control.json").map { it.toEvent() }
            .fold(LiveTimingReducer.initial(session, drivers), LiveTimingReducer::reduce)
        assertEquals(14, state.raceControl.size)
    }

    @Test
    fun `live feed messages are parsed by topic and unknown topics ignored`() {
        val stream = OpenF1LiveStream(OkHttpClient(), testJson) { t0 }
        val event = stream.parse(
            "v1/position",
            """{"date":"2026-10-11T12:33:37.580000+00:00","session_key":11388,"driver_number":1,"position":4,"meeting_key":1296,"_key":"x","_id":1}""",
        )
        assertEquals(TimingEvent.Position(Instant.parse("2026-10-11T12:33:37.580Z"), 1, 4), event)
        assertNull(stream.parse("v1/location", "{}"))
        assertNull(stream.parse("v1/position", "not json"))
    }
}
