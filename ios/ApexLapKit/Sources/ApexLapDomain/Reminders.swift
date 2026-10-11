import Foundation

public enum ReminderScope: String, CaseIterable, Codable, Sendable {
    case allSessions, competitive, racesOnly

    public var label: String {
        switch self {
        case .allSessions: "Toutes les séances"
        case .competitive: "Qualifs, sprints et courses"
        case .racesOnly: "Courses uniquement"
        }
    }

    public func includes(_ type: SessionType) -> Bool {
        switch self {
        case .allSessions: true
        case .competitive: ![.practice1, .practice2, .practice3].contains(type)
        case .racesOnly: type == .race
        }
    }
}

public struct ReminderSettings: Codable, Hashable, Sendable {
    public static let leadOptions = [5, 15, 30, 60]

    public var enabled: Bool
    public var leadMinutes: Int
    public var scope: ReminderScope

    public init(enabled: Bool = false, leadMinutes: Int = 15, scope: ReminderScope = .competitive) {
        self.enabled = enabled
        self.leadMinutes = leadMinutes
        self.scope = scope
    }
}

public struct PlannedReminder: Hashable, Sendable, Identifiable {
    /// Stable per session, so re-planning replaces pending notifications instead of duplicating them.
    public let id: String
    public let race: Race
    public let session: Session
    public let fireAt: Date
}

public enum ReminderPlanner {
    public static let defaultHorizon: TimeInterval = 14 * 24 * 3600

    /// Sessions to remind about within `horizon`, and when. A reminder whose time has passed is dropped, not fired late.
    public static func plan(_ calendar: [Race], now: Date, settings: ReminderSettings, horizon: TimeInterval = defaultHorizon) -> [PlannedReminder] {
        guard settings.enabled else { return [] }
        let lead = TimeInterval(settings.leadMinutes * 60)
        return calendar.flatMap { race in
            race.sessions.compactMap { session -> PlannedReminder? in
                guard let start = session.startsAt, settings.scope.includes(session.type) else { return nil }
                let fire = start.addingTimeInterval(-lead)
                guard fire > now, start <= now.addingTimeInterval(horizon) else { return nil }
                return PlannedReminder(id: "\(race.season)-\(race.round)-\(session.type.rawValue)", race: race, session: session, fireAt: fire)
            }
        }
        .sorted { $0.fireAt < $1.fireAt }
    }

    /// "dans 2 j 4 h", "dans 3 h 05", "dans 45 min", "maintenant".
    public static func relativeStart(from: Date, to: Date) -> String {
        let minutes = Int(to.timeIntervalSince(from) / 60)
        if minutes <= 0 { return "maintenant" }
        if minutes < 60 { return "dans \(minutes) min" }
        if minutes < 24 * 60 {
            let rest = minutes % 60
            return "dans \(minutes / 60) h" + (rest > 0 ? String(format: " %02d", rest) : "")
        }
        let hours = (minutes / 60) % 24
        return "dans \(minutes / (24 * 60)) j" + (hours > 0 ? " \(hours) h" : "")
    }
}
