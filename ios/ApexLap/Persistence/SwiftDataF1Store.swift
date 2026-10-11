import ApexLapData
import ApexLapDomain
import Foundation
import SwiftData

/// One cached API payload. Domain models are `Codable`, so a single key/value entity keeps the schema
/// (and its migrations) trivial while still giving an indexed, persistent offline cache.
@Model
final class CachedPayload {
    @Attribute(.unique) var key: String
    var data: Data
    var updatedAt: Date

    init(key: String, data: Data, updatedAt: Date = .now) {
        self.key = key
        self.data = data
        self.updatedAt = updatedAt
    }
}

@ModelActor
actor SwiftDataF1Store: F1LocalStore {
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private func read<T: Decodable>(_ type: T.Type, key: String) -> T? {
        var descriptor = FetchDescriptor<CachedPayload>(predicate: #Predicate { $0.key == key })
        descriptor.fetchLimit = 1
        guard let payload = try? modelContext.fetch(descriptor).first else { return nil }
        return try? decoder.decode(T.self, from: payload.data)
    }

    private func write<T: Encodable>(_ value: T, key: String) {
        guard let data = try? encoder.encode(value) else { return }
        var descriptor = FetchDescriptor<CachedPayload>(predicate: #Predicate { $0.key == key })
        descriptor.fetchLimit = 1
        if let existing = try? modelContext.fetch(descriptor).first {
            existing.data = data
            existing.updatedAt = .now
        } else {
            modelContext.insert(CachedPayload(key: key, data: data))
        }
        try? modelContext.save()
    }

    func races(season: Int) -> [Race] { read([Race].self, key: "races-\(season)") ?? [] }
    func replaceRaces(_ races: [Race], season: Int) {
        write(races, key: "races-\(season)")
        SharedCalendar.save(races)
    }

    func driverStandings(season: Int) -> [DriverStanding] {
        read([DriverStanding].self, key: "driver-standings-\(season)") ?? []
    }

    func constructorStandings(season: Int) -> [ConstructorStanding] {
        read([ConstructorStanding].self, key: "constructor-standings-\(season)") ?? []
    }

    func replaceStandings(drivers: [DriverStanding], constructors: [ConstructorStanding], season: Int) {
        write(drivers, key: "driver-standings-\(season)")
        write(constructors, key: "constructor-standings-\(season)")
    }

    func raceResults(season: Int, round: Int) -> [RaceResult] {
        read([RaceResult].self, key: "results-\(season)-\(round)") ?? []
    }

    func replaceRaceResults(_ results: [RaceResult], season: Int, round: Int) {
        write(results, key: "results-\(season)-\(round)")
    }

    func sprintResults(season: Int, round: Int) -> [RaceResult] {
        read([RaceResult].self, key: "sprint-\(season)-\(round)") ?? []
    }

    func replaceSprintResults(_ results: [RaceResult], season: Int, round: Int) {
        write(results, key: "sprint-\(season)-\(round)")
    }

    func qualifying(season: Int, round: Int) -> [QualifyingResult] {
        read([QualifyingResult].self, key: "qualifying-\(season)-\(round)") ?? []
    }

    func replaceQualifying(_ results: [QualifyingResult], season: Int, round: Int) {
        write(results, key: "qualifying-\(season)-\(round)")
    }

    func seasonResults(season: Int) -> [ScoredResult] { read([ScoredResult].self, key: "season-\(season)") ?? [] }
    func replaceSeasonResults(_ results: [ScoredResult], season: Int) { write(results, key: "season-\(season)") }

    func driverProfile(id: String) -> DriverProfile? { read(DriverProfile.self, key: "driver-\(id)") }
    func saveDriverProfile(_ profile: DriverProfile) { write(profile, key: "driver-\(profile.driver.id)") }

    func constructorProfile(id: String) -> ConstructorProfile? { read(ConstructorProfile.self, key: "constructor-\(id)") }
    func saveConstructorProfile(_ profile: ConstructorProfile) {
        write(profile, key: "constructor-\(profile.constructor.id)")
    }
}
