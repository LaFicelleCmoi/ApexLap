@testable import ApexLapData
import Testing
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// Serves recorded Jolpica fixtures by URL path; `offline` simulates a network failure.
final class StubHTTPClient: HTTPClient, @unchecked Sendable {
    private let lock = NSLock()
    private var _offline = false
    private var _requests: [URL] = []
    var counts: [String: Int] = [:]

    var offline: Bool {
        get { lock.withLock { _offline } }
        set { lock.withLock { _offline = newValue } }
    }

    var requests: [URL] { lock.withLock { _requests } }

    func send(_ request: URLRequest) async throws -> Data {
        let url = request.url!
        lock.withLock { _requests.append(url) }
        if offline { throw NetworkError.offline }
        let path = url.path.replacingOccurrences(of: "/ergast/f1/", with: "")
        switch path {
        case "2026.json": return try fixture("calendar_2026")
        case "2026/driverstandings.json": return try fixture("driver_standings_2026")
        case "2026/constructorstandings.json": return Data(#"{"MRData":{"StandingsTable":{"StandingsLists":[]}}}"#.utf8)
        case "2026/17/results.json": return try fixture("results_singapore_2026")
        case "2026/17/qualifying.json": return try fixture("qualifying_singapore_2026")
        case "2026/17/sprint.json", "2026/sprint.json": return try asSprint(fixture("results_singapore_2026"))
        case let circuit where circuit.hasPrefix("circuits/"), "2026/results/1.json":
            return try fixture("results_singapore_2026")
        case "2026/results.json":
            // Pretend there are 150 rows so the client walks two pages of the same fixture.
            let raw = String(decoding: try fixture("results_singapore_2026"), as: UTF8.self)
            return Data(raw.replacingOccurrences(of: #""total":"22""#, with: #""total":"150""#).utf8)
        case "drivers/antonelli.json":
            return Data(#"{"MRData":{"DriverTable":{"Drivers":[{"driverId":"antonelli","permanentNumber":"12","code":"ANT","givenName":"Andrea Kimi","familyName":"Antonelli","dateOfBirth":"2006-08-25","nationality":"Italian"}]}}}"#.utf8)
        default:
            // Longest match first: `grid/1/results.json` also ends with `/results.json`.
            let matches = counts.keys.filter { path.hasSuffix("/\($0).json") }
            if let key = matches.max(by: { $0.count < $1.count }) {
                return Data(#"{"MRData":{"total":"\#(counts[key]!)"}}"#.utf8)
            }
            throw NetworkError.http(status: 404)
        }
    }
}

/// Re-labels race results as sprint results (same row format).
func asSprint(_ data: Data) -> Data {
    Data(String(decoding: data, as: UTF8.self).replacingOccurrences(of: #""Results""#, with: #""SprintResults""#).utf8)
}

func fixture(_ name: String) throws -> Data {
    let url = try #require(Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures"))
    return try Data(contentsOf: url)
}

