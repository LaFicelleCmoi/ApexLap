package com.apexlap.domain.telemetry

import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** One OpenF1 `car_data` sample (~3.7 Hz). Brake is 0 or 100. */
data class CarSample(val date: Instant, val speedKmh: Double, val throttle: Double, val brake: Double, val gear: Int)

/** One OpenF1 `location` sample, track coordinates in decimetres. */
data class TrackPoint(val date: Instant, val x: Double, val y: Double)

data class LapRef(val driver: Int, val lapNumber: Int, val start: Instant, val duration: Double, val sectors: List<Double?>)

/** Telemetry of one lap as functions of elapsed time (s) and travelled distance (m). */
class LapTrace(
    val lap: LapRef,
    val time: DoubleArray,
    val distance: DoubleArray,
    val speed: DoubleArray,
    val throttle: DoubleArray,
    val brake: DoubleArray,
    val gear: DoubleArray,
) {
    val length: Double get() = distance.lastOrNull() ?: 0.0

    companion object {
        /**
         * OpenF1 has no distance channel, so distance is integrated from speed (trapezoidal rule), the same
         * approach FastF1 uses. Samples are clipped to the lap window.
         */
        fun build(lap: LapRef, samples: List<CarSample>): LapTrace? {
            val end = lap.start.plusMillis((lap.duration * 1000).toLong())
            val inLap = samples.filter { !it.date.isBefore(lap.start) && !it.date.isAfter(end) }.sortedBy { it.date }
            if (inLap.size < 10) return null
            val time = DoubleArray(inLap.size) { seconds(lap.start, inLap[it].date) }
            val distance = DoubleArray(inLap.size)
            for (i in 1 until inLap.size) {
                val dt = time[i] - time[i - 1]
                distance[i] = distance[i - 1] + (inLap[i].speedKmh + inLap[i - 1].speedKmh) / 2 / 3.6 * dt
            }
            return LapTrace(
                lap, time, distance,
                speed = DoubleArray(inLap.size) { inLap[it].speedKmh },
                throttle = DoubleArray(inLap.size) { inLap[it].throttle },
                brake = DoubleArray(inLap.size) { inLap[it].brake.coerceIn(0.0, 100.0) },
                gear = DoubleArray(inLap.size) { inLap[it].gear.toDouble() },
            )
        }

        private fun seconds(from: Instant, to: Instant) = Duration.between(from, to).toNanos() / 1e9
    }
}

data class LapComparison(
    /** Common distance grid in metres. */
    val distance: List<Double>,
    val speedA: List<Double>,
    val speedB: List<Double>,
    val throttleA: List<Double>,
    val throttleB: List<Double>,
    val brakeA: List<Double>,
    val brakeB: List<Double>,
    /** Time B − time A at each distance: positive when driver A is ahead. */
    val delta: List<Double>,
    /** Per mini-sector: 0 if A was quicker through it, 1 if B was. */
    val miniSectorWinners: List<Int>,
    val lapTimeA: Double,
    val lapTimeB: Double,
)

class CompareLapsUseCase @Inject constructor() {

    operator fun invoke(a: LapTrace, b: LapTrace, stepMeters: Double = 10.0, miniSectors: Int = 25): LapComparison {
        // Integrated lengths differ by ~1 %; scale both onto their mean so the delta is not skewed.
        val length = (a.length + b.length) / 2
        val scaleA = length / a.length
        val scaleB = length / b.length
        val distA = DoubleArray(a.distance.size) { a.distance[it] * scaleA }
        val distB = DoubleArray(b.distance.size) { b.distance[it] * scaleB }
        val grid = generateSequence(0.0) { it + stepMeters }.takeWhile { it <= length }.toList()

        fun resample(xs: DoubleArray, ys: DoubleArray) = grid.map { interpolate(xs, ys, it) }

        val timeA = resample(distA, a.time)
        val timeB = resample(distB, b.time)
        val boundaries = (0..miniSectors).map { length * it / miniSectors }
        val winners = (0 until miniSectors).map { i ->
            val durA = interpolate(distA, a.time, boundaries[i + 1]) - interpolate(distA, a.time, boundaries[i])
            val durB = interpolate(distB, b.time, boundaries[i + 1]) - interpolate(distB, b.time, boundaries[i])
            if (durA <= durB) 0 else 1
        }
        return LapComparison(
            distance = grid,
            speedA = resample(distA, a.speed),
            speedB = resample(distB, b.speed),
            throttleA = resample(distA, a.throttle),
            throttleB = resample(distB, b.throttle),
            brakeA = resample(distA, a.brake),
            brakeB = resample(distB, b.brake),
            delta = grid.indices.map { timeB[it] - timeA[it] },
            miniSectorWinners = winners,
            lapTimeA = a.lap.duration,
            lapTimeB = b.lap.duration,
        )
    }
}

/**
 * Projects a lap's location samples onto lap fraction (0..1) using the time → distance mapping of its
 * trace, so the track map can be coloured by mini-sector.
 */
class MapTrackUseCase @Inject constructor() {
    data class MappedPoint(val x: Double, val y: Double, val fraction: Double)

    operator fun invoke(trace: LapTrace, points: List<TrackPoint>): List<MappedPoint> {
        val start = trace.lap.start
        return points.sortedBy { it.date }.mapNotNull { p ->
            val t = Duration.between(start, p.date).toNanos() / 1e9
            if (t < 0 || t > trace.lap.duration) return@mapNotNull null
            MappedPoint(p.x, p.y, (interpolate(trace.time, trace.distance, t) / trace.length).coerceIn(0.0, 1.0))
        }
    }
}

/** Linear interpolation of y at x over ascending xs (clamped at the ends). */
internal fun interpolate(xs: DoubleArray, ys: DoubleArray, x: Double): Double {
    if (x <= xs.first()) return ys.first()
    if (x >= xs.last()) return ys.last()
    var lo = 0
    var hi = xs.size - 1
    while (hi - lo > 1) {
        val mid = (lo + hi) / 2
        if (xs[mid] <= x) lo = mid else hi = mid
    }
    val span = xs[hi] - xs[lo]
    if (span == 0.0) return ys[lo]
    return ys[lo] + (ys[hi] - ys[lo]) * (x - xs[lo]) / span
}
