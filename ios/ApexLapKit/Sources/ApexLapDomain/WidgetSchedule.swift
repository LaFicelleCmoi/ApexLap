import Foundation

public enum SessionPhase: Sendable {
    case done, live, next, later
}

public struct WeekendSession: Hashable, Sendable {
    public let session: Session
    public let phase: SessionPhase
}

/// What the home-screen widget shows at a given instant (same rules as Android's `WidgetState.kt`).
public struct WidgetSnapshot: Sendable {
    public let date: Date
    public let hasCalendar: Bool
    /// Weekend on show: the one in progress, else the next one. Nil once the season is over.
    public let race: Race?
    public let sessions: [WeekendSession]
    /// Session running right now.
    public let live: Session?
    /// Next session to start, and its weekend (the following one when the current weekend is over).
    public let next: Session?
    public let nextRace: Race?
}

extension SessionType {
    /// Typical length of each session, to know when one is live and when it is over.
    public var typicalDuration: TimeInterval {
        switch self {
        case .race: 120 * 60
        case .sprintQualifying: 45 * 60
        default: 60 * 60
        }
    }
}

extension Session {
    public var endsAt: Date? { startsAt?.addingTimeInterval(type.typicalDuration) }
}

public enum WidgetSchedule {
    public static func snapshot(_ calendar: [Race], at now: Date) -> WidgetSnapshot {
        let all = calendar.sorted { $0.round < $1.round }.flatMap { race in race.sessions.map { (race, $0) } }
        let live = all.first { _, s in
            guard let start = s.startsAt, let end = s.endsAt else { return false }
            return start <= now && now < end
        }
        let next = all.filter { ($0.1.startsAt ?? .distantPast) > now }.min { $0.1.startsAt! < $1.1.startsAt! }
        guard let race = live?.0 ?? next?.0 else {
            return WidgetSnapshot(date: now, hasCalendar: !calendar.isEmpty, race: nil, sessions: [], live: nil, next: nil, nextRace: nil)
        }
        var nextMarked = false
        let sessions = race.sessions.sorted { ($0.startsAt ?? $0.day) < ($1.startsAt ?? $1.day) }.map { session -> WeekendSession in
            let phase: SessionPhase
            if session == live?.1 {
                phase = .live
            } else if let end = session.endsAt, end <= now {
                phase = .done
            } else if !nextMarked && session == next?.1 {
                phase = .next
                nextMarked = true
            } else {
                phase = .later
            }
            return WeekendSession(session: session, phase: phase)
        }
        return WidgetSnapshot(date: now, hasCalendar: true, race: race, sessions: sessions, live: live?.1, next: next?.1, nextRace: next?.0)
    }

    /// Instants at which the widget must redraw (a session starts or ends), so WidgetKit can switch between
    /// "countdown" and "live" on its own. The countdown text itself is rendered live by the system.
    public static func timeline(_ calendar: [Race], from now: Date, horizon: TimeInterval = 24 * 3600) -> [Date] {
        let limit = now.addingTimeInterval(horizon)
        let changes = calendar.flatMap(\.sessions).flatMap { [$0.startsAt, $0.endsAt] }
            .compactMap { $0 }
            .filter { $0 > now && $0 <= limit }
        return [now] + Array(Set(changes)).sorted()
    }
}
