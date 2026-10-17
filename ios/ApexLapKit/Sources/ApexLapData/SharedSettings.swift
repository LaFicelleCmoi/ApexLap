import ApexLapDomain
import Foundation

/// App-wide settings: language and favourites. Reminders keep their own store.
public struct AppSettings: Codable, Hashable, Sendable {
    public var language: LanguagePreference
    /// Jolpica ids of the driver and team the user follows.
    public var favoriteDriverId: String?
    public var favoriteConstructorId: String?

    public init(language: LanguagePreference = .system, favoriteDriverId: String? = nil, favoriteConstructorId: String? = nil) {
        self.language = language
        self.favoriteDriverId = favoriteDriverId
        self.favoriteConstructorId = favoriteConstructorId
    }

    public var hasFavorites: Bool { favoriteDriverId != nil || favoriteConstructorId != nil }
}

/// Settings and the current standings, shared with the widget through the App Group container.
public enum SharedSettings {
    private static let settingsKey = "settings"
    private static let driversKey = "driverStandings"
    private static let constructorsKey = "constructorStandings"

    private static var defaults: UserDefaults { UserDefaults(suiteName: SharedCalendar.appGroup) ?? .standard }

    public static func load() -> AppSettings {
        guard let data = defaults.data(forKey: settingsKey) else { return AppSettings() }
        return (try? JSONDecoder().decode(AppSettings.self, from: data)) ?? AppSettings()
    }

    public static func save(_ settings: AppSettings) {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: settingsKey)
    }

    public static func saveStandings(drivers: [DriverStanding], constructors: [ConstructorStanding]) {
        if let data = try? JSONEncoder().encode(drivers) { defaults.set(data, forKey: driversKey) }
        if let data = try? JSONEncoder().encode(constructors) { defaults.set(data, forKey: constructorsKey) }
    }

    public static func driverStandings() -> [DriverStanding] {
        defaults.data(forKey: driversKey).flatMap { try? JSONDecoder().decode([DriverStanding].self, from: $0) } ?? []
    }

    public static func constructorStandings() -> [ConstructorStanding] {
        defaults.data(forKey: constructorsKey).flatMap { try? JSONDecoder().decode([ConstructorStanding].self, from: $0) } ?? []
    }
}
