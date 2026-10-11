import Foundation

/// Offline-first contract: `cached*` read the local store instantly (single source of truth),
/// `refresh*` fetch from the network, persist, and return the fresh value.
public protocol F1Repository: Sendable {
    func cachedCalendar(season: Int) async -> [Race]
    func refreshCalendar(season: Int) async throws -> [Race]

    func cachedDriverStandings(season: Int) async -> [DriverStanding]
    func cachedConstructorStandings(season: Int) async -> [ConstructorStanding]
    func refreshStandings(season: Int) async throws -> ([DriverStanding], [ConstructorStanding])

    func cachedRaceResults(season: Int, round: Int) async -> [RaceResult]
    func refreshRaceResults(season: Int, round: Int) async throws -> [RaceResult]

    func cachedDriverProfile(id: String) async -> DriverProfile?
    func refreshDriverProfile(id: String) async throws -> DriverProfile

    func cachedConstructorProfile(id: String) async -> ConstructorProfile?
    func refreshConstructorProfile(id: String) async throws -> ConstructorProfile
}
