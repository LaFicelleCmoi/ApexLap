import ApexLapDomain
import SwiftUI

struct DriverView: View {
    @Environment(\.repository) private var repository
    @State private var model: DriverViewModel

    init(driverId: String) {
        _model = State(initialValue: DriverViewModel(driverId: driverId))
    }

    var body: some View {
        Group {
            if let profile = model.profile.value {
                content(profile)
            } else if model.profile.isRefreshing {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                EmptyStateView(message: model.profile.error ?? "Pilote introuvable") {
                    Task { await model.refresh(using: repository) }
                }
            }
        }
        .background(ApexColor.asphalt)
        .navigationTitle(model.profile.value?.driver.fullName ?? "Pilote")
        .navigationBarTitleDisplayMode(.inline)
        .task { await model.load(using: repository) }
    }

    private func content(_ profile: DriverProfile) -> some View {
        let team = model.standing?.constructor
        let color = TeamColor.of(team?.id)
        let stats = profile.stats

        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let error = model.profile.error {
                    OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                }

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(profile.driver.number ?? "—").font(ApexFont.hero).foregroundStyle(color)
                        Text(profile.driver.givenName).font(ApexFont.title)
                        Text(profile.driver.familyName.uppercased()).font(ApexFont.headline)
                        Text("\(Format.flag(profile.driver.nationality)) \(profile.driver.nationality) · \(profile.driver.code)")
                            .font(.caption).foregroundStyle(ApexColor.telemetry).padding(.top, 4)
                    }
                    Spacer()
                    HelmetView(color: color)
                }
                .padding(20)
                .background(
                    LinearGradient(colors: [color.opacity(0.45), ApexColor.carbon], startPoint: .leading, endPoint: .trailing),
                    in: RoundedRectangle(cornerRadius: 20)
                )
                .padding(16)

                if let team {
                    NavigationLink(value: AppRoute.constructor(id: team.id)) {
                        HStack {
                            Text("ÉCURIE").font(ApexFont.label).foregroundStyle(ApexColor.asphalt)
                                .padding(.horizontal, 6).padding(.vertical, 2)
                                .background(color, in: RoundedRectangle(cornerRadius: 3))
                            Text(team.name).font(ApexFont.title)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(ApexColor.telemetry)
                        }
                        .padding(.horizontal, 16)
                    }
                    .buttonStyle(.plain)
                }

                if let standing = model.standing {
                    SectionHeader(title: "Saison \(model.season)")
                    HStack(spacing: 12) {
                        StatTile(label: "Position", value: "P\(standing.position)", accent: color)
                        StatTile(label: "Points", value: Format.points(standing.points), accent: color)
                        StatTile(label: "Victoires", value: "\(standing.wins)", accent: color)
                    }
                    .padding(.horizontal, 16)
                }

                SectionHeader(title: "Carrière")
                Grid(horizontalSpacing: 12, verticalSpacing: 12) {
                    GridRow {
                        StatTile(label: "Saisons", value: "\(stats.seasons)")
                        StatTile(label: "Courses", value: "\(stats.races)")
                        StatTile(label: "Poles", value: "\(stats.poles)", accent: ApexColor.purpleSector)
                    }
                    GridRow {
                        StatTile(label: "Victoires", value: "\(stats.wins)", accent: ApexColor.gold)
                        StatTile(label: "Podiums", value: "\(stats.podiums)", accent: ApexColor.silver)
                        StatTile(label: "% victoires",
                                 value: "\(stats.races > 0 ? stats.wins * 100 / stats.races : 0)%",
                                 accent: ApexColor.greenSector)
                    }
                }
                .padding(.horizontal, 16)

                if let birth = profile.driver.dateOfBirth {
                    Text("Né le \(Format.utcDay(birth))")
                        .font(.caption).foregroundStyle(ApexColor.telemetry).padding(16)
                }
            }
        }
        .refreshable { await model.refresh(using: repository) }
    }
}
