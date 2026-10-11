import Foundation

public enum SessionType: String, Codable, Sendable, CaseIterable {
    case practice1, practice2, practice3, sprintQualifying, sprint, qualifying, race

    public var label: String {
        switch self {
        case .practice1: "Essais libres 1"
        case .practice2: "Essais libres 2"
        case .practice3: "Essais libres 3"
        case .sprintQualifying: "Qualifications sprint"
        case .sprint: "Sprint"
        case .qualifying: "Qualifications"
        case .race: "Course"
        }
    }

    public var shortLabel: String {
        switch self {
        case .practice1: "EL1"
        case .practice2: "EL2"
        case .practice3: "EL3"
        case .sprintQualifying: "SQ"
        case .sprint: "SPR"
        case .qualifying: "Q"
        case .race: "GP"
        }
    }
}

public struct Session: Codable, Hashable, Sendable {
    public let type: SessionType
    /// Nil when the official time is not yet published (only the day is known).
    public let startsAt: Date?
    /// Midnight UTC of the session day.
    public let day: Date

    public init(type: SessionType, startsAt: Date?, day: Date) {
        self.type = type
        self.startsAt = startsAt
        self.day = day
    }
}

public struct Circuit: Codable, Hashable, Sendable {
    public let id: String
    public let name: String
    public let locality: String
    public let country: String

    public init(id: String, name: String, locality: String, country: String) {
        self.id = id
        self.name = name
        self.locality = locality
        self.country = country
    }
}

public struct Race: Codable, Hashable, Sendable, Identifiable {
    public static let raceDuration: TimeInterval = 2 * 60 * 60

    public let season: Int
    public let round: Int
    public let name: String
    public let circuit: Circuit
    public let sessions: [Session]

    public var id: String { "\(season)-\(round)" }
    public var raceSession: Session? { sessions.first { $0.type == .race } }
    public var isSprintWeekend: Bool { sessions.contains { $0.type == .sprint } }

    public init(season: Int, round: Int, name: String, circuit: Circuit, sessions: [Session]) {
        self.season = season
        self.round = round
        self.name = name
        self.circuit = circuit
        self.sessions = sessions
    }

    public func isFinished(at now: Date) -> Bool {
        guard let race = raceSession else { return false }
        return now > (race.startsAt ?? race.day).addingTimeInterval(Self.raceDuration)
    }
}

public struct Driver: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let code: String
    public let number: String?
    public let givenName: String
    public let familyName: String
    public let nationality: String
    public let dateOfBirth: Date?

    public var fullName: String { "\(givenName) \(familyName)" }

    public init(id: String, code: String, number: String?, givenName: String, familyName: String,
                nationality: String, dateOfBirth: Date?) {
        self.id = id
        self.code = code
        self.number = number
        self.givenName = givenName
        self.familyName = familyName
        self.nationality = nationality
        self.dateOfBirth = dateOfBirth
    }
}

public struct Constructor: Codable, Hashable, Sendable, Identifiable {
    public let id: String
    public let name: String
    public let nationality: String

    public init(id: String, name: String, nationality: String) {
        self.id = id
        self.name = name
        self.nationality = nationality
    }
}

public struct DriverStanding: Codable, Hashable, Sendable, Identifiable {
    public let position: Int
    public let points: Double
    public let wins: Int
    public let driver: Driver
    public let constructor: Constructor?

    public var id: String { driver.id }

    public init(position: Int, points: Double, wins: Int, driver: Driver, constructor: Constructor?) {
        self.position = position
        self.points = points
        self.wins = wins
        self.driver = driver
        self.constructor = constructor
    }
}

public struct ConstructorStanding: Codable, Hashable, Sendable, Identifiable {
    public let position: Int
    public let points: Double
    public let wins: Int
    public let constructor: Constructor

    public var id: String { constructor.id }

    public init(position: Int, points: Double, wins: Int, constructor: Constructor) {
        self.position = position
        self.points = points
        self.wins = wins
        self.constructor = constructor
    }
}

public struct RaceResult: Codable, Hashable, Sendable, Identifiable {
    public let position: Int
    public let positionText: String
    public let points: Double
    public let grid: Int
    public let laps: Int
    public let status: String
    public let time: String?
    public let fastestLapRank: Int?
    public let fastestLapTime: String?
    public let driver: Driver
    public let constructor: Constructor

    public var id: String { driver.id }
    public var hasFastestLap: Bool { fastestLapRank == 1 }
    /// Positive = places gained from the grid. Pit-lane starts (grid 0) are ignored.
    public var positionsGained: Int? { grid > 0 && position > 0 ? grid - position : nil }

    public init(position: Int, positionText: String, points: Double, grid: Int, laps: Int, status: String,
                time: String?, fastestLapRank: Int?, fastestLapTime: String?, driver: Driver, constructor: Constructor) {
        self.position = position
        self.positionText = positionText
        self.points = points
        self.grid = grid
        self.laps = laps
        self.status = status
        self.time = time
        self.fastestLapRank = fastestLapRank
        self.fastestLapTime = fastestLapTime
        self.driver = driver
        self.constructor = constructor
    }
}

public struct CareerStats: Codable, Hashable, Sendable {
    public let seasons: Int
    public let races: Int
    public let wins: Int
    public let podiums: Int
    public let poles: Int

    public init(seasons: Int, races: Int, wins: Int, podiums: Int, poles: Int) {
        self.seasons = seasons
        self.races = races
        self.wins = wins
        self.podiums = podiums
        self.poles = poles
    }
}

public struct DriverProfile: Codable, Hashable, Sendable {
    public let driver: Driver
    public let stats: CareerStats

    public init(driver: Driver, stats: CareerStats) {
        self.driver = driver
        self.stats = stats
    }
}

public struct ConstructorProfile: Codable, Hashable, Sendable {
    public let constructor: Constructor
    public let stats: CareerStats

    public init(constructor: Constructor, stats: CareerStats) {
        self.constructor = constructor
        self.stats = stats
    }
}

public struct Countdown: Hashable, Sendable {
    public let days: Int
    public let hours: Int
    public let minutes: Int
    public let seconds: Int

    public var isOver: Bool { days == 0 && hours == 0 && minutes == 0 && seconds == 0 }

    public init(days: Int, hours: Int, minutes: Int, seconds: Int) {
        self.days = days
        self.hours = hours
        self.minutes = minutes
        self.seconds = seconds
    }
}

/// A race or sprint result tagged with its round, as used for season-wide statistics.
public struct ScoredResult: Codable, Hashable, Sendable {
    public let round: Int
    public let isSprint: Bool
    public let result: RaceResult

    public init(round: Int, isSprint: Bool, result: RaceResult) {
        self.round = round
        self.isSprint = isSprint
        self.result = result
    }
}

public struct QualifyingResult: Codable, Hashable, Sendable, Identifiable {
    public let position: Int
    public let driver: Driver
    public let constructor: Constructor
    public let q1: String?
    public let q2: String?
    public let q3: String?

    public var id: String { driver.id }
    /// Best lap of the deepest session reached.
    public var bestTime: String? { q3 ?? q2 ?? q1 }
    public var eliminatedIn: String? { q3 != nil ? nil : (q2 != nil ? "Q2" : "Q1") }

    public init(position: Int, driver: Driver, constructor: Constructor, q1: String?, q2: String?, q3: String?) {
        self.position = position
        self.driver = driver
        self.constructor = constructor
        self.q1 = q1
        self.q2 = q2
        self.q3 = q3
    }
}

extension RaceResult {
    /// Classified finishers have a numeric `positionText`; R/D/W/N/E/F mark retirements and exclusions.
    public var isClassified: Bool { !positionText.isEmpty && positionText.allSatisfy(\.isNumber) }
}
