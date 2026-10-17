import ApexLapDomain
import Foundation

/// Historical F1 data (same sources as Android). The all-time lists are downloaded once and kept on disk;
/// circuit and season histories are fetched on demand and kept in memory; past champions come from a bundled file.
public actor EncyclopediaRepository {
    private let client: JolpicaClient
    private let directory: URL?
    private var registry: ChampionsRegistry?
    private var circuitHistories: [String: CircuitHistory] = [:]
    private var circuitRecords: [String: CircuitRecords] = [:]
    private var seasonWinners: [Int: [SeasonWinner]] = [:]

    /// `directory` holds the downloaded lists; nil keeps them in memory only (tests).
    public init(client: JolpicaClient = JolpicaClient(), directory: URL? = EncyclopediaRepository.defaultDirectory) {
        self.client = client
        self.directory = directory
    }

    public static var defaultDirectory: URL? {
        FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first?.appendingPathComponent("encyclopedia", isDirectory: true)
    }

    // MARK: Champions

    public func champions() -> ChampionsRegistry {
        if let registry { return registry }
        let loaded = Bundle.module.url(forResource: "champions", withExtension: "json")
            .flatMap { try? Data(contentsOf: $0) }
            .flatMap { try? ChampionsRegistry.decode($0) } ?? ChampionsRegistry([])
        registry = loaded
        return loaded
    }

    // MARK: All-time lists (cached on disk, refreshed when empty or on demand)

    public func cachedDrivers() -> [Driver] { read("drivers") ?? [] }
    public func cachedConstructors() -> [Constructor] { read("constructors") ?? [] }
    public func cachedCircuits() -> [Circuit] { read("circuits") ?? [] }

    public func drivers(force: Bool = false) async throws -> [Driver] {
        if !force, let cached: [Driver] = read("drivers"), !cached.isEmpty { return cached }
        let fresh = try await client.allDrivers()
        write(fresh, "drivers")
        return fresh
    }

    public func constructors(force: Bool = false) async throws -> [Constructor] {
        if !force, let cached: [Constructor] = read("constructors"), !cached.isEmpty { return cached }
        let fresh = try await client.allConstructors()
        write(fresh, "constructors")
        return fresh
    }

    public func circuits(force: Bool = false) async throws -> [Circuit] {
        if !force, let cached: [Circuit] = read("circuits"), !cached.isEmpty { return cached }
        let fresh = try await client.allCircuits()
        write(fresh, "circuits")
        return fresh
    }

    // MARK: On-demand histories

    public func circuitHistory(_ circuitId: String) async throws -> CircuitHistory {
        if let cached = circuitHistories[circuitId] { return cached }
        let rows = try await client.racesWithFirstResult("circuits/\(circuitId)/results/1")
        let circuit = rows.first?.race.circuit ?? Circuit(id: circuitId, name: circuitId, locality: "", country: "")
        let history = CircuitHistory(circuit: circuit, wins: rows.map {
            CircuitWin(season: $0.race.season, round: $0.race.round, raceName: $0.race.name, driver: $0.result.driver, constructor: $0.result.constructor)
        })
        circuitHistories[circuitId] = history
        return history
    }

    public func circuitRecords(_ circuitId: String) async throws -> CircuitRecords {
        if let cached = circuitRecords[circuitId] { return cached }
        let laps = try await client.racesWithFirstResult("circuits/\(circuitId)/fastest/1/results")
        var raceLap: LapRecord?
        var best = Int.max
        for row in laps {
            guard let time = row.result.fastestLapTime, let ms = LapTime.milliseconds(time), ms < best else { continue }
            best = ms
            raceLap = LapRecord(time: time, season: row.race.season, driver: row.result.driver, constructor: row.result.constructor)
        }
        let poles = try await client.racesWithFirstResult("circuits/\(circuitId)/grid/1/results")
        let poleDrivers: [Driver] = poles.map { $0.result.driver }
        let grouped: [String: [Driver]] = Dictionary(grouping: poleDrivers, by: { $0.id })
        let counts: [DriverCount] = grouped.values.map { DriverCount(driver: $0[0], count: $0.count) }
        let poleSitters = counts.sorted(by: Self.byCountThenName)
        let records = CircuitRecords(raceLap: raceLap, poleSitters: Array(poleSitters.prefix(3)))
        circuitRecords[circuitId] = records
        return records
    }

    public func seasonWinners(_ season: Int) async throws -> [SeasonWinner] {
        if let cached = seasonWinners[season] { return cached }
        let winners = try await client.racesWithFirstResult("\(season)/results/1")
            .map { SeasonWinner(race: $0.race, winner: $0.result) }
            .sorted { $0.race.round < $1.race.round }
        seasonWinners[season] = winners
        return winners
    }

    private static func byCountThenName(_ a: DriverCount, _ b: DriverCount) -> Bool {
        a.count != b.count ? a.count > b.count : a.driver.familyName < b.driver.familyName
    }

    // MARK: Disk cache

    private func read<T: Decodable>(_ name: String) -> T? {
        guard let url = directory?.appendingPathComponent("\(name).json"), let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    private func write<T: Encodable>(_ value: T, _ name: String) {
        guard let directory else { return }
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        guard let data = try? JSONEncoder().encode(value) else { return }
        try? data.write(to: directory.appendingPathComponent("\(name).json"), options: .atomic)
    }
}
