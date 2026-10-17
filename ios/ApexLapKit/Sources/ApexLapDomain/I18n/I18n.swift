import Foundation

public enum AppLanguage: String, CaseIterable, Codable, Sendable {
    case fr, en, es

    public var nativeName: String {
        switch self {
        case .fr: "Français"
        case .en: "English"
        case .es: "Español"
        }
    }

    public var locale: Locale { Locale(identifier: rawValue) }

    /// Language of the device, English when it is none of the supported ones.
    public static func fromSystem(_ preferred: [String] = Locale.preferredLanguages) -> AppLanguage {
        for tag in preferred {
            if let language = AppLanguage(rawValue: String(tag.prefix(2)).lowercased()) { return language }
        }
        return .en
    }
}

/// What the user picked in the settings: a language, or follow the device.
public enum LanguagePreference: String, CaseIterable, Codable, Sendable {
    case system, fr, en, es

    public var language: AppLanguage? { AppLanguage(rawValue: rawValue) }

    public func resolve(_ preferred: [String] = Locale.preferredLanguages) -> AppLanguage {
        language ?? AppLanguage.fromSystem(preferred)
    }
}

/// A piece of content written in the three languages.
public struct L: Hashable, Sendable {
    public let fr: String
    public let en: String
    public let es: String

    public init(_ fr: String, _ en: String, _ es: String) {
        self.fr = fr
        self.en = en
        self.es = es
    }

    public subscript(language: AppLanguage) -> String {
        switch language {
        case .fr: fr
        case .en: en
        case .es: es
        }
    }
}

/// Current language for code outside SwiftUI (notifications, widget, error messages, formatting).
/// SwiftUI views read the strings from the environment instead.
public enum I18n {
    private final class State: @unchecked Sendable {
        private let lock = NSLock()
        private var value = AppLanguage.fromSystem()

        var language: AppLanguage {
            get { lock.withLock { value } }
            set { lock.withLock { value = newValue } }
        }
    }

    private static let state = State()

    public static var language: AppLanguage { state.language }
    public static var strings: Strings { Strings(language) }

    public static func apply(_ preference: LanguagePreference) {
        state.language = preference.resolve()
    }

    public static func apply(_ language: AppLanguage) {
        state.language = language
    }
}
