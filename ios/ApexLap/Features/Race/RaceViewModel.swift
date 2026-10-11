import ApexLapDomain
import Foundation
import Observation

@MainActor
@Observable
final class RaceViewModel {
    let season: Int
    let round: Int
    private(set) var race: Race?
    let results = OfflineFirst<[RaceResult]>()

    init(season: Int, round: Int) {
        self.season = season
        self.round = round
    }

    func load(using repository: any F1Repository) async {
        race = await repository.cachedCalendar(season: season).first { $0.round == round }
        await results.load(
            cached: { await repository.cachedRaceResults(season: season, round: round) },
            refresh: { try await repository.refreshRaceResults(season: season, round: round) }
        )
    }

    func refresh(using repository: any F1Repository) async {
        await results.refresh { try await repository.refreshRaceResults(season: season, round: round) }
    }
}
