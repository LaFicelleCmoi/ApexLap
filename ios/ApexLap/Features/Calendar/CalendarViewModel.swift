import ApexLapDomain
import Foundation
import Observation

@MainActor
@Observable
final class CalendarViewModel {
    let season: Int
    let calendar = OfflineFirst<[Race]>()

    init(now: Date = .now) {
        season = CalendarLogic.currentSeason(now: now)
    }

    var races: [Race] { calendar.value ?? [] }

    func load(using repository: any F1Repository) async {
        await calendar.load(
            cached: { await repository.cachedCalendar(season: season) },
            refresh: { try await repository.refreshCalendar(season: season) }
        )
    }

    func refresh(using repository: any F1Repository) async {
        await calendar.refresh { try await repository.refreshCalendar(season: season) }
    }
}
