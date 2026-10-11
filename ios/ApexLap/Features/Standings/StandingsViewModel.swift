import ApexLapDomain
import Foundation
import Observation

@MainActor
@Observable
final class StandingsViewModel {
    typealias Tables = (drivers: [DriverStanding], constructors: [ConstructorStanding])

    let season: Int
    let standings = OfflineFirst<Tables>()
    var query = ""

    init(now: Date = .now) {
        season = CalendarLogic.currentSeason(now: now)
    }

    var drivers: [DriverStanding] { StandingsFilter.drivers(standings.value?.drivers ?? [], matching: query) }
    var constructors: [ConstructorStanding] {
        StandingsFilter.constructors(standings.value?.constructors ?? [], matching: query)
    }
    var hasData: Bool { !(standings.value?.drivers.isEmpty ?? true) }

    func load(using repository: any F1Repository) async {
        await standings.load(
            cached: {
                let drivers = await repository.cachedDriverStandings(season: season)
                let constructors = await repository.cachedConstructorStandings(season: season)
                return drivers.isEmpty ? nil : (drivers, constructors)
            },
            refresh: { try await self.fetch(repository) }
        )
    }

    func refresh(using repository: any F1Repository) async {
        await standings.refresh { try await self.fetch(repository) }
    }

    private func fetch(_ repository: any F1Repository) async throws -> Tables {
        let (drivers, constructors) = try await repository.refreshStandings(season: season)
        return (drivers, constructors)
    }
}
