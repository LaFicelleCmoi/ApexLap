import ApexLapDomain
import Foundation
import Observation

struct WeekendResults {
    var race: [RaceResult] = []
    var qualifying: [QualifyingResult] = []
    var sprint: [RaceResult] = []
    var isEmpty: Bool { race.isEmpty && qualifying.isEmpty && sprint.isEmpty }
}

@MainActor
@Observable
final class RaceViewModel {
    let season: Int
    let round: Int
    private(set) var race: Race?
    private(set) var results = WeekendResults()
    private(set) var isRefreshing = false
    private(set) var error: String?

    init(season: Int, round: Int) {
        self.season = season
        self.round = round
    }

    var hasStarted: Bool { race.map { ($0.sessions.first?.startsAt ?? .distantPast) < .now } ?? true }

    func load(using repository: any F1Repository) async {
        race = await repository.cachedCalendar(season: season).first { $0.round == round }
        results = WeekendResults(
            race: await repository.cachedRaceResults(season: season, round: round),
            qualifying: await repository.cachedQualifying(season: season, round: round),
            sprint: await repository.cachedSprintResults(season: season, round: round)
        )
        await refresh(using: repository)
    }

    /// Nothing to download before the first session of the weekend has started.
    func refresh(using repository: any F1Repository) async {
        guard hasStarted, !isRefreshing else { return }
        isRefreshing = true
        error = nil
        defer { isRefreshing = false }
        do {
            try await repository.refreshRaceWeekend(season: season, round: round, includeSprint: race?.isSprintWeekend ?? true)
            results = WeekendResults(
                race: await repository.cachedRaceResults(season: season, round: round),
                qualifying: await repository.cachedQualifying(season: season, round: round),
                sprint: await repository.cachedSprintResults(season: season, round: round)
            )
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }
}
