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
    func send(_ request: URLRequest) async throws -> Data
}

extension HTTPClient {
    public func get(_ url: URL) async throws -> Data {
        try await send(URLRequest(url: url, timeoutInterval: 30))
    }
}

public struct URLSessionHTTPClient: HTTPClient {
    private let session: URLSession

    public init(session: URLSession = .shared) {
        self.session = session
    }

    public func send(_ request: URLRequest) async throws -> Data {
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

    /// Jolpica allows ~4 requests/second; requests are spaced and HTTP 429 retried.
    public init(http: HTTPClient = ThrottledHTTPClient(URLSessionHTTPClient(), minInterval: 0.3), baseURL: URL = JolpicaClient.baseURL) {
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

    func sprintResults(season: Int, round: Int) async throws -> [RaceResult] {
        let response: JolpicaResponse<RaceTableData> = try await get("\(season)/\(round)/sprint.json", limit: 100)
        return response.data.raceTable.races.first?.sprintResults?.map { $0.toDomain() } ?? []
    }

    func qualifying(season: Int, round: Int) async throws -> [QualifyingResult] {
        let response: JolpicaResponse<RaceTableData> = try await get("\(season)/\(round)/qualifying.json", limit: 100)
        return (response.data.raceTable.races.first?.qualifyingResults?.map { $0.toDomain() } ?? []).sorted { $0.position < $1.position }
    }

    /// Every race and sprint result of a season, walking the 100-row pages and dropping rows repeated across pages.
    func seasonResults(season: Int) async throws -> [ScoredResult] {
        var rows: [ScoredResult] = []
        for (path, isSprint) in [("results", false), ("sprint", true)] {
            var offset = 0
            var total = 0
            repeat {
                let page: JolpicaResponse<RaceTableData> = try await get("\(season)/\(path).json", limit: 100, offset: offset)
                for race in page.data.raceTable.races {
                    let results = (isSprint ? race.sprintResults : race.results) ?? []
                    rows += results.map { ScoredResult(round: Int(race.round) ?? 0, isSprint: isSprint, result: $0.toDomain()) }
                }
                total = Int(page.data.total) ?? 0
                offset += 100
            } while offset < total
        }
        var seen = Set<String>()
        return rows.filter { seen.insert("\($0.round)-\($0.isSprint)-\($0.result.driver.id)").inserted }
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

    // MARK: Encyclopedia

    /// All-time drivers (≈900) or constructors, walking the 100-row pages.
    func allDrivers() async throws -> [Driver] {
        try await paged { offset in
            let page: JolpicaResponse<DriverTableData> = try await get("drivers.json", limit: 100, offset: offset)
            return (page.data.driverTable.drivers.map { $0.toDomain() }, Int(page.data.total ?? "") ?? 0)
        }
    }

    func allConstructors() async throws -> [Constructor] {
        try await paged { offset in
            let page: JolpicaResponse<ConstructorTableData> = try await get("constructors.json", limit: 100, offset: offset)
            return (page.data.constructorTable.constructors.map { $0.toDomain() }, Int(page.data.total ?? "") ?? 0)
        }
    }

    func allCircuits() async throws -> [Circuit] {
        try await paged { offset in
            let page: JolpicaResponse<CircuitTableData> = try await get("circuits.json", limit: 100, offset: offset)
            return (page.data.circuitTable.circuits.map { $0.toDomain() }, Int(page.data.total ?? "") ?? 0)
        }
    }

    /// Races of `path` (`circuits/{id}/results/1`, `{season}/results/1`…) with their single result row.
    func racesWithFirstResult(_ path: String) async throws -> [(race: Race, result: RaceResult)] {
        let response: JolpicaResponse<RaceTableData> = try await get("\(path).json", limit: 100)
        return response.data.raceTable.races.compactMap { dto in
            guard let result = dto.results?.first?.toDomain() else { return nil }
            return (dto.toDomain(), result)
        }
    }

    private func paged<T>(_ page: (Int) async throws -> ([T], Int)) async throws -> [T] {
        var rows: [T] = []
        var offset = 0
        var total = 0
        repeat {
            let (items, count) = try await page(offset)
            rows += items
            total = count
            offset += 100
        } while offset < total
        return rows
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

    private func get<T: Decodable>(_ path: String, limit: Int? = nil, offset: Int? = nil) async throws -> T {
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        var items: [URLQueryItem] = []
        if let limit { items.append(URLQueryItem(name: "limit", value: String(limit))) }
        if let offset { items.append(URLQueryItem(name: "offset", value: String(offset))) }
        if !items.isEmpty { components.queryItems = items }
        let data = try await http.get(components.url!)
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw NetworkError.decoding
        }
    }
}
