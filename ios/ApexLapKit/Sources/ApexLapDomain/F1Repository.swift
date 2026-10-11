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

    func cachedSprintResults(season: Int, round: Int) async -> [RaceResult]
    func cachedQualifying(season: Int, round: Int) async -> [QualifyingResult]
    /// Race, qualifying and (for sprint weekends) sprint results of one Grand Prix.
    func refreshRaceWeekend(season: Int, round: Int, includeSprint: Bool) async throws

    /// Every race and sprint result of the season: the input of the results and statistics screens.
    func cachedSeasonResults(season: Int) async -> [ScoredResult]
    func refreshSeasonResults(season: Int) async throws -> [ScoredResult]

    func cachedDriverProfile(id: String) async -> DriverProfile?
    func refreshDriverProfile(id: String) async throws -> DriverProfile

    func cachedConstructorProfile(id: String) async -> ConstructorProfile?
    func refreshConstructorProfile(id: String) async throws -> ConstructorProfile
}
