import ApexLapData
import Foundation

/// OpenF1 access with in-memory caches: the free tier allows 30 requests a minute and past sessions never change.
public actor OpenF1Repository {
    private let client: OpenF1Client
    private let now: @Sendable () -> Date
    private var sessionsByYear: [Int: [LiveSession]] = [:]
    private var driversBySession: [Int: [LiveDriver]] = [:]
    private var replays: [Int: ReplayTimeline] = [:]
    private var lapsBySession: [Int: [Int: [LapRef]]] = [:]
    private var traces: [LapRef: LapTrace] = [:]
    private var locations: [LapRef: [TrackPoint]] = [:]

    public init(client: OpenF1Client = OpenF1Client(), now: @escaping @Sendable () -> Date = Date.init) {
        self.client = client
        self.now = now
    }

    /// Sessions that have started, most recent first.
    public func sessions(year: Int) async throws -> [LiveSession] {
        let all: [LiveSession]
        if let cached = sessionsByYear[year] {
            all = cached
        } else {
            let dtos: [SessionDTO] = try await client.get("sessions", [("year", "\(year)")])
            all = dtos.compactMap(\.domain)
            sessionsByYear[year] = all
        }
        let current = now()
        return all.filter { $0.start < current }.sorted { $0.start > $1.start }
    }

    public func drivers(session: Int) async throws -> [LiveDriver] {
        if let cached = driversBySession[session] { return cached }
        let dtos: [DriverDTO] = try await client.get("drivers", [("session_key", "\(session)")])
        var seen = Set<Int>()
        let drivers = dtos.map(\.domain).filter { seen.insert($0.number).inserted }.sorted { $0.number < $1.number }
        driversBySession[session] = drivers
        return drivers
    }

    /// Every timing event of a finished session, ready to be replayed.
    public func replay(_ session: LiveSession) async throws -> ReplayTimeline {
        if let cached = replays[session.key] { return cached }
        let events = try await loadEvents(key: "\(session.key)", token: nil, session: session)
        let timeline = ReplayTimeline.build(session: session, drivers: try await drivers(session: session.key), events: events)
        replays[session.key] = timeline
        return timeline
    }

    /// Current session through the authenticated REST API (`session_key=latest`), before the push feed takes over.
    public func liveSnapshot(token: String) async throws -> LiveTimingState {
        let sessions: [SessionDTO] = try await client.get("sessions", [("session_key", "latest")], token: token)
        guard let session = sessions.first?.domain else { throw NetworkError.decoding }
        let dtos: [DriverDTO] = try await client.get("drivers", [("session_key", "latest")], token: token)
        let events = try await loadEvents(key: "latest", token: token, session: session)
        return events.sorted { $0.date < $1.date }
            .reduce(LiveTimingState(session: session, drivers: dtos.map(\.domain)), LiveTimingReducer.reduce)
    }

    /// Timed laps of every driver in one request.
    public func laps(session: Int) async throws -> [Int: [LapRef]] {
        if let cached = lapsBySession[session] { return cached }
        let dtos: [LapDTO] = try await client.get("laps", [("session_key", "\(session)")])
        let laps = Dictionary(grouping: dtos.compactMap(\.ref), by: \.driver).mapValues { $0.sorted { $0.lapNumber < $1.lapNumber } }
        lapsBySession[session] = laps
        return laps
    }

    public func trace(session: Int, lap: LapRef) async throws -> LapTrace? {
        if let cached = traces[lap] { return cached }
        let dtos: [CarDataDTO] = try await client.get("car_data", window(session: session, lap: lap))
        let trace = LapTrace.build(lap: lap, samples: dtos.compactMap(\.sample))
        traces[lap] = trace
        return trace
    }

    public func location(session: Int, lap: LapRef) async throws -> [TrackPoint] {
        if let cached = locations[lap] { return cached }
        let dtos: [LocationDTO] = try await client.get("location", window(session: session, lap: lap))
        let points = dtos.compactMap(\.point)
        locations[lap] = points
        return points
    }

    private func window(session: Int, lap: LapRef) -> [(String, String)] {
        [
            ("session_key", "\(session)"),
            ("driver_number", "\(lap.driver)"),
            ("date>", OpenF1Date.format(lap.start.addingTimeInterval(-1))),
            ("date<", OpenF1Date.format(lap.start.addingTimeInterval(lap.duration + 1))),
        ]
    }

    private func loadEvents(key: String, token: String?, session: LiveSession) async throws -> [TimingEvent] {
        let query = [("session_key", key)]
        var events: [TimingEvent] = []
        let positions: [PositionDTO] = try await client.get("position", query, token: token)
        events += positions.compactMap(\.event)
        // Intervals only exist for races.
        if session.isRace {
            let intervals: [IntervalDTO] = try await client.get("intervals", query, token: token)
            events += intervals.compactMap(\.event)
        }
        let laps: [LapDTO] = try await client.get("laps", query, token: token)
        events += laps.compactMap { $0.event(notBefore: session.start.addingTimeInterval(-3600)) }
        let stints: [StintDTO] = try await client.get("stints", query, token: token)
        events += stints.map { $0.event(at: session.start.addingTimeInterval(-120)) }
        let raceControl: [RaceControlDTO] = try await client.get("race_control", query, token: token)
        events += raceControl.compactMap(\.event)
        let pits: [PitDTO] = try await client.get("pit", query, token: token)
        events += pits.compactMap(\.event)
        return events
    }
}

/// Exchanges sponsor credentials for an OAuth2 token (valid one hour) and renews it before expiry.
public actor OpenF1TokenProvider {
    public typealias Credentials = @Sendable () async -> (username: String, password: String)?
    private let client: OpenF1Client
    private let credentials: Credentials
    private var cached: (token: String, expiry: Date)?

    public init(client: OpenF1Client = OpenF1Client(), credentials: @escaping Credentials) {
        self.client = client
        self.credentials = credentials
    }

    /// Nil when no sponsor account is configured.
    public func token() async throws -> String? {
        if let cached, cached.expiry.addingTimeInterval(-120) > Date() { return cached.token }
        guard let account = await credentials() else { return nil }
        let (token, lifetime) = try await client.token(username: account.username, password: account.password)
        cached = (token, Date().addingTimeInterval(lifetime))
        return token
    }

    public func invalidate() { cached = nil }
}
