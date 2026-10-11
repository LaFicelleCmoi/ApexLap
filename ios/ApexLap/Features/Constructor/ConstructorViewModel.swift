import ApexLapDomain
import Foundation
import Observation

@MainActor
@Observable
final class ConstructorViewModel {
    let constructorId: String
    let season: Int
    let profile = OfflineFirst<ConstructorProfile>()
    private(set) var standing: ConstructorStanding?
    private(set) var drivers: [DriverStanding] = []

    init(constructorId: String, now: Date = .now) {
        self.constructorId = constructorId
        season = CalendarLogic.currentSeason(now: now)
    }

    func load(using repository: any F1Repository) async {
        standing = await repository.cachedConstructorStandings(season: season).first { $0.constructor.id == constructorId }
        drivers = await repository.cachedDriverStandings(season: season).filter { $0.constructor?.id == constructorId }
        await profile.load(
            cached: { await repository.cachedConstructorProfile(id: constructorId) },
            refresh: { try await repository.refreshConstructorProfile(id: constructorId) }
        )
    }

    func refresh(using repository: any F1Repository) async {
        await profile.refresh { try await repository.refreshConstructorProfile(id: constructorId) }
    }
}
