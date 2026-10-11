import ApexLapDomain
import Foundation

/// Local persistence used by the offline-first repository. The app backs it with SwiftData;
/// tests and previews use `InMemoryF1Store`.
public protocol F1LocalStore: Sendable {
    func races(season: Int) async -> [Race]
    func replaceRaces(_ races: [Race], season: Int) async

    func driverStandings(season: Int) async -> [DriverStanding]
    func constructorStandings(season: Int) async -> [ConstructorStanding]
    func replaceStandings(drivers: [DriverStanding], constructors: [ConstructorStanding], season: Int) async

    func raceResults(season: Int, round: Int) async -> [RaceResult]
    func replaceRaceResults(_ results: [RaceResult], season: Int, round: Int) async

    func driverProfile(id: String) async -> DriverProfile?
    func saveDriverProfile(_ profile: DriverProfile) async

    func constructorProfile(id: String) async -> ConstructorProfile?
    func saveConstructorProfile(_ profile: ConstructorProfile) async
}

public actor InMemoryF1Store: F1LocalStore {
    private var races: [Int: [Race]] = [:]
    private var drivers: [Int: [DriverStanding]] = [:]
    private var constructors: [Int: [ConstructorStanding]] = [:]
    private var results: [String: [RaceResult]] = [:]
    private var driverProfiles: [String: DriverProfile] = [:]
    private var constructorProfiles: [String: ConstructorProfile] = [:]

    public init() {}

    public func races(season: Int) -> [Race] { races[season] ?? [] }
    public func replaceRaces(_ races: [Race], season: Int) { self.races[season] = races }

    public func driverStandings(season: Int) -> [DriverStanding] { drivers[season] ?? [] }
    public func constructorStandings(season: Int) -> [ConstructorStanding] { constructors[season] ?? [] }
    public func replaceStandings(drivers: [DriverStanding], constructors: [ConstructorStanding], season: Int) {
        self.drivers[season] = drivers
        self.constructors[season] = constructors
    }

    public func raceResults(season: Int, round: Int) -> [RaceResult] { results["\(season)-\(round)"] ?? [] }
    public func replaceRaceResults(_ results: [RaceResult], season: Int, round: Int) {
        self.results["\(season)-\(round)"] = results
    }

    public func driverProfile(id: String) -> DriverProfile? { driverProfiles[id] }
    public func saveDriverProfile(_ profile: DriverProfile) { driverProfiles[profile.driver.id] = profile }

    public func constructorProfile(id: String) -> ConstructorProfile? { constructorProfiles[id] }
    public func saveConstructorProfile(_ profile: ConstructorProfile) {
        constructorProfiles[profile.constructor.id] = profile
    }
}
