import ApexLapData
import ApexLapDomain
import SwiftUI

enum AppRoute: Hashable {
    case race(season: Int, round: Int)
    case driver(id: String)
    case constructor(id: String)
    case reminders
    /// An OpenF1 session key, or nil to start from the latest qualifying.
    case telemetry(sessionKey: Int?)
    case settings
    case encyclopedia
    case season(Int)
    case circuit(id: String)
}

extension View {
    func withAppRoutes() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case let .race(season, round): RaceView(season: season, round: round)
            case let .driver(id): DriverView(driverId: id)
            case let .constructor(id): ConstructorView(constructorId: id)
            case .reminders: RemindersView()
            case let .telemetry(sessionKey): LapComparisonView(sessionKey: sessionKey)
            case .settings: SettingsView()
            case .encyclopedia: EncyclopediaView()
            case let .season(season): SeasonView(season: season)
            case let .circuit(id): CircuitView(circuitId: id)
            }
        }
    }
}

private struct RepositoryKey: EnvironmentKey {
    /// Previews get an empty in-memory cache backed by the real API.
    static let defaultValue: any F1Repository = OfflineFirstF1Repository(store: InMemoryF1Store())
}

extension EnvironmentValues {
    var repository: any F1Repository {
        get { self[RepositoryKey.self] }
        set { self[RepositoryKey.self] = newValue }
    }
}
