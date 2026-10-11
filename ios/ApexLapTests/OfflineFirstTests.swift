@testable import ApexLap
import ApexLapData
import XCTest

@MainActor
final class OfflineFirstTests: XCTestCase {
    func testCachedValueIsKeptWhenRefreshFails() async {
        let loader = OfflineFirst<[Int]>()

        await loader.load(cached: { [1, 2, 3] }, refresh: { throw NetworkError.offline })

        XCTAssertEqual(loader.value, [1, 2, 3])
        XCTAssertEqual(loader.error, "Hors ligne — affichage des données en cache")
        XCTAssertFalse(loader.isRefreshing)
    }

    func testRefreshReplacesCacheAndClearsError() async {
        let loader = OfflineFirst<[Int]>()
        await loader.load(cached: { [1] }, refresh: { throw NetworkError.http(status: 503) })
        XCTAssertEqual(loader.error, "Serveur indisponible (HTTP 503)")

        await loader.refresh { [4, 5] }

        XCTAssertEqual(loader.value, [4, 5])
        XCTAssertNil(loader.error)
    }

    func testStandingsViewModelFiltersWithQuery() async throws {
        let model = StandingsViewModel(now: ISO8601DateFormatter().date(from: "2026-10-11T18:00:00Z")!)
        XCTAssertEqual(model.season, 2026)
        XCTAssertTrue(model.drivers.isEmpty)
    }
}
