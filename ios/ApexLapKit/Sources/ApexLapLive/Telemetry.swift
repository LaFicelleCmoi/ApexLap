import Foundation

/// One OpenF1 `car_data` sample (~3.7 Hz). Brake is 0 or 100.
public struct CarSample: Hashable, Sendable {
    public let date: Date
    public let speed: Double
    public let throttle: Double
    public let brake: Double
    public let gear: Int

    public init(date: Date, speed: Double, throttle: Double, brake: Double, gear: Int) {
        self.date = date
        self.speed = speed
        self.throttle = throttle
        self.brake = brake
        self.gear = gear
    }
}

/// One OpenF1 `location` sample, track coordinates in decimetres.
public struct TrackPoint: Hashable, Sendable {
    public let date: Date
    public let x: Double
    public let y: Double

    public init(date: Date, x: Double, y: Double) {
        self.date = date
        self.x = x
        self.y = y
    }
}

public struct LapRef: Hashable, Sendable, Identifiable {
    public let driver: Int
    public let lapNumber: Int
    public let start: Date
    public let duration: Double
    public let sectors: [Double?]

    public var id: String { "\(driver)-\(lapNumber)" }

    public init(driver: Int, lapNumber: Int, start: Date, duration: Double, sectors: [Double?]) {
        self.driver = driver
        self.lapNumber = lapNumber
        self.start = start
        self.duration = duration
        self.sectors = sectors
    }
}

/// Telemetry of one lap against elapsed time (s) and travelled distance (m).
public struct LapTrace: Sendable {
    public let lap: LapRef
    public let time: [Double]
    public let distance: [Double]
    public let speed: [Double]
    public let throttle: [Double]
    public let brake: [Double]

    public var length: Double { distance.last ?? 0 }

    /// OpenF1 has no distance channel, so distance is integrated from speed (trapezoidal rule), like FastF1.
    public static func build(lap: LapRef, samples: [CarSample]) -> LapTrace? {
        let end = lap.start.addingTimeInterval(lap.duration)
        let inLap = samples.filter { $0.date >= lap.start && $0.date <= end }.sorted { $0.date < $1.date }
        guard inLap.count >= 10 else { return nil }
        let time = inLap.map { $0.date.timeIntervalSince(lap.start) }
        var distance = [0.0]
        for i in 1..<inLap.count {
            distance.append(distance[i - 1] + (inLap[i].speed + inLap[i - 1].speed) / 2 / 3.6 * (time[i] - time[i - 1]))
        }
        return LapTrace(lap: lap, time: time, distance: distance, speed: inLap.map(\.speed),
                        throttle: inLap.map(\.throttle), brake: inLap.map { min(100, max(0, $0.brake)) })
    }
}

public struct LapComparison: Sendable {
    public let distance: [Double]
    public let speedA: [Double]
    public let speedB: [Double]
    public let throttleA: [Double]
    public let throttleB: [Double]
    public let brakeA: [Double]
    public let brakeB: [Double]
    /// Time B − time A at each distance: positive when driver A is ahead.
    public let delta: [Double]
    /// Per mini-sector: 0 if A was quicker through it, 1 if B was.
    public let miniSectorWinners: [Int]
}

public enum Telemetry {
    public static func compare(_ a: LapTrace, _ b: LapTrace, step: Double = 10, miniSectors: Int = 25) -> LapComparison {
        // Integrated lengths differ by ~1 %; scale both onto their mean so the delta is not skewed.
        let length = (a.length + b.length) / 2
        let distA = a.distance.map { $0 * length / a.length }
        let distB = b.distance.map { $0 * length / b.length }
        let grid = stride(from: 0.0, through: length, by: step).map { $0 }
        func resample(_ xs: [Double], _ ys: [Double]) -> [Double] { grid.map { interpolate(xs, ys, $0) } }
        let timeA = resample(distA, a.time)
        let timeB = resample(distB, b.time)
        let bounds = (0...miniSectors).map { length * Double($0) / Double(miniSectors) }
        let winners = (0..<miniSectors).map { i -> Int in
            let durA = interpolate(distA, a.time, bounds[i + 1]) - interpolate(distA, a.time, bounds[i])
            let durB = interpolate(distB, b.time, bounds[i + 1]) - interpolate(distB, b.time, bounds[i])
            return durA <= durB ? 0 : 1
        }
        return LapComparison(
            distance: grid,
            speedA: resample(distA, a.speed), speedB: resample(distB, b.speed),
            throttleA: resample(distA, a.throttle), throttleB: resample(distB, b.throttle),
            brakeA: resample(distA, a.brake), brakeB: resample(distB, b.brake),
            delta: zip(timeB, timeA).map { $0 - $1 },
            miniSectorWinners: winners
        )
    }

    public struct MappedPoint: Hashable, Sendable {
        public let x: Double
        public let y: Double
        public let fraction: Double
    }

    /// Projects a lap's location samples onto lap fraction (0…1) to colour the track by mini-sector.
    public static func mapTrack(_ trace: LapTrace, points: [TrackPoint]) -> [MappedPoint] {
        points.sorted { $0.date < $1.date }.compactMap { p in
            let t = p.date.timeIntervalSince(trace.lap.start)
            guard t >= 0, t <= trace.lap.duration else { return nil }
            return MappedPoint(x: p.x, y: p.y, fraction: min(1, max(0, interpolate(trace.time, trace.distance, t) / trace.length)))
        }
    }

    /// Linear interpolation of y at x over ascending xs (clamped at the ends).
    static func interpolate(_ xs: [Double], _ ys: [Double], _ x: Double) -> Double {
        guard let first = xs.first, let last = xs.last else { return 0 }
        if x <= first { return ys[0] }
        if x >= last { return ys[ys.count - 1] }
        var lo = 0, hi = xs.count - 1
        while hi - lo > 1 {
            let mid = (lo + hi) / 2
            if xs[mid] <= x { lo = mid } else { hi = mid }
        }
        let span = xs[hi] - xs[lo]
        return span == 0 ? ys[lo] : ys[lo] + (ys[hi] - ys[lo]) * (x - xs[lo]) / span
    }
}
