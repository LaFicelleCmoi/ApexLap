import SwiftUI

/// "F1 night race" palette: asphalt blacks, floodlight whites, racing red.
enum ApexColor {
    static let asphalt = Color(hex: 0x0B0B10)
    static let carbon = Color(hex: 0x15151E)
    static let carbonRaised = Color(hex: 0x1F1F2B)
    static let kerb = Color(hex: 0x2C2C3A)
    static let racingRed = Color(hex: 0xE10600)
    static let floodlight = Color(hex: 0xF5F5F7)
    static let telemetry = Color(hex: 0x9A9AAD)
    static let gold = Color(hex: 0xFFD24C)
    static let silver = Color(hex: 0xC9CCD6)
    static let bronze = Color(hex: 0xCD7F4E)
    static let purpleSector = Color(hex: 0xB138DD)
    static let greenSector = Color(hex: 0x2ED573)
}

/// Categorical chart series colours, validated (lightness, chroma, colour-blind separation, 3:1 contrast) on the
/// dark card surface. Team colours are not used for series: several pairs are too close and team-mates would collide.
enum ChartPalette {
    static let colors: [Color] = [Color(hex: 0x3987E5), Color(hex: 0xD95926), Color(hex: 0x199E70), Color(hex: 0xC98500), Color(hex: 0xD55181)]
}

enum TeamColor {
    private static let colors: [String: Color] = [
        "mercedes": Color(hex: 0x27F4D2),
        "ferrari": Color(hex: 0xE8002D),
        "red_bull": Color(hex: 0x3671C6),
        "mclaren": Color(hex: 0xFF8000),
        "aston_martin": Color(hex: 0x229971),
        "alpine": Color(hex: 0x0093CC),
        "williams": Color(hex: 0x64C4FF),
        "rb": Color(hex: 0x6692FF),
        "audi": Color(hex: 0xBB0A30),
        "sauber": Color(hex: 0x52E252),
        "haas": Color(hex: 0xB6BABD),
        "cadillac": Color(hex: 0xC9A961),
    ]

    static func of(_ constructorId: String?) -> Color {
        constructorId.flatMap { colors[$0] } ?? ApexColor.telemetry
    }
}

/// Racing typography: heavy, italic, condensed-looking display styles over the system font.
enum ApexFont {
    static let display = Font.system(size: 34, weight: .black).italic()
    static let hero = Font.system(size: 56, weight: .black).italic()
    static let headline = Font.system(size: 24, weight: .heavy).italic()
    static let title = Font.system(size: 17, weight: .bold)
    static let label = Font.system(size: 11, weight: .bold).width(.expanded)
    static let mono = Font.system(size: 24, weight: .bold, design: .monospaced)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
