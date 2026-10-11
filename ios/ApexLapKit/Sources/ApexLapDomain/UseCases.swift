import Foundation

public struct UpcomingSession: Hashable, Sendable {
    public let race: Race
    public let session: Session
}

public enum CalendarLogic {
    /// The next session that has not started yet across the whole calendar.
    public static func nextSession(in calendar: [Race], now: Date) -> UpcomingSession? {
        calendar
            .flatMap { race in race.sessions.map { UpcomingSession(race: race, session: $0) } }
            .filter { ($0.session.startsAt ?? .distantPast) > now }
            .min { $0.session.startsAt! < $1.session.startsAt! }
    }

    /// The next race weekend: the first race whose main race has not finished.
    public static func nextRace(in calendar: [Race], now: Date) -> Race? {
        calendar.sorted { $0.round < $1.round }.first { !$0.isFinished(at: now) }
    }

    public static func countdown(to target: Date, now: Date) -> Countdown {
        let total = max(0, Int(target.timeIntervalSince(now)))
        return Countdown(
            days: total / 86_400,
            hours: total % 86_400 / 3_600,
            minutes: total % 3_600 / 60,
            seconds: total % 60
        )
    }

    public static func currentSeason(now: Date) -> Int {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar.component(.year, from: now)
    }
}

public enum StandingsFilter {
    public static func drivers(_ rows: [DriverStanding], matching query: String) -> [DriverStanding] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return rows }
        return rows.filter {
            $0.driver.fullName.localizedCaseInsensitiveContains(q)
                || $0.driver.code.localizedCaseInsensitiveContains(q)
                || ($0.constructor?.name.localizedCaseInsensitiveContains(q) ?? false)
        }
    }

    public static func constructors(_ rows: [ConstructorStanding], matching query: String) -> [ConstructorStanding] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return rows }
        return rows.filter { $0.constructor.name.localizedCaseInsensitiveContains(q) }
    }
}
