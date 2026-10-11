import ApexLapData
import ApexLapDomain
import Foundation
import Observation
import UserNotifications
import WidgetKit

/// Local notifications before sessions. Idempotent: re-plans pending requests from the cached calendar.
@MainActor
@Observable
final class ReminderScheduler {
    static let shared = ReminderScheduler()
    private static let settingsKey = "reminderSettings"
    private static let prefix = "session-"

    private(set) var settings: ReminderSettings
    private(set) var authorization: UNAuthorizationStatus = .notDetermined

    private init() {
        settings = UserDefaults.standard.data(forKey: Self.settingsKey)
            .flatMap { try? JSONDecoder().decode(ReminderSettings.self, from: $0) } ?? ReminderSettings()
    }

    func refreshAuthorization() async {
        authorization = await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }

    /// Asks for permission when enabling; returns false if the user declined.
    func update(_ transform: (inout ReminderSettings) -> Void) async -> Bool {
        var next = settings
        transform(&next)
        if next.enabled && !settings.enabled {
            let granted = (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])) ?? false
            await refreshAuthorization()
            if !granted { return false }
        }
        settings = next
        UserDefaults.standard.set(try? JSONEncoder().encode(next), forKey: Self.settingsKey)
        await reschedule()
        return true
    }

    func upcoming(now: Date = .now) -> [PlannedReminder] {
        ReminderPlanner.plan(SharedCalendar.load(), now: now, settings: settings)
    }

    /// Called on launch, when returning to the foreground, after a calendar refresh and on settings changes.
    func reschedule() async {
        let center = UNUserNotificationCenter.current()
        let planned = upcoming()
        let pending = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix(Self.prefix) }
        let keep = Set(planned.map { Self.prefix + $0.id })
        center.removePendingNotificationRequests(withIdentifiers: pending.filter { !keep.contains($0) })
        for reminder in planned {
            guard let start = reminder.session.startsAt else { continue }
            let content = UNMutableNotificationContent()
            content.title = "\(Format.flag(reminder.race.circuit.country)) \(reminder.session.type.label) · \(reminder.race.name)"
            content.body = "Départ à \(Format.time(start)) — dans \(settings.leadMinutes) min"
            content.sound = .default
            content.userInfo = ["season": reminder.race.season, "round": reminder.race.round]
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, reminder.fireAt.timeIntervalSinceNow), repeats: false)
            try? await center.add(UNNotificationRequest(identifier: Self.prefix + reminder.id, content: content, trigger: trigger))
        }
        WidgetCenter.shared.reloadAllTimelines()
    }
}
