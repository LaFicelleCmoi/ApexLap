import Foundation

public enum Bios {
    /// "2008, 2014–2020": consecutive seasons collapsed into ranges.
    public static func formatSeasons(_ seasons: [Int]) -> String {
        var ranges: [ClosedRange<Int>] = []
        for season in Set(seasons).sorted() {
            if let last = ranges.last, last.upperBound + 1 == season {
                ranges[ranges.count - 1] = last.lowerBound...season
            } else {
                ranges.append(season...season)
            }
        }
        return ranges.map { $0.lowerBound == $0.upperBound ? "\($0.lowerBound)" : "\($0.lowerBound)–\($0.upperBound)" }
            .joined(separator: ", ")
    }

    private static func points(_ value: Double) -> String {
        value.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(value)) : String(value)
    }

    private struct Words {
        let language: AppLanguage
        func t(_ fr: String, _ en: String, _ es: String) -> String {
            switch language {
            case .fr: fr
            case .en: en
            case .es: es
            }
        }
        func count(_ n: Int, _ one: String, _ many: String) -> String { "\(n) \(n > 1 ? many : one)" }
    }

    /// Short biography generated from the data we already have (same wording as Android).
    public static func driver(_ profile: DriverProfile, titles: Titles, standing: DriverStanding?, season: Int,
                              today: Date, language: AppLanguage) -> String {
        let w = Words(language: language)
        let driver = profile.driver
        let stats = profile.stats
        var sentences: [String] = []
        if let birth = driver.dateOfBirth {
            var utc = Calendar(identifier: .gregorian)
            utc.timeZone = TimeZone(identifier: "UTC")!
            let age = utc.dateComponents([.year], from: birth, to: today).year ?? 0
            var style = Date.FormatStyle.dateTime.day().month(.wide).year().locale(language.locale)
            style.timeZone = TimeZone(identifier: "UTC")!
            let date = birth.formatted(style)
            sentences.append(w.t("\(driver.fullName), né le \(date) (\(age) ans).",
                                 "\(driver.fullName), born \(date) (\(age) years old).",
                                 "\(driver.fullName), nacido el \(date) (\(age) años)."))
        } else {
            sentences.append("\(driver.fullName).")
        }
        if titles.count > 0 {
            let seasons = formatSeasons(titles.seasons)
            sentences.append(titles.count == 1
                ? w.t("Champion du monde (\(seasons)).", "World champion (\(seasons)).", "Campeón del mundo (\(seasons)).")
                : w.t("\(titles.count) fois champion du monde (\(seasons)).", "\(titles.count)-time world champion (\(seasons)).",
                      "\(titles.count) veces campeón del mundo (\(seasons))."))
        }
        if stats.races > 0 {
            var achievements: [String] = []
            if stats.wins > 0 { achievements.append(w.count(stats.wins, w.t("victoire", "win", "victoria"), w.t("victoires", "wins", "victorias"))) }
            if stats.podiums > 0 { achievements.append(w.count(stats.podiums, w.t("podium", "podium", "podio"), w.t("podiums", "podiums", "podios"))) }
            if stats.poles > 0 { achievements.append(w.count(stats.poles, "pole position", w.t("pole positions", "pole positions", "poles"))) }
            let races = w.count(stats.races, w.t("Grand Prix", "Grand Prix", "Gran Premio"), w.t("Grands Prix", "Grands Prix", "Grandes Premios"))
            let seasons = w.count(stats.seasons, w.t("saison", "season", "temporada"), w.t("saisons", "seasons", "temporadas"))
            sentences.append(w.t("\(races) en \(seasons)", "\(races) over \(seasons)", "\(races) en \(seasons)")
                + (achievements.isEmpty ? "." : w.t(" : ", ": ", ": ") + achievements.joined(separator: ", ") + "."))
        }
        if let s = standing {
            let team = s.constructor?.name
            let number = driver.number
            switch language {
            case .fr:
                sentences.append("En \(season), il court" + (team.map { " chez \($0)" } ?? "") + (number.map { ", numéro \($0)" } ?? "")
                    + " et occupe la \(s.position)e place du championnat.")
            case .en:
                sentences.append("In \(season), he races" + (team.map { " for \($0)" } ?? "") + (number.map { " with number \($0)" } ?? "")
                    + " and is P\(s.position) in the championship.")
            case .es:
                sentences.append("En \(season) corre" + (team.map { " con \($0)" } ?? "") + (number.map { " con el número \($0)" } ?? "")
                    + " y ocupa el \(s.position).º puesto del campeonato.")
            }
        }
        return sentences.joined(separator: " ")
    }

    public static func constructor(_ profile: ConstructorProfile, titles: Titles, standing: ConstructorStanding?, season: Int,
                                   language: AppLanguage) -> String {
        let w = Words(language: language)
        let name = profile.constructor.name
        let stats = profile.stats
        var sentences: [String] = []
        if stats.seasons > 0 {
            let seasons = w.count(stats.seasons, w.t("saison", "season", "temporada"), w.t("saisons", "seasons", "temporadas"))
            let wins = w.count(stats.wins, w.t("victoire", "win", "victoria"), w.t("victoires", "wins", "victorias"))
            sentences.append(w.t("\(name) compte \(seasons) en Formule 1", "\(name) has raced \(seasons) in Formula 1", "\(name) suma \(seasons) en la Fórmula 1")
                + (stats.wins > 0 ? w.t(" et \(wins) en Grand Prix.", " with \(wins) in Grands Prix.", " y \(wins) en Grandes Premios.") : "."))
        } else {
            sentences.append("\(name).")
        }
        if titles.count > 0 {
            let seasons = formatSeasons(titles.seasons)
            sentences.append(titles.count == 1
                ? w.t("Championne du monde des constructeurs (\(seasons)).", "Constructors' world champion (\(seasons)).",
                      "Campeona del mundo de constructores (\(seasons)).")
                : w.t("\(titles.count) titres constructeurs (\(seasons)).", "\(titles.count) constructors' titles (\(seasons)).",
                      "\(titles.count) títulos de constructores (\(seasons))."))
        }
        if let s = standing {
            sentences.append(w.t(
                "En \(season), l'écurie est \(s.position)e du championnat avec \(points(s.points)) points.",
                "In \(season), the team is P\(s.position) in the championship with \(points(s.points)) points.",
                "En \(season), la escudería es \(s.position).ª del campeonato con \(points(s.points)) puntos."))
        }
        return sentences.joined(separator: " ")
    }
}
