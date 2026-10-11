@testable import ApexLapData
import ApexLapDomain
import Foundation
import Testing

@Suite("Offline-first repository")
struct RepositoryTests {
    let http = StubHTTPClient()
    let store = InMemoryF1Store()
    var repository: OfflineFirstF1Repository {
        OfflineFirstF1Repository(client: JolpicaClient(http: http), store: store)
    }

    @Test func refreshPersistsTheCalendar() async throws {
        #expect(await repository.cachedCalendar(season: 2026).isEmpty)
        let fresh = try await repository.refreshCalendar(season: 2026)
        #expect(fresh.map(\.round) == [1, 2, 3])
        #expect(await repository.cachedCalendar(season: 2026) == fresh)
    }

    @Test func failedRefreshKeepsPreviousCache() async throws {
        _ = try await repository.refreshStandings(season: 2026)
        http.offline = true

        await #expect(throws: NetworkError.offline) {
            try await repository.refreshStandings(season: 2026)
        }
        let cached = await repository.cachedDriverStandings(season: 2026)
        #expect(cached.map(\.driver.id) == ["antonelli", "russell", "hamilton"])
    }

    @Test func careerStatsAreDerivedFromResultCounts() async throws {
        http.counts = ["seasons": 12, "results": 250, "results/1": 73, "results/2": 40, "results/3": 15, "grid/1/results": 48]

        let profile = try await repository.refreshDriverProfile(id: "antonelli")

        #expect(profile.stats == CareerStats(seasons: 12, races: 250, wins: 73, podiums: 128, poles: 48))
        #expect(await repository.cachedDriverProfile(id: "antonelli") == profile)
        #expect(http.requests.allSatisfy { $0.path.contains("drivers/antonelli") })
    }

    @Test func standingsSearchMatchesNameCodeOrTeam() async throws {
        let (drivers, _) = try await repository.refreshStandings(season: 2026)
        #expect(StandingsFilter.drivers(drivers, matching: "ham").map(\.driver.id) == ["hamilton"])
        #expect(StandingsFilter.drivers(drivers, matching: "mercedes").map(\.driver.id) == ["antonelli", "russell"])
        #expect(StandingsFilter.drivers(drivers, matching: " ").count == 3)
    }
}
