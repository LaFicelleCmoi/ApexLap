import ApexLapDomain
import Foundation

public struct LiveSession: Hashable, Sendable, Identifiable {
    public let key: Int
    public let name: String
    /// OpenF1 `session_type`: Race, Qualifying, Practice. Sprints have type Race / Qualifying.
    public let type: String
    public let start: Date
    public let end: Date
    public let circuit: String
    public let country: String

    public var id: Int { key }
    public var isRace: Bool { type == "Race" }
    public func isLive(at now: Date) -> Bool { now >= start.addingTimeInterval(-15 * 60) && now < end.addingTimeInterval(30 * 60) }
    public func hasEnded(at now: Date) -> Bool { now > end }

    public init(key: Int, name: String, type: String, start: Date, end: Date, circuit: String, country: String) {
        self.key = key
        self.name = name
        self.type = type
        self.start = start
        self.end = end
        self.circuit = circuit
        self.country = country
    }

    /// Session name in the app language ("Race" → "Course" / "Race" / "Carrera").
    public func localizedName(_ strings: Strings) -> String { strings.openF1Session(name) }
}

public struct LiveDriver: Hashable, Sendable, Identifiable {
    public let number: Int
    public let acronym: String
    public let fullName: String
    public let team: String
    /// RRGGBB from OpenF1.
    public let teamColour: UInt32?

    public var id: Int { number }

    public init(number: Int, acronym: String, fullName: String, team: String, teamColour: UInt32?) {
        self.number = number
        self.acronym = acronym
        self.fullName = fullName
        self.team = team
        self.teamColour = teamColour
    }
}

public enum TyreCompound: String, Sendable, CaseIterable {
    case soft = "SOFT", medium = "MEDIUM", hard = "HARD", intermediate = "INTERMEDIATE", wet = "WET", unknown = "UNKNOWN"

    public init(apiValue: String?) { self = TyreCompound(rawValue: apiValue?.uppercased() ?? "") ?? .unknown }

    public var letter: String {
        switch self {
        case .soft: "S"
        case .medium: "M"
        case .hard: "H"
        case .intermediate: "I"
        case .wet: "W"
        case .unknown: "?"
        }
    }
}

public struct Stint: Hashable, Sendable {
    public let number: Int
    public let compound: TyreCompound
    public let lapStart: Int
    public let lapEnd: Int?
    public let tyreAgeAtStart: Int

    public init(number: Int, compound: TyreCompound, lapStart: Int, lapEnd: Int?, tyreAgeAtStart: Int) {
        self.number = number
        self.compound = compound
        self.lapStart = lapStart
        self.lapEnd = lapEnd
        self.tyreAgeAtStart = tyreAgeAtStart
    }
}

public enum TrackStatus: String, Sendable {
    case green, yellow, vsc, safetyCar, red, chequered

    public func label(_ language: AppLanguage) -> String {
        switch self {
        case .green: L("Piste dégagée", "Track clear", "Pista despejada")[language]
        case .yellow: L("Drapeau jaune", "Yellow flag", "Bandera amarilla")[language]
        case .vsc: "Virtual Safety Car"
        case .safetyCar: "Safety Car"
        case .red: L("Drapeau rouge", "Red flag", "Bandera roja")[language]
        case .chequered: L("Drapeau à damier", "Chequered flag", "Bandera a cuadros")[language]
        }
    }
}

public enum Gap: Hashable, Sendable {
    case seconds(Double)
    case text(String)

    public func display(_ strings: Strings) -> String {
        switch self {
        case let .seconds(value): value == 0 ? strings.leaderGap : String(format: "+%.3f", value)
        case let .text(value): value.replacingOccurrences(of: "LAPS", with: strings.lapsWord).replacingOccurrences(of: "LAP", with: strings.lapWord)
        }
    }
}

public enum TimingEvent: Hashable, Sendable {
    case position(date: Date, driver: Int, position: Int)
    case interval(date: Date, driver: Int, gapToLeader: Gap?, interval: Gap?)
    /// Emitted when a lap is completed (or updated live, sector by sector, with a nil duration).
    case lap(date: Date, driver: Int, lapNumber: Int, duration: Double?, sectors: [Double?])
    case stint(date: Date, driver: Int, stint: Stint)
    case raceControl(date: Date, flag: String?, scope: String?, message: String, lap: Int?)
    case pitStop(date: Date, driver: Int, lap: Int?)

    public var date: Date {
        switch self {
        case let .position(date, _, _), let .interval(date, _, _, _), let .lap(date, _, _, _, _),
             let .stint(date, _, _), let .raceControl(date, _, _, _, _), let .pitStop(date, _, _):
            date
        }
    }
}

public struct DriverTiming: Hashable, Sendable, Identifiable {
    public var driver: LiveDriver
    public var position: Int?
    public var gapToLeader: Gap?
    public var interval: Gap?
    public var lapsCompleted = 0
    public var lastLap: Double?
    public var bestLap: Double?
    public var lastSectors: [Double?] = [nil, nil, nil]
    public var pitStops = 0
    public var stints: [Stint] = []

    public var id: Int { driver.number }

    public init(driver: LiveDriver) { self.driver = driver }

    /// Tyre currently fitted: latest stint that started on or before the lap being driven.
    public var currentStint: Stint? { stints.filter { $0.lapStart <= lapsCompleted + 1 }.max { $0.number < $1.number } }
    public var tyreAge: Int? { currentStint.map { $0.tyreAgeAtStart + max(0, lapsCompleted + 1 - $0.lapStart) } }
}

public struct RaceControlEntry: Hashable, Sendable, Identifiable {
    public let date: Date
    public let message: String
    public let lap: Int?
    public var id: String { "\(date.timeIntervalSince1970)-\(message)" }
}

public struct LiveTimingState: Hashable, Sendable {
    public var session: LiveSession?
    public var timings: [Int: DriverTiming] = [:]
    public var raceControl: [RaceControlEntry] = []
    /// Track-wide condition, ignoring local yellows.
    public var incident: TrackStatus = .green
    public var yellowSectors: Set<Int> = []
    public var clock: Date?
    public var fastestLap: (driver: Int, time: Double)? {
        get { fastestDriver.map { ($0, fastestTime ?? 0) } }
        set { fastestDriver = newValue?.driver; fastestTime = newValue?.time }
    }
    private var fastestDriver: Int?
    private var fastestTime: Double?

    public init(session: LiveSession? = nil, drivers: [LiveDriver] = []) {
        self.session = session
        timings = Dictionary(uniqueKeysWithValues: drivers.map { ($0.number, DriverTiming(driver: $0)) })
    }

    public var trackStatus: TrackStatus { incident == .green && !yellowSectors.isEmpty ? .yellow : incident }
    public var leaderLap: Int { timings.values.map(\.lapsCompleted).max() ?? 0 }

    /// Tower order: official position first, then best lap (qualifying/practice), then car number.
    public var tower: [DriverTiming] {
        timings.values.sorted {
            ($0.position ?? .max, $0.bestLap ?? .greatestFiniteMagnitude, $0.driver.number)
                < ($1.position ?? .max, $1.bestLap ?? .greatestFiniteMagnitude, $1.driver.number)
        }
    }
}
