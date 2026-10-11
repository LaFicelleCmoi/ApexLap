import ApexLapData
import ApexLapDomain
import SwiftUI
import WidgetKit

struct NextSessionEntry: TimelineEntry {
    let date: Date
    let next: UpcomingSession?
    let hasCalendar: Bool
}

/// Reads the calendar the app shares through the App Group; the countdown text updates itself.
struct NextSessionProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextSessionEntry {
        NextSessionEntry(date: .now, next: nil, hasCalendar: true)
    }

    func getSnapshot(in context: Context, completion: @escaping (NextSessionEntry) -> Void) {
        completion(entry(at: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextSessionEntry>) -> Void) {
        let now = Date()
        let current = entry(at: now)
        // Re-evaluate when the session starts (the next one becomes current), or in an hour at the latest.
        let refresh = min(current.next?.session.startsAt ?? now.addingTimeInterval(3600), now.addingTimeInterval(3600))
        completion(Timeline(entries: [current], policy: .after(refresh)))
    }

    private func entry(at date: Date) -> NextSessionEntry {
        let calendar = SharedCalendar.load()
        return NextSessionEntry(date: date, next: CalendarLogic.nextSession(in: calendar, now: date), hasCalendar: !calendar.isEmpty)
    }
}

struct NextSessionWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: NextSessionEntry

    private let red = Color(red: 0.882, green: 0.024, blue: 0)
    private let grey = Color(red: 0.604, green: 0.604, blue: 0.678)

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Rectangle().fill(red).frame(width: 4, height: 12)
                Text("PROCHAINE SÉANCE").font(.system(size: 10, weight: .bold)).foregroundStyle(.white)
            }
            if let next = entry.next, let start = next.session.startsAt {
                Text("\(flag(next.race.circuit.country)) \(next.race.name)")
                    .font(.system(size: family == .systemSmall ? 13 : 16, weight: .bold)).foregroundStyle(.white).lineLimit(2)
                Text(next.session.type.label + (family == .systemSmall ? "" : " · " + start.formatted(.dateTime.weekday(.abbreviated).day().hour().minute())))
                    .font(.caption).foregroundStyle(grey).lineLimit(1)
                Spacer(minLength: 0)
                Text(start, style: .relative)
                    .font(.system(size: family == .systemSmall ? 18 : 22, weight: .heavy).monospacedDigit())
                    .foregroundStyle(red)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            } else {
                Text(entry.hasCalendar ? "Saison terminée" : "Ouvrez Apex Lap pour charger le calendrier")
                    .font(.caption).foregroundStyle(grey)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .containerBackground(Color(red: 0.043, green: 0.043, blue: 0.063), for: .widget)
    }

    private func flag(_ country: String) -> String {
        let codes = ["Australia": "AU", "China": "CN", "Japan": "JP", "Bahrain": "BH", "Saudi Arabia": "SA", "USA": "US",
                     "United States": "US", "Italy": "IT", "Monaco": "MC", "Spain": "ES", "Canada": "CA", "Austria": "AT",
                     "UK": "GB", "United Kingdom": "GB", "Belgium": "BE", "Hungary": "HU", "Netherlands": "NL", "Azerbaijan": "AZ",
                     "Singapore": "SG", "Mexico": "MX", "Brazil": "BR", "Qatar": "QA", "UAE": "AE", "Abu Dhabi": "AE"]
        guard let code = codes[country] else { return "🏁" }
        return code.unicodeScalars.compactMap { UnicodeScalar(0x1F1E6 + $0.value - 65) }.map(String.init).joined()
    }
}

struct NextSessionWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "NextSession", provider: NextSessionProvider()) { entry in
            NextSessionWidgetView(entry: entry)
        }
        .configurationDisplayName("Prochaine séance")
        .description("La prochaine séance de F1 et le temps restant.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct ApexLapWidgets: WidgetBundle {
    var body: some Widget {
        NextSessionWidget()
    }
}
