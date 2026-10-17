import ApexLapData
import ApexLapDomain
import SwiftUI

/// A circuit's history: key corners, records, the most successful drivers and every winner.
struct CircuitView: View {
    @Environment(\.encyclopedia) private var encyclopedia
    @Environment(\.strings) private var strings
    @State private var history: CircuitHistory?
    @State private var records: CircuitRecords?
    @State private var isLoading = true
    @State private var error: String?
    let circuitId: String

    var body: some View {
        Group {
            if let history {
                content(history)
            } else if isLoading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                EmptyStateView(message: error ?? strings.circuitNotFound) { Task { await load() } }
            }
        }
        .background(ApexColor.asphalt)
        .navigationTitle(history?.circuit.name ?? strings.circuit)
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func content(_ history: CircuitHistory) -> some View {
        let language = strings.language
        return List {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(Format.flag(history.circuit.country))  \(history.circuit.name)").font(ApexFont.headline)
                Text("\(history.circuit.locality), \(history.circuit.country)").font(.subheadline).foregroundStyle(ApexColor.telemetry)
                HStack(spacing: 12) {
                    StatTile(label: strings.statGrandsPrix, value: "\(history.grandsPrix)")
                    StatTile(label: strings.statFirst, value: history.firstSeason.map(String.init) ?? "—")
                    StatTile(label: strings.statLast, value: history.lastSeason.map(String.init) ?? "—")
                }
                .padding(.top, 8)
            }
            .listRowBackground(Color.clear)

            if let guide = CircuitGuides.of(history.circuit.id) {
                Text(guide.summary[language]).font(.subheadline).listRowBackground(Color.clear)
                Section {
                    ForEach(guide.corners) { corner in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(corner.name[language]).font(.subheadline.weight(.semibold))
                            Text(corner.description[language]).font(.caption).foregroundStyle(ApexColor.telemetry)
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                } header: {
                    SectionHeader(title: strings.keyCorners).listRowInsets(EdgeInsets())
                }
            }

            if let records, records.raceLap != nil || !records.poleSitters.isEmpty {
                Section {
                    if let lap = records.raceLap {
                        recordRow(strings.raceLapRecord, lap.time, "\(lap.driver.fullName) · \(lap.constructor.name) · \(lap.season)", lap.driver.id)
                    }
                    if let top = records.poleSitters.first {
                        recordRow(strings.mostPoleStarts, "\(top.count)",
                                  records.poleSitters.map { "\($0.driver.familyName) \($0.count)" }.joined(separator: "  ·  "), top.driver.id)
                    }
                    Text(strings.recordsNote).font(.caption2).foregroundStyle(ApexColor.telemetry).listRowBackground(ApexColor.asphalt)
                } header: {
                    SectionHeader(title: strings.records).listRowInsets(EdgeInsets())
                }
            }

            if history.wins.isEmpty {
                Text(strings.noGrandPrixHereYet).foregroundStyle(ApexColor.telemetry).listRowBackground(Color.clear)
            } else {
                Section {
                    ForEach(history.topDrivers()) { top in
                        NavigationLink(value: AppRoute.driver(id: top.driver.id)) {
                            HStack {
                                Text("\(Format.flag(top.driver.nationality))  \(top.driver.fullName)").font(.subheadline.weight(.semibold))
                                Spacer()
                                Text(strings.wins(top.count)).font(ApexFont.label).foregroundStyle(ApexColor.gold)
                            }
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                    Text(history.topConstructors().map { "\($0.constructor.name) \($0.count)" }.joined(separator: "  ·  "))
                        .font(.caption).foregroundStyle(ApexColor.telemetry).listRowBackground(ApexColor.asphalt)
                } header: {
                    SectionHeader(title: strings.kingsOfCircuit).listRowInsets(EdgeInsets())
                }

                Section {
                    ForEach(history.wins) { win in
                        NavigationLink(value: AppRoute.race(season: win.season, round: win.round)) {
                            HStack(spacing: 10) {
                                Text(String(win.season)).font(ApexFont.title).frame(width: 52, alignment: .leading)
                                TeamStripe(color: TeamColor.of(win.constructor.id), height: 24)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(win.driver.fullName).font(.subheadline.weight(.semibold))
                                    Text(win.constructor.name).font(.caption).foregroundStyle(ApexColor.telemetry)
                                }
                            }
                        }
                        .listRowBackground(ApexColor.asphalt)
                    }
                } header: {
                    SectionHeader(title: strings.roll).listRowInsets(EdgeInsets())
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    private func recordRow(_ label: String, _ value: String, _ detail: String, _ driverId: String) -> some View {
        NavigationLink(value: AppRoute.driver(id: driverId)) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(label.uppercased()).font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                    Text(detail).font(.caption)
                }
                Spacer()
                Text(value).font(ApexFont.title).foregroundStyle(ApexColor.purpleSector)
            }
        }
        .listRowBackground(ApexColor.asphalt)
    }

    private func load() async {
        isLoading = true
        error = nil
        defer { isLoading = false }
        do {
            history = try await encyclopedia.circuitHistory(circuitId)
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
            return
        }
        // Records are a bonus: the history is still shown without them.
        records = try? await encyclopedia.circuitRecords(circuitId)
    }
}
