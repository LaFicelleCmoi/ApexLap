import ApexLapDomain
import Foundation
import Observation

@MainActor
@Observable
final class DriverViewModel {
    let driverId: String
    let season: Int
    let profile = OfflineFirst<DriverProfile>()
    /// Current-season standing, also gives the driver's team (and therefore colours).
    private(set) var standing: DriverStanding?

    init(driverId: String, now: Date = .now) {
        self.driverId = driverId
        season = CalendarLogic.currentSeason(now: now)
    }

    func load(using repository: any F1Repository) async {
        standing = await repository.cachedDriverStandings(season: season).first { $0.driver.id == driverId }
        await profile.load(
            cached: { await repository.cachedDriverProfile(id: driverId) },
            refresh: { try await repository.refreshDriverProfile(id: driverId) }
        )
    }

    func refresh(using repository: any F1Repository) async {
        await profile.refresh { try await repository.refreshDriverProfile(id: driverId) }
    }
}
