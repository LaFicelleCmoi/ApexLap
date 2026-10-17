import ApexLapDomain
import SwiftUI

struct RaceView: View {
    @Environment(\.repository) private var repository
    @Environment(\.encyclopedia) private var encyclopedia
    @Environment(\.strings) private var strings
    @State private var model: RaceViewModel
    @State private var tab: WeekendTab?

    enum WeekendTab: Hashable {
        case race, qualifying, sprint
    }

    private func label(_ tab: WeekendTab) -> String {
        switch tab {
        case .race: strings.tabRace
        case .qualifying: strings.tabQualifying
        case .sprint: strings.tabSprint
        }
    }

    private var tabs: [WeekendTab] { model.race?.isSprintWeekend == true ? [.race, .qualifying, .sprint] : [.race, .qualifying] }

    /// Until the user picks a tab, show the most advanced session that has results.
    private var currentTab: WeekendTab {
        if let tab { return tab }
        if !model.results.race.isEmpty { return .race }
        if !model.results.sprint.isEmpty { return .sprint }
        if !model.results.qualifying.isEmpty { return .qualifying }
        return .race
    }

    @ViewBuilder
    private func resultRows(_ rows: [RaceResult]) -> some View {
        if rows.isEmpty { emptyRow } else {
            ForEach(rows) { result in
                NavigationLink(value: AppRoute.driver(id: result.driver.id)) { ResultRow(result: result) }
            }
        }
    }

    @ViewBuilder
    private var emptyRow: some View {
        if model.isRefreshing {
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        } else {
            Text(strings.noSessionResults).font(.subheadline).foregroundStyle(ApexColor.telemetry)
                .listRowBackground(Color.clear)
        }
    }

    init(season: Int, round: Int) {
        _model = State(initialValue: RaceViewModel(season: season, round: round))
    }

    var body: some View {
        List {
            if let race = model.race {
                VStack(alignment: .leading, spacing: 4) {
                    Text(strings.roundOfSeason(race.round, race.season)).font(ApexFont.label).foregroundStyle(ApexColor.racingRed)
                    Text("\(Format.flag(race.circuit.country))  \(race.circuit.name)").font(ApexFont.headline)
                    Text("\(race.circuit.locality), \(race.circuit.country)").font(.subheadline).foregroundStyle(ApexColor.telemetry)
                }
                .listRowBackground(Color.clear)

                let facts = model.facts
                if !facts.isEmpty {
                    FactsCard(facts: facts)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                }

                Section {
                    if let context = model.context {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(strings.edition(context.edition)).font(.subheadline)
                            if let win = context.previousWinner {
                                Text(strings.previousWinner(win.driver.fullName, win.constructor.name, win.season))
                                    .font(.caption).foregroundStyle(ApexColor.telemetry)
                            }
                            if let most = context.mostWins {
                                Text(strings.mostWinsHere(most.driver.fullName, most.count))
                                    .font(.caption).foregroundStyle(ApexColor.telemetry)
                            }
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                    NavigationLink(value: AppRoute.circuit(id: race.circuit.id)) {
                        Text(strings.circuitPage).font(.subheadline.weight(.semibold)).foregroundStyle(ApexColor.racingRed)
                    }
                    .listRowBackground(ApexColor.asphalt)
                } header: {
                    SectionHeader(title: strings.context).listRowInsets(EdgeInsets())
                }

                Section {
                    ForEach(race.sessions, id: \.type) { SessionRow(session: $0) }
                } header: {
                    SectionHeader(title: strings.schedule).listRowInsets(EdgeInsets())
                }
            }

            if let error = model.error {
                OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }

            if model.hasStarted || !model.results.isEmpty {
                Picker(strings.sessionLabel, selection: Binding(get: { currentTab }, set: { tab = $0 })) {
                    ForEach(tabs, id: \.self) { Text(label($0)).tag($0) }
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)

                switch currentTab {
                case .race: resultRows(model.results.race)
                case .sprint: resultRows(model.results.sprint)
                case .qualifying:
                    if model.results.qualifying.isEmpty { emptyRow } else {
                        ForEach(model.results.qualifying) { result in
                            NavigationLink(value: AppRoute.driver(id: result.driver.id)) { QualifyingRow(result: result) }
                        }
                    }
                }
            } else {
                Text(strings.resultsSoon)
                    .font(.subheadline).foregroundStyle(ApexColor.telemetry)
                    .listRowBackground(Color.clear)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle(model.race?.name ?? strings.grandPrix)
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await model.refresh(using: repository) }
        .task {
            await model.load(using: repository)
            await model.loadContext(using: encyclopedia)
        }
    }
}

/// Winner, pole and fastest lap of the weekend.
private struct FactsCard: View {
    @Environment(\.strings) private var strings
    let facts: GrandPrixFacts

    var body: some View {
        VStack(spacing: 8) {
            if let winner = facts.winner {
                row(strings.winner, winner.driver.fullName, winner.constructor.name, ApexColor.gold, winner.driver.id)
            }
            if let pole = facts.pole {
                row(strings.polePosition, pole.fullName, facts.poleTime ?? "", ApexColor.purpleSector, pole.id)
            }
            if let lap = facts.fastestLap {
                row(strings.fastestLap, lap.driver.fullName, lap.fastestLapTime ?? "", ApexColor.purpleSector, lap.driver.id)
            }
        }
        .padding(14)
        .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 16))
    }

    private func row(_ label: String, _ name: String, _ detail: String, _ accent: Color, _ driverId: String) -> some View {
        NavigationLink(value: AppRoute.driver(id: driverId)) {
            HStack {
                Text(label.uppercased()).font(ApexFont.label).foregroundStyle(accent).frame(width: 120, alignment: .leading)
                Text(name).font(.subheadline.weight(.semibold))
                Spacer()
                Text(detail).font(.caption.monospacedDigit()).foregroundStyle(ApexColor.telemetry)
            }
        }
        .buttonStyle(.plain)
    }
}

private struct SessionRow: View {
    @Environment(\.strings) private var strings
    let session: Session

    var body: some View {
        HStack(spacing: 12) {
            Text(strings.sessionShort(session.type)).font(ApexFont.label)
                .frame(width: 48).padding(.vertical, 6)
                .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 8))
            Text(strings.session(session.type)).font(.subheadline.weight(.semibold))
            Spacer()
            VStack(alignment: .trailing) {
                Text(session.startsAt.map(Format.day) ?? Format.utcDay(session.day)).font(.caption)
                Text(session.startsAt.map(Format.time) ?? strings.timeTba).font(ApexFont.label)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}

private struct ResultRow: View {
    @Environment(\.strings) private var strings
    let result: RaceResult

    var body: some View {
        HStack(spacing: 8) {
            PositionText(position: result.position, label: result.isClassified ? nil : result.positionText)
            TeamStripe(color: TeamColor.of(result.constructor.id))
            VStack(alignment: .leading, spacing: 2) {
                Text(result.driver.fullName).font(.subheadline.weight(.semibold))
                Text(result.constructor.name).font(.caption).foregroundStyle(ApexColor.telemetry)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(result.time ?? result.status).font(.caption)
                HStack(spacing: 6) {
                    if result.hasFastestLap {
                        Text(strings.fastestLapShort).foregroundStyle(ApexColor.purpleSector)
                    }
                    if let gained = result.positionsGained, gained != 0 {
                        Text(gained > 0 ? "▲\(gained)" : "▼\(-gained)")
                            .foregroundStyle(gained > 0 ? ApexColor.greenSector : .red)
                    }
                    Text("+\(Format.points(result.points)) \(strings.pts)")
                }
                .font(ApexFont.label)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}

private struct QualifyingRow: View {
    @Environment(\.strings) private var strings
    let result: QualifyingResult

    var body: some View {
        HStack(spacing: 8) {
            PositionText(position: result.position)
            TeamStripe(color: TeamColor.of(result.constructor.id))
            VStack(alignment: .leading, spacing: 2) {
                Text(result.driver.fullName).font(.subheadline.weight(.semibold))
                Text([result.q1.map { "Q1 \($0)" }, result.q2.map { "Q2 \($0)" }].compactMap { $0 }.joined(separator: "  ·  "))
                    .font(.caption).foregroundStyle(ApexColor.telemetry)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(result.bestTime ?? "—").font(.subheadline.monospacedDigit())
                    .foregroundStyle(result.position == 1 ? ApexColor.purpleSector : ApexColor.floodlight)
                Text(result.eliminatedIn.map { strings.eliminatedIn($0) } ?? "Q3").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}
