import ApexLapDomain
import SwiftUI

struct RaceView: View {
    @Environment(\.repository) private var repository
    @State private var model: RaceViewModel

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

            if let error = model.results.error {
                OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }

            let results = model.results.value ?? []
            if !results.isEmpty {
                Section {
                    ForEach(results) { result in
                        NavigationLink(value: AppRoute.driver(id: result.driver.id)) { ResultRow(result: result) }
                    }
                } header: {
                    SectionHeader(title: "Résultats de la course").listRowInsets(EdgeInsets())
                }
            } else if model.results.isRefreshing {
                ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
            } else if let race = model.race, !race.isFinished(at: .now) {
                Text("Les résultats seront disponibles après la course.")
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
            PositionText(position: result.position,
                         label: result.positionText.allSatisfy(\.isNumber) ? nil : result.positionText)
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
