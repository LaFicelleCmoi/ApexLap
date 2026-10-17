import Foundation

public struct ChampionDriver: Codable, Hashable, Sendable {
    public let id: String
    public let name: String
    public let nationality: String
    public let points: Double
    public let wins: Int
    public let team: String?
    public let teamId: String?
}

public struct ChampionTeam: Codable, Hashable, Sendable {
    public let id: String
    public let name: String
    public let nationality: String
    public let points: Double
    public let wins: Int
}

/// World champions of a finished season. The constructors' title only exists since 1958.
public struct Champion: Codable, Hashable, Sendable, Identifiable {
    public let season: Int
    public let races: Int
    public let driver: ChampionDriver
    public let constructor: ChampionTeam?
    public var id: Int { season }
}

/// Titles won by a driver or a constructor, with the seasons.
public struct Titles: Hashable, Sendable {
    public let count: Int
    public let seasons: [Int]
    public static let none = Titles(count: 0, seasons: [])

    public init(count: Int, seasons: [Int]) {
        self.count = count
        self.seasons = seasons
    }
}

public struct ChampionsRegistry: Sendable {
    /// Most recent first.
    public let champions: [Champion]
    private let driverTitles: [String: Titles]
    private let constructorTitles: [String: Titles]

    public init(_ champions: [Champion]) {
        self.champions = champions.sorted { $0.season > $1.season }
        driverTitles = Dictionary(grouping: champions, by: \.driver.id).mapValues { Titles(count: $0.count, seasons: $0.map(\.season)) }
        constructorTitles = Dictionary(grouping: champions.filter { $0.constructor != nil }, by: { $0.constructor!.id })
            .mapValues { Titles(count: $0.count, seasons: $0.map(\.season)) }
    }

    /// Parses the bundled `champions.json` (same file as the Android asset).
    public static func decode(_ data: Data) throws -> ChampionsRegistry {
        ChampionsRegistry(try JSONDecoder().decode([Champion].self, from: data))
    }

    public func driverTitles(_ id: String) -> Titles { driverTitles[id] ?? .none }
    public func constructorTitles(_ id: String) -> Titles { constructorTitles[id] ?? .none }
    public func season(_ season: Int) -> Champion? { champions.first { $0.season == season } }
}

public struct SeasonWinner: Hashable, Sendable, Identifiable {
    public let race: Race
    public let winner: RaceResult
    public var id: Int { race.round }

    public init(race: Race, winner: RaceResult) {
        self.race = race
        self.winner = winner
    }
}

public struct CircuitWin: Hashable, Sendable, Identifiable {
    public let season: Int
    public let round: Int
    public let raceName: String
    public let driver: Driver
    public let constructor: Constructor
    public var id: String { "\(season)-\(round)" }

    public init(season: Int, round: Int, raceName: String, driver: Driver, constructor: Constructor) {
        self.season = season
        self.round = round
        self.raceName = raceName
        self.driver = driver
        self.constructor = constructor
    }
}

public struct DriverCount: Hashable, Sendable, Identifiable {
    public let driver: Driver
    public let count: Int
    public var id: String { driver.id }
}

public struct ConstructorCount: Hashable, Sendable, Identifiable {
    public let constructor: Constructor
    public let count: Int
    public var id: String { constructor.id }
}

public struct GrandPrixContext: Hashable, Sendable {
    /// 1 for the first Grand Prix held at the circuit.
    public let edition: Int
    public let previousWinner: CircuitWin?
    public let mostWins: DriverCount?
}

public struct CircuitHistory: Hashable, Sendable {
    public let circuit: Circuit
    /// Most recent first.
    public let wins: [CircuitWin]

    public init(circuit: Circuit, wins: [CircuitWin]) {
        self.circuit = circuit
        self.wins = wins.sorted { $0.season > $1.season }
    }

    public var grandsPrix: Int { wins.count }
    public var firstSeason: Int? { wins.map(\.season).min() }
    public var lastSeason: Int? { wins.map(\.season).max() }

    public func topDrivers(_ size: Int = 3) -> [DriverCount] {
        Dictionary(grouping: wins, by: \.driver.id).values
            .map { DriverCount(driver: $0[0].driver, count: $0.count) }
            .sorted { $0.count != $1.count ? $0.count > $1.count : $0.driver.familyName < $1.driver.familyName }
            .prefix(size).map { $0 }
    }

    public func topConstructors(_ size: Int = 3) -> [ConstructorCount] {
        Dictionary(grouping: wins, by: \.constructor.id).values
            .map { ConstructorCount(constructor: $0[0].constructor, count: $0.count) }
            .sorted { $0.count != $1.count ? $0.count > $1.count : $0.constructor.name < $1.constructor.name }
            .prefix(size).map { $0 }
    }

    /// Where the Grand Prix of `season` sits in the history of this circuit.
    public func context(for season: Int) -> GrandPrixContext {
        let before = wins.filter { $0.season < season }
        return GrandPrixContext(
            edition: before.count + 1,
            previousWinner: before.max { $0.season < $1.season },
            mostWins: topDrivers(1).first.flatMap { $0.count > 1 ? $0 : nil }
        )
    }
}

public struct LapRecord: Hashable, Sendable {
    public let time: String
    public let season: Int
    public let driver: Driver
    public let constructor: Constructor

    public init(time: String, season: Int, driver: Driver, constructor: Constructor) {
        self.time = time
        self.season = season
        self.driver = driver
        self.constructor = constructor
    }
}

public struct CircuitRecords: Hashable, Sendable {
    /// Fastest race lap (timing data exists since 2004; layouts may have changed since).
    public let raceLap: LapRecord?
    /// Drivers who started the most races here from pole.
    public let poleSitters: [DriverCount]

    public init(raceLap: LapRecord?, poleSitters: [DriverCount]) {
        self.raceLap = raceLap
        self.poleSitters = poleSitters
    }
}

public enum LapTime {
    /// "1:21.046" or "58.123" → milliseconds, nil when unparseable.
    public static func milliseconds(_ time: String) -> Int? {
        let parts = time.trimmingCharacters(in: .whitespaces).split(separator: ":", omittingEmptySubsequences: false)
        guard parts.count <= 2, let seconds = Double(parts.last ?? "") else { return nil }
        let minutes = parts.count == 2 ? Int(parts[0]) : 0
        guard let minutes else { return nil }
        return minutes * 60_000 + Int((seconds * 1000).rounded())
    }
}

/// Key facts of a weekend: winner, pole and fastest lap.
public struct GrandPrixFacts: Hashable, Sendable {
    public let winner: RaceResult?
    public let pole: Driver?
    public let poleTime: String?
    public let fastestLap: RaceResult?

    public var isEmpty: Bool { winner == nil && pole == nil && fastestLap == nil }

    /// Qualifying data only exists since the 1990s: the pole falls back to the grid of the race.
    public init(race: [RaceResult], qualifying: [QualifyingResult]) {
        let qualifyingPole = qualifying.first { $0.position == 1 }
        winner = race.first { $0.position == 1 }
        pole = qualifyingPole?.driver ?? race.first { $0.grid == 1 }?.driver
        poleTime = qualifyingPole?.bestTime
        fastestLap = race.first(where: \.hasFastestLap)
    }
}

/// Highlights of a season, computed from the Grand Prix winners and the final standings.
public struct SeasonHighlights: Hashable, Sendable {
    public let differentWinners: Int
    public let topWinner: DriverCount?
    public let topTeam: ConstructorCount?
    public let winsFromPole: Int
    /// Lowest starting position a race was won from.
    public let deepestWin: SeasonWinner?
    public let runnerUp: DriverStanding?
    /// Points between the leader/champion and the runner-up.
    public let titleMargin: Double?

    public init(winners: [SeasonWinner], standings: [DriverStanding]) {
        let byDriver = Dictionary(grouping: winners, by: \.winner.driver.id).values
        let byTeam = Dictionary(grouping: winners, by: \.winner.constructor.id).values
        let sorted = standings.sorted { $0.position < $1.position }
        differentWinners = byDriver.count
        topWinner = byDriver.max { $0.count < $1.count }.map { DriverCount(driver: $0[0].winner.driver, count: $0.count) }
        topTeam = byTeam.max { $0.count < $1.count }.map { ConstructorCount(constructor: $0[0].winner.constructor, count: $0.count) }
        winsFromPole = winners.filter { $0.winner.grid == 1 }.count
        deepestWin = winners.filter { $0.winner.grid > 1 }.max { $0.winner.grid < $1.winner.grid }
        runnerUp = sorted.count > 1 ? sorted[1] : nil
        titleMargin = sorted.count >= 2 ? sorted[0].points - sorted[1].points : nil
    }
}

extension String {
    /// Case- and accent-insensitive search ("perez" finds "Pérez").
    public func matchesSearch(_ query: String) -> Bool {
        let q = query.trimmingCharacters(in: .whitespaces)
        if q.isEmpty { return true }
        let options: String.CompareOptions = [.caseInsensitive, .diacriticInsensitive]
        return range(of: q, options: options) != nil
    }
}
