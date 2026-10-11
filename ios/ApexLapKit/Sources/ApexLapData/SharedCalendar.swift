import ApexLapDomain
import Foundation

/// The season calendar shared with the home-screen widget through an App Group container.
public enum SharedCalendar {
    public static let appGroup = "group.com.apexlap.ios"
    private static let key = "calendar"

    private static var defaults: UserDefaults { UserDefaults(suiteName: appGroup) ?? .standard }

    public static func save(_ races: [Race]) {
        guard let data = try? JSONEncoder().encode(races) else { return }
        defaults.set(data, forKey: key)
    }

    public static func load() -> [Race] {
        guard let data = defaults.data(forKey: key) else { return [] }
        return (try? JSONDecoder().decode([Race].self, from: data)) ?? []
    }
}
