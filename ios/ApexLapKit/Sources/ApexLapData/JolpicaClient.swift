import ApexLapDomain
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public enum NetworkError: Error, Equatable, Sendable {
    case offline
    case http(status: Int)
    case decoding
}

/// Transport abstraction so the client can be tested without the network.
public protocol HTTPClient: Sendable {
    func get(_ url: URL) async throws -> Data
}

public struct URLSessionHTTPClient: HTTPClient {
    private let session: URLSession

    public init(session: URLSession = .shared) {
        self.session = session
    }

    public func get(_ url: URL) async throws -> Data {
        let request = URLRequest(url: url, timeoutInterval: 20)
        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw NetworkError.offline
        }
        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw NetworkError.http(status: http.statusCode)
        }
        return data
    }
}

/// Jolpica-F1 (Ergast successor): https://github.com/jolpica/jolpica-f1
public struct JolpicaClient: Sendable {
    public static let baseURL = URL(string: "https://api.jolpi.ca/ergast/f1/")!

    private let http: HTTPClient
    private let baseURL: URL

    public init(http: HTTPClient = URLSessionHTTPClient(), baseURL: URL = JolpicaClient.baseURL) {
        self.http = http
        self.baseURL = baseURL
    }

    func calendar(season: Int) async throws -> [Race] {
        let response: JolpicaResponse<RaceTableData> = try await get("\(season).json", limit: 100)
        return response.data.raceTable.races.map { $0.toDomain() }
    }

    func driverStandings(season: Int) async throws -> [DriverStanding] {
        let response: JolpicaResponse<StandingsData> = try await get("\(season)/driverstandings.json", limit: 100)
        let rows = response.data.standingsTable.lists.first?.driverStandings ?? []
        return rows.enumerated().map { $1.toDomain(fallbackPosition: $0 + 1) }
    }

    func constructorStandings(season: Int) async throws -> [ConstructorStanding] {
        let response: JolpicaResponse<StandingsData> = try await get("\(season)/constructorstandings.json", limit: 100)
        let rows = response.data.standingsTable.lists.first?.constructorStandings ?? []
        return rows.enumerated().map { $1.toDomain(fallbackPosition: $0 + 1) }
    }

    func raceResults(season: Int, round: Int) async throws -> [RaceResult] {
        let response: JolpicaResponse<RaceTableData> = try await get("\(season)/\(round)/results.json", limit: 100)
        return response.data.raceTable.races.first?.results?.map { $0.toDomain() } ?? []
    }

    func driver(id: String) async throws -> Driver {
        let response: JolpicaResponse<DriverTableData> = try await get("drivers/\(id).json")
        guard let driver = response.data.driverTable.drivers.first else { throw NetworkError.decoding }
        return driver.toDomain()
    }

    func constructor(id: String) async throws -> Constructor {
        let response: JolpicaResponse<ConstructorTableData> = try await get("constructors/\(id).json")
        guard let constructor = response.data.constructorTable.constructors.first else { throw NetworkError.decoding }
        return constructor.toDomain()
    }

    /// Jolpica has no "career summary" endpoint, so statistics are derived from result counts.
    /// Poles are starts from grid slot 1 (the qualifying endpoint over-counts).
    /// For constructors, wins/podiums count car finishes (a 1-2 counts as one win and two podiums).
    func careerStats(kind: String, id: String) async throws -> CareerStats {
        async let seasons = count("\(kind)/\(id)/seasons.json")
        async let races = count("\(kind)/\(id)/results.json")
        async let wins = count("\(kind)/\(id)/results/1.json")
        async let seconds = count("\(kind)/\(id)/results/2.json")
        async let thirds = count("\(kind)/\(id)/results/3.json")
        async let poles = count("\(kind)/\(id)/grid/1/results.json")
        let (s, r, w, p2, p3, q) = try await (seasons, races, wins, seconds, thirds, poles)
        return CareerStats(seasons: s, races: r, wins: w, podiums: w + p2 + p3, poles: q)
    }

    private func count(_ path: String) async throws -> Int {
        let response: JolpicaResponse<TotalData> = try await get(path, limit: 1)
        return Int(response.data.total) ?? 0
    }

    private func get<T: Decodable>(_ path: String, limit: Int? = nil) async throws -> T {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        if let limit {
            components.queryItems = [URLQueryItem(name: "limit", value: String(limit))]
        }
        let data = try await http.get(components.url!)
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw NetworkError.decoding
        }
    }
}
