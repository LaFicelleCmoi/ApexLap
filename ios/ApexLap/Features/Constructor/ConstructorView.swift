import ApexLapDomain
import SwiftUI

struct ConstructorView: View {
    @Environment(\.repository) private var repository
    @Environment(\.encyclopedia) private var encyclopedia
    @Environment(\.strings) private var strings
    @Environment(SettingsStore.self) private var store
    @State private var model: ConstructorViewModel
    @State private var titles = Titles.none

    init(constructorId: String) {
        _model = State(initialValue: ConstructorViewModel(constructorId: constructorId))
    }

    var body: some View {
        Group {
            if let profile = model.profile.value {
                content(profile)
            } else if model.profile.isRefreshing {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                EmptyStateView(message: model.profile.error ?? strings.teamNotFound) {
                    Task { await model.refresh(using: repository) }
                }
            }
        }
        .background(ApexColor.asphalt)
        .navigationTitle(model.profile.value?.constructor.name ?? strings.team)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            FollowButton(isFollowed: store.settings.favoriteConstructorId == model.constructorId) { store.toggleConstructor(model.constructorId) }
        }
        .task {
            titles = await encyclopedia.champions().constructorTitles(model.constructorId)
            await model.load(using: repository)
        }
    }

    private func content(_ profile: ConstructorProfile) -> some View {
        let color = TeamColor.of(profile.constructor.id)
        let stats = profile.stats

        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let error = model.profile.error {
                    OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(profile.constructor.name.uppercased()).font(ApexFont.headline)
                    Text("\(Format.flag(profile.constructor.nationality)) \(profile.constructor.nationality)")
                        .font(.subheadline).foregroundStyle(ApexColor.telemetry)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                .background(
                    LinearGradient(colors: [color.opacity(0.5), ApexColor.carbon], startPoint: .leading, endPoint: .trailing),
                    in: RoundedRectangle(cornerRadius: 20)
                )
                .padding(16)

                SectionHeader(title: strings.bio)
                Text(Bios.constructor(profile, titles: titles, standing: model.standing, season: model.season, language: strings.language))
                    .font(.subheadline)
                    .padding(.horizontal, 16)

                if let standing = model.standing {
                    SectionHeader(title: strings.seasonHeader(model.season))
                    HStack(spacing: 12) {
                        StatTile(label: strings.statPosition, value: "P\(standing.position)", accent: color)
                        StatTile(label: strings.statPoints, value: Format.points(standing.points), accent: color)
                        StatTile(label: strings.statWins, value: "\(standing.wins)", accent: color)
                    }
                    .padding(.horizontal, 16)
                }

                if !model.drivers.isEmpty {
                    SectionHeader(title: strings.drivers)
                    ForEach(model.drivers) { driver in
                        NavigationLink(value: AppRoute.driver(id: driver.driver.id)) {
                            HStack(spacing: 12) {
                                HelmetView(color: color, size: 44)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(driver.driver.fullName).font(ApexFont.title)
                                    Text("P\(driver.position) · \(Format.points(driver.points)) \(strings.pts)")
                                        .font(.caption).foregroundStyle(ApexColor.telemetry)
                                }
                                Spacer()
                                Text(driver.driver.number ?? "").font(ApexFont.headline).foregroundStyle(color)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                        }
                        .buttonStyle(.plain)
                    }
                }

                if titles.count > 0 {
                    SectionHeader(title: strings.constructorTitles)
                    TitlesRow(titles: titles)
                }

                SectionHeader(title: strings.history)
                Grid(horizontalSpacing: 12, verticalSpacing: 12) {
                    GridRow {
                        StatTile(label: strings.statSeasons, value: "\(stats.seasons)")
                        StatTile(label: strings.statStarts, value: "\(stats.races)")
                        StatTile(label: strings.statPoles, value: "\(stats.poles)", accent: ApexColor.purpleSector)
                    }
                    GridRow {
                        StatTile(label: strings.statWins, value: "\(stats.wins)", accent: ApexColor.gold)
                        StatTile(label: strings.statPodiums, value: "\(stats.podiums)", accent: ApexColor.silver)
                        Color.clear.gridCellUnsizedAxes([.horizontal, .vertical])
                    }
                }
                .padding(.horizontal, 16)

                Text(strings.constructorStatsNote)
                    .font(.caption).foregroundStyle(ApexColor.telemetry).padding(16)
            }
        }
        .refreshable { await model.refresh(using: repository) }
    }
}
