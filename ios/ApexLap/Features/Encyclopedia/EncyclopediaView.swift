import ApexLapData
import ApexLapDomain
import SwiftUI

/// Seasons since 1950, all-time drivers, teams and circuits, and a glossary (same content as Android).
struct EncyclopediaView: View {
    enum Tab: CaseIterable, Hashable {
        case seasons, drivers, teams, circuits, glossary
    }

    @Environment(\.repository) private var repository
    @Environment(\.encyclopedia) private var encyclopedia
    @Environment(\.strings) private var strings
    @State private var tab: Tab = .seasons
    @State private var query = ""
    @State private var championsOnly = false
    @State private var registry = ChampionsRegistry([])
    @State private var leader: DriverStanding?
    @State private var drivers: [Driver] = []
    @State private var constructors: [Constructor] = []
    @State private var circuits: [Circuit] = []
    @State private var loading: Set<Tab> = []
    @State private var error: String?
    private let season = CalendarLogic.currentSeason(now: .now)

    var body: some View {
        List {
            Picker(strings.encyclopedia, selection: $tab) {
                ForEach(Tab.allCases, id: \.self) { Text(label($0)).tag($0) }
            }
            .pickerStyle(.segmented)
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            if tab == .drivers || tab == .teams {
                Toggle(strings.worldChampionsFilter, isOn: $championsOnly)
                    .listRowBackground(Color.clear)
            }
            if let error, tab != .seasons, tab != .glossary {
                OfflineBanner(message: error) { Task { await load(tab, force: true) } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }
            if loading.contains(tab) && isEmpty(tab) {
                ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
            }

            switch tab {
            case .seasons: seasons
            case .drivers: driverRows
            case .teams: teamRows
            case .circuits: circuitRows
            case .glossary: glossary
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .searchable(text: $query, prompt: strings.searchPrompt)
        .navigationTitle(strings.encyclopedia)
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await load(tab, force: true) }
        .task {
            registry = await encyclopedia.champions()
            leader = await repository.cachedDriverStandings(season: season).first
        }
        .task(id: tab) { await load(tab, force: false) }
    }

    private func label(_ tab: Tab) -> String {
        switch tab {
        case .seasons: strings.tabSeasons
        case .drivers: strings.drivers
        case .teams: strings.tabTeams
        case .circuits: strings.tabCircuits
        case .glossary: strings.tabGlossary
        }
    }

    private func isEmpty(_ tab: Tab) -> Bool {
        switch tab {
        case .drivers: drivers.isEmpty
        case .teams: constructors.isEmpty
        case .circuits: circuits.isEmpty
        default: false
        }
    }

    /// Lists are only downloaded when their tab is first shown (cached on disk afterwards).
    private func load(_ tab: Tab, force: Bool) async {
        guard tab == .drivers || tab == .teams || tab == .circuits else { return }
        error = nil
        if isEmpty(tab) {
            switch tab {
            case .drivers: drivers = await encyclopedia.cachedDrivers()
            case .teams: constructors = await encyclopedia.cachedConstructors()
            default: circuits = await encyclopedia.cachedCircuits()
            }
        }
        guard force || isEmpty(tab) else { return }
        loading.insert(tab)
        defer { loading.remove(tab) }
        do {
            switch tab {
            case .drivers: drivers = try await encyclopedia.drivers(force: force)
            case .teams: constructors = try await encyclopedia.constructors(force: force)
            default: circuits = try await encyclopedia.circuits(force: force)
            }
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }

    // MARK: Seasons

    @ViewBuilder
    private var seasons: some View {
        if registry.season(season) == nil {
            NavigationLink(value: AppRoute.season(season)) {
                SeasonRow(season: season,
                          driverLine: leader.map { strings.leaderLine($0.driver.fullName, Format.points($0.points)) } ?? strings.seasonInProgress,
                          teamLine: leader?.constructor?.name, teamId: leader?.constructor?.id, ongoing: true)
            }
            .listRowBackground(ApexColor.asphalt)
        }
        ForEach(registry.champions) { champion in
            NavigationLink(value: AppRoute.season(champion.season)) {
                SeasonRow(season: champion.season,
                          driverLine: "\(Format.flag(champion.driver.nationality))  \(champion.driver.name)" + (champion.driver.team.map { " · \($0)" } ?? ""),
                          teamLine: champion.constructor.map { strings.constructorsChampion($0.name) },
                          teamId: champion.driver.teamId, ongoing: false)
            }
            .listRowBackground(ApexColor.asphalt)
        }
    }

    // MARK: Lists

    @ViewBuilder
    private var driverRows: some View {
        let rows = drivers.filter { driver in
            (!championsOnly || registry.driverTitles(driver.id).count > 0)
                && (driver.fullName.matchesSearch(query) || driver.nationality.matchesSearch(query))
        }
        let sorted = championsOnly ? rows.sorted { registry.driverTitles($0.id).count > registry.driverTitles($1.id).count } : rows
        if !loading.contains(.drivers) && sorted.isEmpty && !drivers.isEmpty {
            Text(strings.noDriverFound).foregroundStyle(ApexColor.telemetry).listRowBackground(Color.clear)
        } else if !sorted.isEmpty {
            Text(strings.results(sorted.count)).font(.caption).foregroundStyle(ApexColor.telemetry).listRowBackground(Color.clear)
        }
        ForEach(sorted) { driver in
            let titles = registry.driverTitles(driver.id).count
            NavigationLink(value: AppRoute.driver(id: driver.id)) {
                EntryRow(title: "\(Format.flag(driver.nationality))  \(driver.fullName)",
                         subtitle: [driver.nationality, driver.dateOfBirth.map { strings.bornIn(Calendar(identifier: .gregorian).component(.year, from: $0)) }]
                            .compactMap { $0 }.joined(separator: " · "),
                         badge: titles > 0 ? "🏆 ×\(titles)" : nil, stripe: nil)
            }
            .listRowBackground(ApexColor.asphalt)
        }
    }

    @ViewBuilder
    private var teamRows: some View {
        let rows = constructors.filter { team in
            (!championsOnly || registry.constructorTitles(team.id).count > 0)
                && (team.name.matchesSearch(query) || team.nationality.matchesSearch(query))
        }
        let sorted = championsOnly ? rows.sorted { registry.constructorTitles($0.id).count > registry.constructorTitles($1.id).count } : rows
        if !loading.contains(.teams) && sorted.isEmpty && !constructors.isEmpty {
            Text(strings.noTeamFound).foregroundStyle(ApexColor.telemetry).listRowBackground(Color.clear)
        }
        ForEach(sorted) { team in
            let titles = registry.constructorTitles(team.id).count
            NavigationLink(value: AppRoute.constructor(id: team.id)) {
                EntryRow(title: team.name, subtitle: "\(Format.flag(team.nationality)) \(team.nationality)",
                         badge: titles > 0 ? "🏆 ×\(titles)" : nil, stripe: TeamColor.of(team.id))
            }
            .listRowBackground(ApexColor.asphalt)
        }
    }

    @ViewBuilder
    private var circuitRows: some View {
        let rows = circuits.filter { $0.name.matchesSearch(query) || $0.locality.matchesSearch(query) || $0.country.matchesSearch(query) }
        if !loading.contains(.circuits) && rows.isEmpty && !circuits.isEmpty {
            Text(strings.noCircuitFound).foregroundStyle(ApexColor.telemetry).listRowBackground(Color.clear)
        }
        ForEach(rows, id: \.id) { circuit in
            NavigationLink(value: AppRoute.circuit(id: circuit.id)) {
                EntryRow(title: "\(Format.flag(circuit.country))  \(circuit.name)", subtitle: "\(circuit.locality), \(circuit.country)",
                         badge: nil, stripe: nil)
            }
            .listRowBackground(ApexColor.asphalt)
        }
    }

    // MARK: Glossary

    @ViewBuilder
    private var glossary: some View {
        let terms = Glossary.terms.filter { $0.matches(query, language: strings.language) }
        if terms.isEmpty {
            Text(strings.noTermFound).foregroundStyle(ApexColor.telemetry).listRowBackground(Color.clear)
        }
        ForEach(GlossaryCategory.allCases, id: \.self) { category in
            let inCategory = terms.filter { $0.category == category }
            if !inCategory.isEmpty {
                Section {
                    ForEach(inCategory) { term in
                        DisclosureGroup {
                            Text(term.definition[strings.language]).font(.subheadline).foregroundStyle(ApexColor.telemetry)
                        } label: {
                            Text(term.term[strings.language]).font(.subheadline.weight(.semibold))
                        }
                        .listRowBackground(ApexColor.carbon)
                    }
                } header: {
                    SectionHeader(title: strings.glossaryCategory(category)).listRowInsets(EdgeInsets())
                }
            }
        }
    }
}

private struct SeasonRow: View {
    @Environment(\.strings) private var strings
    let season: Int
    let driverLine: String
    let teamLine: String?
    let teamId: String?
    let ongoing: Bool

    var body: some View {
        HStack(spacing: 10) {
            Text(String(season)).font(ApexFont.headline)
                .foregroundStyle(ongoing ? ApexColor.racingRed : ApexColor.floodlight)
                .frame(width: 72, alignment: .leading)
            TeamStripe(color: TeamColor.of(teamId), height: 34)
            VStack(alignment: .leading, spacing: 2) {
                Text(driverLine).font(.subheadline.weight(.semibold)).lineLimit(1)
                if let teamLine { Text(teamLine).font(.caption).foregroundStyle(ApexColor.telemetry).lineLimit(1) }
            }
            Spacer()
            if ongoing { Text(strings.inProgress).font(ApexFont.label).foregroundStyle(ApexColor.telemetry) }
        }
    }
}

private struct EntryRow: View {
    let title: String
    let subtitle: String
    let badge: String?
    let stripe: Color?

    var body: some View {
        HStack(spacing: 10) {
            if let stripe { TeamStripe(color: stripe) }
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.semibold)).lineLimit(1)
                Text(subtitle).font(.caption).foregroundStyle(ApexColor.telemetry).lineLimit(1)
            }
            Spacer()
            if let badge { Text(badge).font(ApexFont.label).foregroundStyle(ApexColor.gold) }
        }
    }
}
