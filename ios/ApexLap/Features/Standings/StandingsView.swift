import ApexLapDomain
import SwiftUI

struct StandingsView: View {
    enum Tab: CaseIterable, Identifiable {
        case drivers, constructors
        var id: Self { self }
    }

    @Environment(\.repository) private var repository
    @Environment(\.strings) private var strings
    @Environment(SettingsStore.self) private var store
    @State private var model = StandingsViewModel()
    @State private var tab: Tab = .drivers

    var body: some View {
        List {
            Picker(strings.navStandings, selection: $tab) {
                ForEach(Tab.allCases) { Text($0 == .drivers ? strings.drivers : strings.constructors).tag($0) }
            }
            .pickerStyle(.segmented)
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            if let error = model.standings.error {
                OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }

            if !model.hasData && model.standings.isRefreshing {
                ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
            } else {
                switch tab {
                case .drivers:
                    ForEach(model.drivers) { standing in
                        NavigationLink(value: AppRoute.driver(id: standing.driver.id)) {
                            StandingRow(
                                position: standing.position,
                                teamId: standing.constructor?.id,
                                title: "\(Format.flag(standing.driver.nationality))  \(standing.driver.fullName)",
                                subtitle: standing.constructor?.name ?? "",
                                points: standing.points,
                                wins: standing.wins,
                                isFavorite: standing.driver.id == store.settings.favoriteDriverId
                            )
                        }
                        .listRowBackground(standing.driver.id == store.settings.favoriteDriverId ? ApexColor.gold.opacity(0.12) : ApexColor.asphalt)
                    }
                case .constructors:
                    ForEach(model.constructors) { standing in
                        NavigationLink(value: AppRoute.constructor(id: standing.constructor.id)) {
                            StandingRow(
                                position: standing.position,
                                teamId: standing.constructor.id,
                                title: standing.constructor.name,
                                subtitle: standing.constructor.nationality,
                                points: standing.points,
                                wins: standing.wins,
                                isFavorite: standing.constructor.id == store.settings.favoriteConstructorId
                            )
                        }
                        .listRowBackground(standing.constructor.id == store.settings.favoriteConstructorId ? ApexColor.gold.opacity(0.12) : ApexColor.asphalt)
                    }
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .overlay {
            if model.hasData && !model.query.isEmpty && (tab == .drivers ? model.drivers.isEmpty : model.constructors.isEmpty) {
                ContentUnavailableView.search(text: model.query)
            } else if !model.hasData && !model.standings.isRefreshing {
                EmptyStateView(message: model.standings.error ?? strings.noStandingsYet) {
                    Task { await model.refresh(using: repository) }
                }
            }
        }
        .searchable(text: $model.query, prompt: strings.searchDriverTeam)
        .navigationTitle(strings.championshipTitle(model.season))
        .refreshable { await model.refresh(using: repository) }
        .task { await model.load(using: repository) }
    }
}

private struct StandingRow: View {
    @Environment(\.strings) private var strings
    let position: Int
    let teamId: String?
    let title: String
    let subtitle: String
    let points: Double
    let wins: Int
    let isFavorite: Bool

    var body: some View {
        HStack(spacing: 8) {
            PositionText(position: position)
            TeamStripe(color: TeamColor.of(teamId), height: 34)
            VStack(alignment: .leading, spacing: 2) {
                Text(isFavorite ? "\(title)  ★" : title).font(.subheadline.weight(.semibold))
                Text(subtitle).font(.caption).foregroundStyle(ApexColor.telemetry)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(Format.points(points)).font(ApexFont.title)
                Text(wins > 0 ? strings.winsShort(wins) : strings.ptsUpper).font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
            }
        }
    }
}
