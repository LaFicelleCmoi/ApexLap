import ApexLapData
import ApexLapDomain
import SwiftUI

/// One season of the archive: champion, highlights, every Grand Prix winner and the final standings.
struct SeasonView: View {
    private static let firstSeason = 1950
    private static let standingsShown = 10

    @Environment(\.repository) private var repository
    @Environment(\.encyclopedia) private var encyclopedia
    @Environment(\.strings) private var strings
    @State private var season: Int
    @State private var champion: Champion?
    @State private var winners: [SeasonWinner] = []
    @State private var drivers: [DriverStanding] = []
    @State private var constructors: [ConstructorStanding] = []
    @State private var isLoading = true
    @State private var error: String?
    private let currentSeason = CalendarLogic.currentSeason(now: .now)

    init(season: Int) {
        _season = State(initialValue: season)
    }

    var body: some View {
        List {
            // Timeline: step to the previous / next season without leaving the page.
            HStack {
                Button { season -= 1 } label: { Label(String(season - 1), systemImage: "chevron.left") }
                    .disabled(season <= Self.firstSeason)
                Spacer()
                Button { season += 1 } label: { Label(String(season + 1), systemImage: "chevron.right").labelStyle(TrailingIcon()) }
                    .disabled(season >= currentSeason)
            }
            .buttonStyle(.borderless)
            .listRowBackground(Color.clear)

            if isLoading { ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear) }
            if let error {
                OfflineBanner(message: error) { Task { await load() } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }
            if let champion {
                ChampionBanner(champion: champion)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    .listRowBackground(Color.clear)
            }

            if !winners.isEmpty {
                let highlights = SeasonHighlights(winners: winners, standings: drivers)
                Section {
                    HighlightsView(highlights: highlights, isFinished: champion != nil)
                        .listRowBackground(ApexColor.asphalt)
                } header: {
                    SectionHeader(title: strings.highlights).listRowInsets(EdgeInsets())
                }

                Section {
                    ForEach(winners) { item in
                        NavigationLink(value: AppRoute.race(season: item.race.season, round: item.race.round)) {
                            HStack(spacing: 10) {
                                Text(String(format: "%02d", item.race.round)).font(ApexFont.title).foregroundStyle(ApexColor.telemetry)
                                Text(Format.flag(item.race.circuit.country)).font(.title3)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.race.name).font(.subheadline.weight(.semibold)).lineLimit(1)
                                    if let day = item.race.raceSession?.day {
                                        Text(Format.shortUTCDay(day)).font(.caption2).foregroundStyle(ApexColor.telemetry)
                                    }
                                }
                                Spacer()
                                TeamStripe(color: TeamColor.of(item.winner.constructor.id), height: 24)
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(item.winner.driver.fullName).font(.caption)
                                    Text(item.winner.constructor.name).font(.caption2).foregroundStyle(ApexColor.telemetry)
                                }
                            }
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                } header: {
                    SectionHeader(title: strings.grandsPrixCount(winners.count)).listRowInsets(EdgeInsets())
                }
            }

            if !drivers.isEmpty {
                Section {
                    ForEach(drivers.prefix(Self.standingsShown)) { s in
                        NavigationLink(value: AppRoute.driver(id: s.driver.id)) {
                            StandingLine(position: s.position, teamId: s.constructor?.id, title: s.driver.fullName,
                                         subtitle: s.constructor?.name ?? "", points: s.points, wins: s.wins)
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                } header: {
                    SectionHeader(title: strings.driversChampionship).listRowInsets(EdgeInsets())
                }
            }
            if !constructors.isEmpty {
                Section {
                    ForEach(constructors.prefix(Self.standingsShown)) { s in
                        NavigationLink(value: AppRoute.constructor(id: s.constructor.id)) {
                            StandingLine(position: s.position, teamId: s.constructor.id, title: s.constructor.name,
                                         subtitle: s.constructor.nationality, points: s.points, wins: s.wins)
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                } header: {
                    SectionHeader(title: strings.constructorsChampionship).listRowInsets(EdgeInsets())
                }
            } else if season < 1958 && !isLoading {
                Text(strings.noConstructorsBefore1958).font(.caption).foregroundStyle(ApexColor.telemetry)
                    .listRowBackground(Color.clear)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle(strings.seasonHeader(season))
        .navigationBarTitleDisplayMode(.inline)
        .task(id: season) { await load() }
    }

    private func load() async {
        isLoading = true
        error = nil
        champion = await encyclopedia.champions().season(season)
        winners = []
        drivers = await repository.cachedDriverStandings(season: season)
        constructors = await repository.cachedConstructorStandings(season: season)
        defer { isLoading = false }
        do {
            winners = try await encyclopedia.seasonWinners(season)
            let fresh = try await repository.refreshStandings(season: season)
            drivers = fresh.0
            constructors = fresh.1
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }
}

private struct TrailingIcon: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 4) { configuration.title; configuration.icon }
    }
}

private struct ChampionBanner: View {
    @Environment(\.strings) private var strings
    let champion: Champion

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(strings.worldChampion).font(ApexFont.label).foregroundStyle(ApexColor.gold)
            NavigationLink(value: AppRoute.driver(id: champion.driver.id)) {
                Text(champion.driver.name).font(ApexFont.headline)
            }
            .buttonStyle(.plain)
            Text(Format.flag(champion.driver.nationality) + " "
                 + strings.championLine(champion.driver.team ?? "", Format.points(champion.driver.points), champion.driver.wins, champion.races))
                .font(.caption).foregroundStyle(ApexColor.telemetry)
            if let team = champion.constructor {
                NavigationLink(value: AppRoute.constructor(id: team.id)) {
                    HStack(spacing: 6) {
                        TeamStripe(color: TeamColor.of(team.id), height: 16)
                        Text(strings.constructorsChampion("\(team.name) · \(Format.points(team.points)) \(strings.pts)"))
                            .font(.subheadline.weight(.semibold))
                    }
                }
                .buttonStyle(.plain)
                .padding(.top, 6)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [ApexColor.gold.opacity(0.35), ApexColor.carbon], startPoint: .leading, endPoint: .trailing),
                    in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct HighlightsView: View {
    @Environment(\.strings) private var strings
    let highlights: SeasonHighlights
    let isFinished: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let top = highlights.topWinner {
                line("🏁", strings.mostWinsHighlight(top.driver.fullName, top.count))
            }
            if let team = highlights.topTeam {
                line("🏎", strings.teamWinsHighlight(team.constructor.name, team.count))
            }
            line("👥", strings.differentWinners(highlights.differentWinners))
            if highlights.winsFromPole > 0 {
                line("🟣", strings.winsFromPole(highlights.winsFromPole))
            }
            if let deepest = highlights.deepestWin {
                line("🚀", strings.deepestWin(deepest.winner.driver.fullName, deepest.winner.grid, deepest.race.name))
            }
            if let margin = highlights.titleMargin, let runnerUp = highlights.runnerUp {
                line("⚖", strings.titleMargin(Format.points(margin), runnerUp.driver.fullName, finished: isFinished))
            }
        }
    }

    private func line(_ icon: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text(icon)
            Text(text).font(.subheadline)
        }
    }
}

private struct StandingLine: View {
    @Environment(\.strings) private var strings
    let position: Int
    let teamId: String?
    let title: String
    let subtitle: String
    let points: Double
    let wins: Int

    var body: some View {
        HStack(spacing: 8) {
            PositionText(position: position)
            TeamStripe(color: TeamColor.of(teamId))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(subtitle).font(.caption).foregroundStyle(ApexColor.telemetry)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(Format.points(points)).font(ApexFont.title)
                if wins > 0 { Text(strings.winsShort(wins)).font(ApexFont.label).foregroundStyle(ApexColor.telemetry) }
            }
        }
    }
}
