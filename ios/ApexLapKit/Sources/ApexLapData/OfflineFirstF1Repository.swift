import ApexLapDomain
import Foundation

public struct OfflineFirstF1Repository: F1Repository {
    private let client: JolpicaClient
    private let store: F1LocalStore

    public init(client: JolpicaClient = JolpicaClient(), store: F1LocalStore) {
        self.client = client
        self.store = store
    }

    public func cachedCalendar(season: Int) async -> [Race] {
        await store.races(season: season)
    }

    public func refreshCalendar(season: Int) async throws -> [Race] {
        let races = try await client.calendar(season: season)
        await store.replaceRaces(races, season: season)
        return races
    }

    public func cachedDriverStandings(season: Int) async -> [DriverStanding] {
        await store.driverStandings(season: season)
    }

    public func cachedConstructorStandings(season: Int) async -> [ConstructorStanding] {
        await store.constructorStandings(season: season)
    }

    public func refreshStandings(season: Int) async throws -> ([DriverStanding], [ConstructorStanding]) {
        async let drivers = client.driverStandings(season: season)
        async let constructors = client.constructorStandings(season: season)
        let fresh = try await (drivers, constructors)
        await store.replaceStandings(drivers: fresh.0, constructors: fresh.1, season: season)
        return fresh
    }

    public func cachedRaceResults(season: Int, round: Int) async -> [RaceResult] {
        await store.raceResults(season: season, round: round)
    }

    public func refreshRaceResults(season: Int, round: Int) async throws -> [RaceResult] {
        let results = try await client.raceResults(season: season, round: round)
        await store.replaceRaceResults(results, season: season, round: round)
        return results
    }

    public func cachedSprintResults(season: Int, round: Int) async -> [RaceResult] {
        await store.sprintResults(season: season, round: round)
    }

    public func cachedQualifying(season: Int, round: Int) async -> [QualifyingResult] {
        await store.qualifying(season: season, round: round)
    }

    public func refreshRaceWeekend(season: Int, round: Int, includeSprint: Bool) async throws {
        await store.replaceRaceResults(try await client.raceResults(season: season, round: round), season: season, round: round)
        await store.replaceQualifying(try await client.qualifying(season: season, round: round), season: season, round: round)
        if includeSprint {
            await store.replaceSprintResults(try await client.sprintResults(season: season, round: round), season: season, round: round)
        }
    }

    public func cachedSeasonResults(season: Int) async -> [ScoredResult] {
        await store.seasonResults(season: season)
    }

    public func refreshSeasonResults(season: Int) async throws -> [ScoredResult] {
        let results = try await client.seasonResults(season: season)
        await store.replaceSeasonResults(results, season: season)
        return results
    }

    public func cachedDriverProfile(id: String) async -> DriverProfile? {
        await store.driverProfile(id: id)
    }

    public func refreshDriverProfile(id: String) async throws -> DriverProfile {
        async let driver = client.driver(id: id)
        async let stats = client.careerStats(kind: "drivers", id: id)
        let profile = try await DriverProfile(driver: driver, stats: stats)
        await store.saveDriverProfile(profile)
        return profile
    }

    public func cachedConstructorProfile(id: String) async -> ConstructorProfile? {
        await store.constructorProfile(id: id)
    }

    public func refreshConstructorProfile(id: String) async throws -> ConstructorProfile {
        async let constructor = client.constructor(id: id)
        async let stats = client.careerStats(kind: "constructors", id: id)
        let profile = try await ConstructorProfile(constructor: constructor, stats: stats)
        await store.saveConstructorProfile(profile)
        return profile
    }
}
