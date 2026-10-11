package com.apexlap.live

import com.apexlap.domain.telemetry.CarSample
import com.apexlap.domain.telemetry.CompareLapsUseCase
import com.apexlap.domain.telemetry.LapRef
import com.apexlap.domain.telemetry.LapTrace
import com.apexlap.domain.telemetry.MapTrackUseCase
import com.apexlap.domain.telemetry.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TelemetryTest {

    private val t0 = Instant.parse("2026-10-11T12:18:54Z")

    /** A lap at constant [kmh] sampled every 0.25 s for [seconds]. */
    private fun lap(driver: Int, kmh: (Double) -> Double, seconds: Double): LapTrace {
        val ref = LapRef(driver, 10, t0, seconds, listOf(null, null, null))
        val samples = generateSequence(0.0) { it + 0.25 }.takeWhile { it <= seconds }.map { t ->
            CarSample(t0.plusMillis((t * 1000).toLong()), kmh(t), 100.0, 0.0, 7)
        }.toList()
        return LapTrace.build(ref, samples)!!
    }

    @Test
    fun `distance is integrated from speed`() {
        // 180 km/h = 50 m/s for 20 s.
        assertEquals(1000.0, lap(1, { 180.0 }, 20.0).length, 1e-6)
    }

    @Test
    fun `too few samples give no trace`() {
        val ref = LapRef(1, 1, t0, 90.0, listOf(null, null, null))
        assertNull(LapTrace.build(ref, listOf(CarSample(t0, 200.0, 100.0, 0.0, 7))))
    }

    @Test
    fun `identical laps have zero delta and A wins ties`() {
        val a = lap(1, { 180.0 }, 20.0)
        val comparison = CompareLapsUseCase()(a, lap(2, { 180.0 }, 20.0))
        assertTrue(comparison.delta.all { kotlin.math.abs(it) < 1e-9 })
        assertTrue(comparison.miniSectorWinners.all { it == 0 })
    }

    @Test
    fun `delta grows where the second driver is slower`() {
        // B is slower over the second half: A gains time only there.
        val a = lap(1, { 180.0 }, 20.0)
        val b = lap(2, { t -> if (t < 10) 180.0 else 150.0 }, 22.0)
        val comparison = CompareLapsUseCase()(a, b, miniSectors = 4)
        val mid = comparison.delta[comparison.distance.indexOfFirst { it >= 400 }]
        assertTrue("delta before the slow part stays near zero, was $mid", kotlin.math.abs(mid) < 0.25)
        assertTrue(comparison.delta.last() > 1.5)
        assertEquals(0, comparison.miniSectorWinners.last())
    }

    @Test
    fun `track points map to lap fractions`() {
        val trace = lap(1, { 180.0 }, 20.0)
        val points = (0..20).map { TrackPoint(t0.plusSeconds(it.toLong()), it * 10.0, 0.0) }
        val mapped = MapTrackUseCase()(trace, points)
        assertEquals(0.0, mapped.first().fraction, 1e-6)
        assertEquals(0.5, mapped[10].fraction, 0.02)
        assertEquals(1.0, mapped.last().fraction, 1e-6)
    }
}
