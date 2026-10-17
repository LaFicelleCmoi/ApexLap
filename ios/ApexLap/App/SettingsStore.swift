import ApexLapData
import ApexLapDomain
import Observation
import SwiftUI
import WidgetKit

/// Language and favourites, persisted in the App Group so the widget follows them.
@MainActor
@Observable
final class SettingsStore {
    static let shared = SettingsStore()

    private(set) var settings: AppSettings
    /// Resolved language (the phone's one when "system" is picked).
    private(set) var language: AppLanguage

    private init() {
        let saved = SharedSettings.load()
        settings = saved
        language = saved.language.resolve()
        I18n.apply(language)
    }

    var strings: Strings { Strings(language) }

    func setLanguage(_ preference: LanguagePreference) {
        update { $0.language = preference }
    }

    func setFavoriteDriver(_ id: String?) { update { $0.favoriteDriverId = id } }
    func setFavoriteConstructor(_ id: String?) { update { $0.favoriteConstructorId = id } }

    func toggleDriver(_ id: String) { setFavoriteDriver(settings.favoriteDriverId == id ? nil : id) }
    func toggleConstructor(_ id: String) { setFavoriteConstructor(settings.favoriteConstructorId == id ? nil : id) }

    /// The phone language may change while the app is in the background.
    func refreshSystemLanguage() {
        let resolved = settings.language.resolve()
        guard resolved != language else { return }
        language = resolved
        I18n.apply(resolved)
        WidgetCenter.shared.reloadAllTimelines()
    }

    private func update(_ transform: (inout AppSettings) -> Void) {
        var next = settings
        transform(&next)
        guard next != settings else { return }
        settings = next
        language = next.language.resolve()
        I18n.apply(language)
        SharedSettings.save(next)
        WidgetCenter.shared.reloadAllTimelines()
        Task { await ReminderScheduler.shared.reschedule() }
    }
}

private struct StringsKey: EnvironmentKey {
    static let defaultValue = Strings(.en)
}

private struct EncyclopediaKey: EnvironmentKey {
    static let defaultValue = EncyclopediaRepository()
}

extension EnvironmentValues {
    /// Texts in the language picked in the settings.
    var strings: Strings {
        get { self[StringsKey.self] }
        set { self[StringsKey.self] = newValue }
    }

    var encyclopedia: EncyclopediaRepository {
        get { self[EncyclopediaKey.self] }
        set { self[EncyclopediaKey.self] = newValue }
    }
}
