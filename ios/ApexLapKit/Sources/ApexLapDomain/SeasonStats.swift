import Foundation

public struct DriverProgression: Hashable, Sendable, Identifiable {
    public let driver: Driver
    /// Team at the driver's latest round.
    public let constructor: Constructor
    /// Cumulative points after each round of `ChampionshipProgression.rounds`.
    public let cumulativePoints: [Double]

    public var id: String { driver.id }
    public var total: Double { cumulativePoints.last ?? 0 }
}

public struct ChampionshipProgression: Hashable, Sendable {
    public let rounds: [Int]
    /// Sorted by total points, leader first.
    public let drivers: [DriverProgression]
}

public enum LeaderCategory: String, CaseIterable, Sendable {
    case wins, podiums, poles, fastestLaps, positionsGained, dnfs

    public var title: String {
        switch self {
        case .wins: "Victoires"
        case .podiums: "Podiums"
        case .poles: "Poles"
        case .fastestLaps: "Meilleurs tours"
        case .positionsGained: "Places gagnées"
        case .dnfs: "Abandons"
        }
    }
}

public struct LeaderEntry: Hashable, Sendable, Identifiable {
    public let driver: Driver
    public let constructor: Constructor
    public let value: Int
    public var id: String { driver.id }
}

public struct LeaderBoard: Hashable, Sendable, Identifiable {
    public let category: LeaderCategory
    public let entries: [LeaderEntry]
    public var id: String { category.rawValue }
}

public struct DuelSide: Hashable, Sendable {
    public let driver: Driver
    public let constructor: Constructor
    public let points: Double
    public let wins: Int
    public let podiums: Int
    public let poles: Int
    public let bestFinish: Int?
    public let averageFinish: Double?
    public let dnfs: Int
    /// Grands Prix (both drivers entered) finished ahead of the rival.
    public let racesAhead: Int
    /// Grands Prix started ahead of the rival on the grid.
    public let startsAhead: Int
}

public struct HeadToHead: Hashable, Sendable {
    public let first: DuelSide
    public let second: DuelSide
    public let sharedRaces: Int
}

/// Same rules as the Android `SeasonStats.kt`, so both apps show identical numbers.
public enum SeasonStats {

    /// Rebuilds the championship round by round from race + sprint points.
    public static func progression(_ results: [ScoredResult]) -> ChampionshipProgression {
        let rounds = Array(Set(results.map(\.round))).sorted()
        let drivers = Dictionary(grouping: results, by: { $0.result.driver.id }).values.map { rows -> DriverProgression in
            let byRound = Dictionary(grouping: rows, by: \.round).mapValues { $0.reduce(0) { $0 + $1.result.points } }
            var total = 0.0
            let cumulative = rounds.map { round -> Double in
                total += byRound[round] ?? 0
                return total
            }
            let latest = rows.max { $0.round < $1.round }!.result
            return DriverProgression(driver: latest.driver, constructor: latest.constructor, cumulativePoints: cumulative)
        }
        return ChampionshipProgression(rounds: rounds, drivers: drivers.sorted { $0.total > $1.total })
    }

    /// Top entries of the season per category, from Grands Prix only (sprints excluded).
    public static func leaders(_ results: [ScoredResult], size: Int = 3) -> [LeaderBoard] {
        let races = results.filter { !$0.isSprint }.map(\.result)
        let byDriver = Dictionary(grouping: races, by: { $0.driver.id }).values
        return LeaderCategory.allCases.map { category in
            let entries = byDriver.map { rows -> LeaderEntry in
                let latest = rows.last!
                return LeaderEntry(driver: latest.driver, constructor: latest.constructor, value: score(rows, category))
            }
            .filter { $0.value > 0 }
            .sorted { $0.value != $1.value ? $0.value > $1.value : $0.driver.familyName < $1.driver.familyName }
            return LeaderBoard(category: category, entries: Array(entries.prefix(size)))
        }
    }

    private static func score(_ rows: [RaceResult], _ category: LeaderCategory) -> Int {
        switch category {
        case .wins: rows.filter { $0.position == 1 }.count
        case .podiums: rows.filter { (1...3).contains($0.position) }.count
        case .poles: rows.filter { $0.grid == 1 }.count
        case .fastestLaps: rows.filter(\.hasFastestLap).count
        case .positionsGained: rows.filter(\.isClassified).reduce(0) { $0 + ($1.positionsGained ?? 0) }
        case .dnfs: rows.filter { !$0.isClassified }.count
        }
    }

    public static func headToHead(_ results: [ScoredResult], first firstId: String, second secondId: String) -> HeadToHead? {
        guard firstId != secondId else { return nil }
        let first = results.filter { $0.result.driver.id == firstId }
        let second = results.filter { $0.result.driver.id == secondId }
        guard !first.isEmpty, !second.isEmpty else { return nil }

        let firstRaces = Dictionary(first.filter { !$0.isSprint }.map { ($0.round, $0.result) }, uniquingKeysWith: { a, _ in a })
        let secondRaces = Dictionary(second.filter { !$0.isSprint }.map { ($0.round, $0.result) }, uniquingKeysWith: { a, _ in a })
        let pairs = Set(firstRaces.keys).intersection(secondRaces.keys).map { (firstRaces[$0]!, secondRaces[$0]!) }
        func grid(_ r: RaceResult) -> Int { r.grid > 0 ? r.grid : Int.max }

        func side(_ rows: [ScoredResult], racesAhead: Int, startsAhead: Int) -> DuelSide {
            let races = rows.filter { !$0.isSprint }.map(\.result)
            let classified = races.filter(\.isClassified)
            let latest = rows.max { $0.round < $1.round }!.result
            return DuelSide(
                driver: latest.driver,
                constructor: latest.constructor,
                points: rows.reduce(0) { $0 + $1.result.points },
                wins: races.filter { $0.position == 1 }.count,
                podiums: races.filter { (1...3).contains($0.position) }.count,
                poles: races.filter { $0.grid == 1 }.count,
                bestFinish: classified.map(\.position).min(),
                averageFinish: classified.isEmpty ? nil : Double(classified.reduce(0) { $0 + $1.position }) / Double(classified.count),
                dnfs: races.filter { !$0.isClassified }.count,
                racesAhead: racesAhead,
                startsAhead: startsAhead
            )
        }

        let firstAhead = pairs.filter { $0.0.position < $0.1.position }.count
        return HeadToHead(
            first: side(first, racesAhead: firstAhead, startsAhead: pairs.filter { grid($0.0) < grid($0.1) }.count),
            second: side(second, racesAhead: pairs.count - firstAhead, startsAhead: pairs.filter { grid($0.1) < grid($0.0) }.count),
            sharedRaces: pairs.count
        )
    }
}
