import ApexLapData
import ApexLapDomain
import SwiftUI
import WidgetKit

struct NextSessionEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot
    let language: AppLanguage
    let favorites: Favorites?
    /// Colour of the followed team, red otherwise.
    let accent: Color

    struct Favorites {
        let driver: DriverStanding?
        let constructor: ConstructorStanding?
        let leader: DriverStanding?
    }
}

/// Reads what the app shares through the App Group. One entry per moment the display changes (a session starts
/// or ends); the countdown itself is rendered live by the system.
struct NextSessionProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextSessionEntry {
        entry(at: .now, calendar: [])
    }

    func getSnapshot(in context: Context, completion: @escaping (NextSessionEntry) -> Void) {
        completion(entry(at: .now, calendar: SharedCalendar.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextSessionEntry>) -> Void) {
        let now = Date()
        let calendar = SharedCalendar.load()
        let dates = WidgetSchedule.timeline(calendar, from: now)
        let entries = dates.map { entry(at: $0, calendar: calendar) }
        // Past the last planned change, rebuild (at the latest in a day, when new sessions come into range).
        let refresh = (dates.last ?? now).addingTimeInterval(60)
        completion(Timeline(entries: entries, policy: .after(max(refresh, now.addingTimeInterval(15 * 60)))))
    }

    private func entry(at date: Date, calendar: [Race]) -> NextSessionEntry {
        let settings = SharedSettings.load()
        let language = settings.language.resolve()
        I18n.apply(language)
        var favorites: NextSessionEntry.Favorites?
        if settings.hasFavorites {
            let drivers = SharedSettings.driverStandings()
            favorites = .init(
                driver: drivers.first { $0.driver.id == settings.favoriteDriverId },
                constructor: SharedSettings.constructorStandings().first { $0.constructor.id == settings.favoriteConstructorId },
                leader: drivers.min { $0.position < $1.position }
            )
        }
        return NextSessionEntry(
            date: date,
            snapshot: WidgetSchedule.snapshot(calendar, at: date),
            language: language,
            favorites: favorites,
            accent: settings.favoriteConstructorId.map { TeamColor.of($0) } ?? ApexColor.racingRed
        )
    }
}

struct NextSessionWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: NextSessionEntry

    private var strings: Strings { Strings(entry.language) }
    private var snapshot: WidgetSnapshot { entry.snapshot }

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .environment(\.locale, entry.language.locale)
            .widgetURL(snapshot.race.flatMap { URL(string: "apexlap://race/\($0.season)/\($0.round)") })
            .containerBackground(ApexColor.asphalt, for: .widget)
    }

    @ViewBuilder
    private var content: some View {
        if let race = snapshot.race {
            switch family {
            case .accessoryRectangular: lockScreen(race)
            case .systemSmall: small(race)
            case .systemLarge:
                VStack(alignment: .leading, spacing: 10) {
                    medium(race)
                    schedule
                    if let favorites = entry.favorites { favoritesView(favorites) }
                    Spacer(minLength: 0)
                }
            default: medium(race)
            }
        } else {
            VStack(alignment: .leading, spacing: 6) {
                header(strings.nextSession)
                Text(snapshot.hasCalendar ? strings.seasonOver : strings.openAppToLoad)
                    .font(.caption).foregroundStyle(ApexColor.telemetry)
            }
        }
    }

    private func header(_ title: String) -> some View {
        HStack(spacing: 6) {
            Rectangle().fill(entry.accent).frame(width: 4, height: 12)
            Text(title).font(.system(size: 10, weight: .bold)).foregroundStyle(ApexColor.floodlight).lineLimit(1)
        }
    }

    /// Countdown to the next session, or "LIVE" while one is running.
    @ViewBuilder
    private func countdown(size: CGFloat, alignment: TextAlignment = .leading) -> some View {
        if snapshot.live != nil {
            Text("● " + strings.liveNow).font(.system(size: size * 0.75, weight: .heavy)).foregroundStyle(ApexColor.racingRed)
        } else if let start = snapshot.next?.startsAt {
            Text(start, style: .relative)
                .font(.system(size: size, weight: .heavy).monospacedDigit())
                .foregroundStyle(entry.accent)
                .multilineTextAlignment(alignment)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
        } else {
            Text("—").font(.system(size: size, weight: .heavy))
        }
    }

    private func small(_ race: Race) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            header("\(Format.flag(race.circuit.country)) \(strings.roundShort(race.round))")
            Text(race.name).font(.system(size: 13, weight: .bold)).foregroundStyle(ApexColor.floodlight).lineLimit(2)
            if let session = snapshot.live ?? snapshot.next {
                Text(strings.session(session.type)).font(.caption).foregroundStyle(ApexColor.telemetry).lineLimit(1)
            }
            Spacer(minLength: 0)
            countdown(size: 20)
        }
    }

    private func medium(_ race: Race) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            header("\(strings.round(race.round)) · \(Format.flag(race.circuit.country))")
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(race.name).font(.system(size: 16, weight: .bold)).foregroundStyle(ApexColor.floodlight).lineLimit(1)
                    let shown = snapshot.live ?? (snapshot.nextRace?.round == race.round ? snapshot.next : nil)
                    if let shown, let start = shown.startsAt {
                        Text("\(strings.session(shown.type)) · \(Format.day(start)) \(Format.time(start))")
                            .font(.caption).foregroundStyle(ApexColor.telemetry).lineLimit(1)
                    }
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 0) {
                    if snapshot.live == nil {
                        Text(strings.startsInLabel).font(.system(size: 8, weight: .bold)).foregroundStyle(ApexColor.telemetry)
                    }
                    countdown(size: 18, alignment: .trailing)
                }
                .frame(maxWidth: 120, alignment: .trailing)
            }
            HStack(spacing: 4) {
                ForEach(Array(snapshot.sessions.enumerated()), id: \.offset) { _, item in
                    chip(item)
                }
            }
        }
    }

    private func chip(_ item: WeekendSession) -> some View {
        let (background, text): (Color, Color) = switch item.phase {
        case .done: (ApexColor.carbon, ApexColor.telemetry.opacity(0.6))
        case .live: (ApexColor.racingRed, ApexColor.floodlight)
        case .next: (entry.accent, ApexColor.asphalt)
        case .later: (ApexColor.carbon, ApexColor.floodlight)
        }
        return Text((item.phase == .done ? "✓ " : "") + strings.sessionShort(item.session.type))
            .font(.system(size: 10, weight: .bold))
            .foregroundStyle(text)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
            .background(background, in: RoundedRectangle(cornerRadius: 6))
    }

    /// The full weekend in local time, the running or next session highlighted.
    private var schedule: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(strings.weekendSchedule).font(.system(size: 10, weight: .bold)).foregroundStyle(ApexColor.telemetry)
            ForEach(Array(snapshot.sessions.enumerated()), id: \.offset) { _, item in
                let color: Color = switch item.phase {
                case .done: ApexColor.telemetry.opacity(0.6)
                case .live: ApexColor.racingRed
                case .next: entry.accent
                case .later: ApexColor.floodlight
                }
                HStack {
                    Text(strings.session(item.session.type))
                        .fontWeight(item.phase == .next || item.phase == .live ? .bold : .regular)
                    Spacer()
                    Text(item.session.startsAt.map { "\(Format.day($0)) \(Format.time($0))" } ?? Format.utcDay(item.session.day))
                }
                .font(.caption)
                .foregroundStyle(color)
                .lineLimit(1)
            }
        }
    }

    private func favoritesView(_ favorites: NextSessionEntry.Favorites) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(strings.myFavorites.uppercased()).font(.system(size: 10, weight: .bold)).foregroundStyle(ApexColor.telemetry)
            if let d = favorites.driver {
                let gap = favorites.leader.flatMap { leader in
                    leader.driver.id == d.driver.id ? nil : " · " + strings.pointsBehind(Format.points(leader.points - d.points))
                } ?? ""
                favoriteLine(TeamColor.of(d.constructor?.id), d.driver.familyName, "P\(d.position) · \(Format.points(d.points)) \(strings.pts)\(gap)")
            }
            if let c = favorites.constructor {
                favoriteLine(TeamColor.of(c.constructor.id), c.constructor.name, "P\(c.position) · \(Format.points(c.points)) \(strings.pts)")
            }
        }
    }

    private func favoriteLine(_ color: Color, _ name: String, _ detail: String) -> some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 1).fill(color).frame(width: 3, height: 14)
            Text(name).font(.caption.weight(.bold)).foregroundStyle(ApexColor.floodlight)
            Text(detail).font(.caption).foregroundStyle(ApexColor.telemetry)
        }
        .lineLimit(1)
    }

    /// Lock screen: one line of context and the countdown.
    private func lockScreen(_ race: Race) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text("\(Format.flag(race.circuit.country)) \(race.name)").font(.headline).lineLimit(1)
            if let session = snapshot.live ?? snapshot.next {
                Text(strings.session(session.type)).font(.caption).lineLimit(1)
            }
            if snapshot.live != nil {
                Text("● " + strings.liveNow).font(.caption.bold())
            } else if let start = snapshot.next?.startsAt {
                Text(start, style: .relative).font(.caption.monospacedDigit())
            }
        }
    }
}

struct NextSessionWidget: Widget {
    var body: some WidgetConfiguration {
        let strings = Strings(SharedSettings.load().language.resolve())
        return StaticConfiguration(kind: "NextSession", provider: NextSessionProvider()) { entry in
            NextSessionWidgetView(entry: entry)
        }
        .configurationDisplayName(strings.nextSessionTitle)
        .description(strings.widgetDescription)
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular])
    }
}

@main
struct ApexLapWidgets: WidgetBundle {
    var body: some Widget {
        NextSessionWidget()
    }
}
