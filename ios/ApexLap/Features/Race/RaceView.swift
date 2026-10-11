import ApexLapDomain
import SwiftUI

struct RaceView: View {
    @Environment(\.repository) private var repository
    @State private var model: RaceViewModel
    @State private var tab: WeekendTab?

    enum WeekendTab: String, Hashable {
        case race = "Course", qualifying = "Qualifs", sprint = "Sprint"
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
            Text("Pas encore de résultats pour cette séance.").font(.subheadline).foregroundStyle(ApexColor.telemetry)
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
                    Text("MANCHE \(race.round) · \(String(race.season))").font(ApexFont.label).foregroundStyle(ApexColor.racingRed)
                    Text("\(Format.flag(race.circuit.country))  \(race.circuit.name)").font(ApexFont.headline)
                    Text("\(race.circuit.locality), \(race.circuit.country)").font(.subheadline).foregroundStyle(ApexColor.telemetry)
                }
                .listRowBackground(Color.clear)

                Section {
                    ForEach(race.sessions, id: \.type) { SessionRow(session: $0) }
                } header: {
                    SectionHeader(title: "Programme (heure locale)").listRowInsets(EdgeInsets())
                }
            }

            if let error = model.error {
                OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }

            if model.hasStarted || !model.results.isEmpty {
                Picker("Séance", selection: Binding(get: { currentTab }, set: { tab = $0 })) {
                    ForEach(tabs, id: \.self) { Text($0.rawValue).tag($0) }
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
                Text("Les résultats apparaîtront ici dès le début du week-end.")
                    .font(.subheadline).foregroundStyle(ApexColor.telemetry)
                    .listRowBackground(Color.clear)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle(model.race?.name ?? "Grand Prix")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await model.refresh(using: repository) }
        .task { await model.load(using: repository) }
    }
}

private struct SessionRow: View {
    let session: Session

    var body: some View {
        HStack(spacing: 12) {
            Text(session.type.shortLabel).font(ApexFont.label)
                .frame(width: 48).padding(.vertical, 6)
                .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 8))
            Text(session.type.label).font(.subheadline.weight(.semibold))
            Spacer()
            VStack(alignment: .trailing) {
                Text(session.startsAt.map(Format.day) ?? Format.utcDay(session.day)).font(.caption)
                Text(session.startsAt.map(Format.time) ?? "Horaire à venir").font(ApexFont.label)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}

private struct ResultRow: View {
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
                        Text("⏱ MT").foregroundStyle(ApexColor.purpleSector)
                    }
                    if let gained = result.positionsGained, gained != 0 {
                        Text(gained > 0 ? "▲\(gained)" : "▼\(-gained)")
                            .foregroundStyle(gained > 0 ? ApexColor.greenSector : .red)
                    }
                    Text("+\(Format.points(result.points)) pts")
                }
                .font(ApexFont.label)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}

private struct QualifyingRow: View {
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
                Text(result.eliminatedIn.map { "ÉLIMINÉ EN \($0)" } ?? "Q3").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }
}
