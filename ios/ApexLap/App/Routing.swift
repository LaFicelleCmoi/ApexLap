import ApexLapData
import ApexLapDomain
import SwiftUI

enum AppRoute: Hashable {
    case race(season: Int, round: Int)
    case driver(id: String)
    case constructor(id: String)
}

extension View {
    func withAppRoutes() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case let .race(season, round): RaceView(season: season, round: round)
            case let .driver(id): DriverView(driverId: id)
            case let .constructor(id): ConstructorView(constructorId: id)
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
