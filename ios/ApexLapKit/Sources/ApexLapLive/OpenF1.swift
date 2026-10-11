import ApexLapData
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

/// OpenF1 dates: ISO-8601 with offset and 0–6 fractional digits ("2026-10-11T12:03:44.511000+00:00").
public enum OpenF1Date {
    public static func parse(_ value: String) -> Date? {
        let parts = value.split(separator: "T", maxSplits: 1)
        guard parts.count == 2 else { return nil }
        let day = parts[0].split(separator: "-").compactMap { Int($0) }
        guard day.count == 3 else { return nil }
        var time = String(parts[1])
        var offset = 0
        if time.hasSuffix("Z") {
            time.removeLast()
        } else if let sign = time.lastIndex(where: { $0 == "+" || $0 == "-" }) {
            let zone = time[time.index(after: sign)...].split(separator: ":").compactMap { Int($0) }
            offset = ((zone.first ?? 0) * 3600 + (zone.count > 1 ? zone[1] * 60 : 0)) * (time[sign] == "-" ? -1 : 1)
            time = String(time[..<sign])
        }
        let clock = time.split(separator: ":")
        guard clock.count == 3, let h = Int(clock[0]), let m = Int(clock[1]), let s = Double(clock[2]) else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        guard let midnight = calendar.date(from: DateComponents(year: day[0], month: day[1], day: day[2])) else { return nil }
        return midnight.addingTimeInterval(Double(h * 3600 + m * 60) + s - Double(offset))
    }

    /// Filter values for `date>` / `date<` query parameters.
    public static func format(_ date: Date) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: date)
    }
}

/// Decodes `gap_to_leader` / `interval`: a number of seconds, a string such as "+1 LAP", or null.
struct GapValue: Decodable {
    let gap: Gap?
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() { gap = nil }
        else if let seconds = try? container.decode(Double.self) { gap = .seconds(seconds) }
        else { gap = (try? container.decode(String.self)).map(Gap.text) }
    }
}

struct SessionDTO: Decodable {
    let session_key: Int
    let session_name: String
    let session_type: String
    let date_start: String
    let date_end: String
    let circuit_short_name: String
    let country_name: String

    var domain: LiveSession? {
        guard let start = OpenF1Date.parse(date_start), let end = OpenF1Date.parse(date_end) else { return nil }
        return LiveSession(key: session_key, name: session_name, type: session_type, start: start, end: end,
                           circuit: circuit_short_name, country: country_name)
    }
}

struct DriverDTO: Decodable {
    let driver_number: Int
    let name_acronym: String?
    let full_name: String?
    let team_name: String?
    let team_colour: String?

    var domain: LiveDriver {
        LiveDriver(number: driver_number, acronym: name_acronym ?? "#\(driver_number)", fullName: full_name ?? "Pilote \(driver_number)",
                   team: team_name ?? "", teamColour: team_colour.flatMap { UInt32($0, radix: 16) })
    }
}

struct PositionDTO: Decodable {
    let date: String
    let driver_number: Int
    let position: Int
    var event: TimingEvent? { OpenF1Date.parse(date).map { .position(date: $0, driver: driver_number, position: position) } }
}

struct IntervalDTO: Decodable {
    let date: String
    let driver_number: Int
    let gap_to_leader: GapValue?
    let interval: GapValue?
    var event: TimingEvent? {
        OpenF1Date.parse(date).map { .interval(date: $0, driver: driver_number, gapToLeader: gap_to_leader?.gap, interval: interval?.gap) }
    }
}

struct LapDTO: Decodable {
    let driver_number: Int
    let lap_number: Int
    let date_start: String?
    let lap_duration: Double?
    let duration_sector_1: Double?
    let duration_sector_2: Double?
    let duration_sector_3: Double?

    var sectors: [Double?] { [duration_sector_1, duration_sector_2, duration_sector_3] }

    /// Stamped at lap completion so a replay reveals the time when it was set.
    func event(notBefore floor: Date) -> TimingEvent? {
        guard let start = date_start.flatMap(OpenF1Date.parse) else { return nil }
        let completed = start.addingTimeInterval(lap_duration ?? 0)
        return .lap(date: max(completed, floor), driver: driver_number, lapNumber: lap_number, duration: lap_duration, sectors: sectors)
    }

    var ref: LapRef? {
        guard let start = date_start.flatMap(OpenF1Date.parse), let duration = lap_duration else { return nil }
        return LapRef(driver: driver_number, lapNumber: lap_number, start: start, duration: duration, sectors: sectors)
    }
}

struct StintDTO: Decodable {
    let driver_number: Int
    let stint_number: Int
    let compound: String?
    let lap_start: Int?
    let lap_end: Int?
    let tyre_age_at_start: Int?
    func event(at date: Date) -> TimingEvent {
        .stint(date: date, driver: driver_number, stint: Stint(number: stint_number, compound: TyreCompound(apiValue: compound),
                                                              lapStart: lap_start ?? 1, lapEnd: lap_end, tyreAgeAtStart: tyre_age_at_start ?? 0))
    }
}

struct RaceControlDTO: Decodable {
    let date: String
    let flag: String?
    let scope: String?
    let message: String
    let lap_number: Int?
    var event: TimingEvent? {
        OpenF1Date.parse(date).map { .raceControl(date: $0, flag: flag, scope: scope, message: message, lap: lap_number) }
    }
}

struct PitDTO: Decodable {
    let date: String
    let driver_number: Int
    let lap_number: Int?
    var event: TimingEvent? { OpenF1Date.parse(date).map { .pitStop(date: $0, driver: driver_number, lap: lap_number) } }
}

struct CarDataDTO: Decodable {
    let date: String
    let speed: Int?
    let throttle: Int?
    let brake: Int?
    let n_gear: Int?
    var sample: CarSample? {
        guard let date = OpenF1Date.parse(date), let speed else { return nil }
        return CarSample(date: date, speed: Double(speed), throttle: Double(throttle ?? 0), brake: Double(brake ?? 0), gear: n_gear ?? 0)
    }
}

struct LocationDTO: Decodable {
    let date: String
    let x: Int
    let y: Int
    var point: TrackPoint? { OpenF1Date.parse(date).map { TrackPoint(date: $0, x: Double(x), y: Double(y)) } }
}

struct TokenDTO: Decodable {
    let access_token: String
    let expires_in: String?
}

/// Messages of the live MQTT feed, one per topic, mirroring the REST payloads.
public enum OpenF1Live {
    public static let websocketURL = URL(string: "wss://mqtt.openf1.org:8084/mqtt")!
    public static let topics = ["v1/position", "v1/intervals", "v1/laps", "v1/stints", "v1/race_control", "v1/pit"]

    public static func parse(topic: String, payload: Data, now: Date) -> TimingEvent? {
        let decoder = JSONDecoder()
        switch topic {
        case "v1/position": return (try? decoder.decode(PositionDTO.self, from: payload))?.event
        case "v1/intervals": return (try? decoder.decode(IntervalDTO.self, from: payload))?.event
        case "v1/laps": return (try? decoder.decode(LapDTO.self, from: payload))?.event(notBefore: now)
        case "v1/stints": return (try? decoder.decode(StintDTO.self, from: payload))?.event(at: now)
        case "v1/race_control": return (try? decoder.decode(RaceControlDTO.self, from: payload))?.event
        case "v1/pit": return (try? decoder.decode(PitDTO.self, from: payload))?.event
        default: return nil
        }
    }
}

/// OpenF1 REST API. Historical data is free (3 req/s, 30 req/min); live data needs a sponsor token.
public struct OpenF1Client: Sendable {
    public static let baseURL = URL(string: "https://api.openf1.org/")!
    private let http: HTTPClient

    public init(http: HTTPClient = ThrottledHTTPClient(URLSessionHTTPClient(), minInterval: 0.35, maxPerMinute: 30)) {
        self.http = http
    }

    func get<T: Decodable>(_ endpoint: String, _ query: [(String, String)], token: String? = nil) async throws -> [T] {
        var components = URLComponents(url: Self.baseURL.appendingPathComponent("v1/\(endpoint)"), resolvingAgainstBaseURL: false)!
        components.queryItems = query.map { URLQueryItem(name: $0.0, value: $0.1) }
        var request = URLRequest(url: components.url!, timeoutInterval: 60)
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        do {
            let data = try await http.send(request)
            return try JSONDecoder().decode([T].self, from: data)
        } catch NetworkError.http(status: 404) {
            // OpenF1 answers 404 {"detail":"No results found."} for empty queries.
            return []
        } catch is DecodingError {
            throw NetworkError.decoding
        }
    }

    public func token(username: String, password: String) async throws -> (token: String, lifetime: TimeInterval) {
        var request = URLRequest(url: Self.baseURL.appendingPathComponent("token"))
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        var form = URLComponents()
        form.queryItems = [URLQueryItem(name: "username", value: username), URLQueryItem(name: "password", value: password)]
        request.httpBody = form.percentEncodedQuery?.data(using: .utf8)
        let data = try await http.send(request)
        guard let dto = try? JSONDecoder().decode(TokenDTO.self, from: data) else { throw NetworkError.decoding }
        return (dto.access_token, TimeInterval(dto.expires_in ?? "") ?? 3600)
    }
}
