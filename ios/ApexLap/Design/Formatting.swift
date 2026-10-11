import Foundation

enum Format {
    private static let french = Locale(identifier: "fr_FR")

    static func day(_ date: Date) -> String {
        date.formatted(.dateTime.locale(french).weekday(.abbreviated).day().month(.abbreviated)).capitalized
    }

    /// Session days are stored as midnight UTC, so they must be formatted in UTC.
    static func utcDay(_ date: Date) -> String {
        var style = Date.FormatStyle.dateTime.locale(french).weekday(.abbreviated).day().month(.abbreviated)
        style.timeZone = TimeZone(identifier: "UTC")!
        return date.formatted(style).capitalized
    }

    static func shortUTCDay(_ date: Date) -> String {
        var style = Date.FormatStyle.dateTime.locale(french).day().month(.abbreviated)
        style.timeZone = TimeZone(identifier: "UTC")!
        return date.formatted(style)
    }

    static func time(_ date: Date) -> String {
        date.formatted(.dateTime.locale(french).hour(.twoDigits(amPM: .omitted)).minute(.twoDigits))
    }

    static func points(_ value: Double) -> String {
        value.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(value)) : String(format: "%.1f", value)
    }

    /// Flag emoji for the countries / nationalities that appear in F1 data.
    static func flag(_ countryOrNationality: String) -> String {
        guard let code = countryCodes[countryOrNationality] else { return "🏁" }
        return code.unicodeScalars
            .compactMap { UnicodeScalar(0x1F1E6 + $0.value - 65) }
            .map(String.init)
            .joined()
    }

    private static let countryCodes: [String: String] = [
        "Australia": "AU", "Australian": "AU", "China": "CN", "Chinese": "CN", "Japan": "JP",
        "Japanese": "JP", "Bahrain": "BH", "Saudi Arabia": "SA", "USA": "US", "United States": "US",
        "American": "US", "Italy": "IT", "Italian": "IT", "Monaco": "MC", "Monegasque": "MC",
        "Spain": "ES", "Spanish": "ES", "Canada": "CA", "Canadian": "CA", "Austria": "AT",
        "Austrian": "AT", "UK": "GB", "United Kingdom": "GB", "British": "GB", "Belgium": "BE",
        "Belgian": "BE", "Hungary": "HU", "Hungarian": "HU", "Netherlands": "NL", "Dutch": "NL",
        "Azerbaijan": "AZ", "Singapore": "SG", "Mexico": "MX", "Mexican": "MX", "Brazil": "BR",
        "Brazilian": "BR", "Qatar": "QA", "UAE": "AE", "Abu Dhabi": "AE", "France": "FR",
        "French": "FR", "Germany": "DE", "German": "DE", "Finland": "FI", "Finnish": "FI",
        "Thailand": "TH", "Thai": "TH", "Argentina": "AR", "Argentine": "AR", "Argentinian": "AR",
        "New Zealand": "NZ", "New Zealander": "NZ", "Danish": "DK", "Denmark": "DK", "Swiss": "CH",
        "Switzerland": "CH", "Portugal": "PT", "Polish": "PL", "Russian": "RU", "Swedish": "SE",
    ]
}
