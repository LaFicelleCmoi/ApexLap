import Foundation

/// Pure state machine shared by the replay and the live WebSocket stream (mirrors Android's reducer).
public enum LiveTimingReducer {
    static let raceControlHistory = 60

    public static func reduce(_ state: LiveTimingState, _ event: TimingEvent) -> LiveTimingState {
        var next = state
        if next.clock.map({ event.date > $0 }) ?? true { next.clock = event.date }
        switch event {
        case let .position(_, driver, position):
            next.update(driver) { $0.position = position }
        case let .interval(_, driver, gap, interval):
            next.update(driver) { $0.gapToLeader = gap; $0.interval = interval }
        case let .lap(_, driver, lapNumber, duration, sectors):
            let valid = duration.flatMap { $0 > 0 ? $0 : nil }
            next.update(driver) { timing in
                if let valid {
                    timing.lapsCompleted = max(timing.lapsCompleted, lapNumber)
                    timing.lastLap = valid
                    timing.bestLap = min(timing.bestLap ?? valid, valid)
                }
                timing.lastSectors = sectors
            }
            if let best = next.timings[driver]?.bestLap, best < (next.fastestLap?.time ?? .greatestFiniteMagnitude) {
                next.fastestLap = (driver, best)
            }
        case let .stint(_, driver, stint):
            next.update(driver) { timing in
                timing.stints = (timing.stints.filter { $0.number != stint.number } + [stint]).sorted { $0.number < $1.number }
            }
        case .pitStop(_, let driver, _):
            next.update(driver) { $0.pitStops += 1 }
        case let .raceControl(date, flag, scope, message, lap):
            next = applyRaceControl(next, flag: flag, scope: scope, message: message)
            next.raceControl = Array(([RaceControlEntry(date: date, message: message, lap: lap)] + next.raceControl).prefix(raceControlHistory))
        }
        return next
    }

    /// Wording varies by season ("VSC DEPLOYED" in 2026, "VIRTUAL SAFETY CAR DEPLOYED" in 2025); sector yellows
    /// are tracked individually so clearing one sector keeps the others yellow.
    static func applyRaceControl(_ state: LiveTimingState, flag rawFlag: String?, scope: String?, message rawMessage: String) -> LiveTimingState {
        var next = state
        let message = rawMessage.uppercased()
        let flag = rawFlag?.uppercased()
        let sector = message.range(of: #"SECTOR (\d+)"#, options: .regularExpression)
            .flatMap { Int(message[$0].split(separator: " ").last ?? "") }
        if flag == "RED" {
            next.incident = .red
        } else if flag == "CHEQUERED" {
            next.incident = .chequered
            next.yellowSectors = []
        } else if message.contains("VSC DEPLOYED") || message.contains("VIRTUAL SAFETY CAR DEPLOYED") {
            next.incident = .vsc
        } else if message.contains("SAFETY CAR DEPLOYED") {
            next.incident = .safetyCar
        } else if (flag == "GREEN" || flag == "CLEAR") && scope == "Track" {
            next.incident = .green
            next.yellowSectors = []
        } else if let sector, flag == "YELLOW" || flag == "DOUBLE YELLOW" {
            next.yellowSectors.insert(sector)
        } else if let sector, flag == "CLEAR" {
            next.yellowSectors.remove(sector)
        }
        return next
    }
}

extension LiveTimingState {
    mutating func update(_ driver: Int, _ transform: (inout DriverTiming) -> Void) {
        var timing = timings[driver] ?? DriverTiming(driver: LiveDriver(number: driver, acronym: "#\(driver)", fullName: "Pilote \(driver)", team: "", teamColour: nil))
        transform(&timing)
        timings[driver] = timing
    }
}

/// A recorded session as a time-ordered event list, replayable forward and seekable backwards.
public final class ReplayTimeline: @unchecked Sendable {
    public let start: Date
    public let end: Date
    private let initial: LiveTimingState
    private let events: [TimingEvent]
    private var cursor = 0
    private var position = Date.distantPast
    public private(set) var state: LiveTimingState

    public init(initial: LiveTimingState, events: [TimingEvent], start: Date, end: Date) {
        self.initial = initial
        self.events = events.sorted { $0.date < $1.date }
        self.start = start
        self.end = end
        state = initial
    }

    /// Positions recorded before the start (the grid) are kept so the tower is ordered from the first frame.
    public static func build(session: LiveSession, drivers: [LiveDriver], events: [TimingEvent]) -> ReplayTimeline {
        let first = events.map(\.date).min() ?? session.start
        let start = first < session.start ? session.start.addingTimeInterval(-60) : first
        let end = max(session.end, events.map(\.date).max() ?? session.end)
        return ReplayTimeline(initial: LiveTimingState(session: session, drivers: drivers), events: events, start: start, end: end)
    }

    @discardableResult
    public func advance(to time: Date) -> LiveTimingState {
        if time < position {
            cursor = 0
            state = initial
        }
        var current = state
        while cursor < events.count, events[cursor].date <= time {
            current = LiveTimingReducer.reduce(current, events[cursor])
            cursor += 1
        }
        position = time
        current.clock = time
        state = current
        return current
    }
}
