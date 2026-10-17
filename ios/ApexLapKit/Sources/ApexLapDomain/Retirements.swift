import Foundation

public enum RetirementKind: Sendable {
    case retired
    /// Retired but still classified (90 % of the race distance covered), marked "*".
    case classified
    /// Not classified, marked "NC".
    case notClassified
}

public struct Retirement: Hashable, Sendable {
    public let grandPrix: String
    public let kind: RetirementKind

    public init(_ grandPrix: String, _ kind: RetirementKind = .retired) {
        self.grandPrix = grandPrix
        self.kind = kind
    }

    public func label(_ language: AppLanguage) -> String {
        let name = SeasonRetirementsRecord.grandPrixName(grandPrix, language)
        switch kind {
        case .retired: return name
        case .classified: return name + "*"
        case .notClassified: return name + " (NC)"
        }
    }
}

public struct DriverRetirements: Hashable, Sendable, Identifiable {
    public let driver: String
    public let retirements: [Retirement]
    public var count: Int { retirements.count }
    public var id: String { driver }
}

public struct NonStarts: Hashable, Sendable {
    public let driver: String
    public let grandsPrix: [String]
}

public struct SeasonRetirements: Hashable, Sendable {
    public let season: Int
    /// Most retirements first.
    public let drivers: [DriverRetirements]
    public let noRetirement: [String]
    public let nonStarters: [NonStarts]
    public var total: Int { drivers.reduce(0) { $0 + $1.count } }
}

/// Hand-kept retirement record, by driver (same data as Android).
public enum SeasonRetirementsRecord {
    public static func of(_ season: Int) -> SeasonRetirements? { season == 2026 ? season2026 : nil }

    /// "Australie (NC), Chine, Azerbaïdjan*" → retirements.
    public static func parse(_ spec: String) -> [Retirement] {
        spec.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.map { item in
            if item.hasSuffix("(NC)") { return Retirement(String(item.dropLast(4)).trimmingCharacters(in: .whitespaces), .notClassified) }
            if item.hasSuffix("*") { return Retirement(String(item.dropLast()).trimmingCharacters(in: .whitespaces), .classified) }
            return Retirement(item)
        }
    }

    /// The record names Grands Prix by their French country (or city) name.
    public static func grandPrixName(_ french: String, _ language: AppLanguage) -> String { names[french]?[language] ?? french }

    private static let names: [String: L] = [
        "Australie": L("Australie", "Australia", "Australia"),
        "Chine": L("Chine", "China", "China"),
        "Japon": L("Japon", "Japan", "Japón"),
        "Bahreïn": L("Bahreïn", "Bahrain", "Baréin"),
        "Miami": L("Miami", "Miami", "Miami"),
        "Canada": L("Canada", "Canada", "Canadá"),
        "Monaco": L("Monaco", "Monaco", "Mónaco"),
        "Barcelone": L("Barcelone", "Barcelona", "Barcelona"),
        "Autriche": L("Autriche", "Austria", "Austria"),
        "Grande-Bretagne": L("Grande-Bretagne", "Great Britain", "Gran Bretaña"),
        "Belgique": L("Belgique", "Belgium", "Bélgica"),
        "Hongrie": L("Hongrie", "Hungary", "Hungría"),
        "Pays-Bas": L("Pays-Bas", "Netherlands", "Países Bajos"),
        "Italie": L("Italie", "Italy", "Italia"),
        "Espagne": L("Espagne", "Spain", "España"),
        "Azerbaïdjan": L("Azerbaïdjan", "Azerbaijan", "Azerbaiyán"),
        "Singapour": L("Singapour", "Singapore", "Singapur"),
    ]

    private static func d(_ driver: String, _ spec: String) -> DriverRetirements { DriverRetirements(driver: driver, retirements: parse(spec)) }

    private static let season2026 = SeasonRetirements(
        season: 2026,
        drivers: [
            d("Stroll", "Australie (NC), Chine, Japon, Monaco, Barcelone, Autriche, Belgique, Pays-Bas, Italie, Espagne, Azerbaïdjan, Singapour"),
            d("Bottas", "Australie, Monaco, Barcelone, Autriche, Hongrie, Pays-Bas, Azerbaïdjan*, Bahreïn"),
            d("Albon", "Canada, Barcelone (NC), Grande-Bretagne, Pays-Bas*, Azerbaïdjan, Bahreïn, Singapour*"),
            d("Alonso", "Australie, Chine, Canada, Barcelone, Italie, Azerbaïdjan"),
            d("Pérez", "Canada, Autriche, Belgique, Hongrie, Espagne"),
            d("Verstappen", "Chine, Monaco, Grande-Bretagne*, Pays-Bas"),
            d("Bearman", "Japon, Monaco, Barcelone*, Pays-Bas"),
            d("Hülkenberg", "Miami, Barcelone, Grande-Bretagne"),
            d("Norris", "Canada, Monaco, Azerbaïdjan"),
            d("Russell", "Canada, Belgique, Bahreïn*"),
            d("Leclerc", "Monaco, Barcelone*, Italie"),
            d("Sainz", "Monaco*, Autriche, Espagne"),
            d("Hadjar", "Australie, Miami"),
            d("Gasly", "Miami, Azerbaïdjan"),
            d("Colapinto", "Azerbaïdjan, Singapour"),
            d("Antonelli", "Barcelone*"),
            d("Piastri", "Hongrie"),
            d("Lawson", "Miami"),
            d("Ocon", "Pays-Bas"),
            d("Hamilton", "Espagne"),
            d("Bortoleto", "Singapour"),
        ],
        noRetirement: ["Lindblad", "Tsunoda"],
        nonStarters: [
            NonStarts(driver: "Piastri", grandsPrix: ["Australie", "Chine"]),
            NonStarts(driver: "Hülkenberg", grandsPrix: ["Australie"]),
            NonStarts(driver: "Norris", grandsPrix: ["Chine"]),
            NonStarts(driver: "Bortoleto", grandsPrix: ["Chine"]),
            NonStarts(driver: "Albon", grandsPrix: ["Chine"]),
            NonStarts(driver: "Lindblad", grandsPrix: ["Canada"]),
        ]
    )
}
