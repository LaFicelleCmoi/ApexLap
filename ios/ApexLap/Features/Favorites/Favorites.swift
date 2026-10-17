import ApexLapData
import ApexLapDomain
import SwiftUI

/// The followed driver and team at a glance on the home screen, or an invitation to pick them.
struct FavoritesCard: View {
    @Environment(\.repository) private var repository
    @Environment(\.strings) private var strings
    @Environment(SettingsStore.self) private var store
    @State private var drivers: [DriverStanding] = []
    @State private var constructors: [ConstructorStanding] = []
    let season: Int

    var body: some View {
        let settings = store.settings
        VStack(alignment: .leading, spacing: 0) {
            if !settings.hasFavorites {
                NavigationLink(value: AppRoute.settings) {
                    HStack(spacing: 12) {
                        Image(systemName: "star.fill").foregroundStyle(ApexColor.gold)
                        Text(strings.chooseFavorites).font(.subheadline)
                        Spacer()
                        Image(systemName: "chevron.right").foregroundStyle(ApexColor.telemetry)
                    }
                    .padding(16)
                }
                .buttonStyle(.plain)
            } else {
                HStack(spacing: 6) {
                    Image(systemName: "star.fill").foregroundStyle(ApexColor.gold)
                    Text(strings.myFavorites.uppercased()).font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                }
                .padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 4)
                if let id = settings.favoriteDriverId {
                    let standing = drivers.first { $0.driver.id == id }
                    row(route: .driver(id: id), teamId: standing?.constructor?.id,
                        title: standing.map { "\(Format.flag($0.driver.nationality))  \($0.driver.fullName)" } ?? id,
                        subtitle: standing.map { strings.standingLine($0.position, Format.points($0.points), $0.wins) } ?? strings.notRankedYet)
                }
                if let id = settings.favoriteConstructorId {
                    let standing = constructors.first { $0.constructor.id == id }
                    row(route: .constructor(id: id), teamId: id, title: standing?.constructor.name ?? id,
                        subtitle: standing.map { strings.standingLine($0.position, Format.points($0.points), $0.wins) } ?? strings.notRankedYet)
                }
            }
        }
        .padding(.bottom, settings.hasFavorites ? 6 : 0)
        .background(ApexColor.carbon, in: RoundedRectangle(cornerRadius: 16))
        .task(id: settings) { await load(settings) }
    }

    private func row(route: AppRoute, teamId: String?, title: String, subtitle: String) -> some View {
        NavigationLink(value: route) {
            HStack(spacing: 12) {
                TeamStripe(color: TeamColor.of(teamId), height: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.subheadline.weight(.semibold))
                    Text(subtitle).font(.caption).foregroundStyle(ApexColor.telemetry)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(ApexColor.telemetry)
            }
            .padding(.horizontal, 16).padding(.vertical, 6)
        }
        .buttonStyle(.plain)
    }

    /// The home screen does not load the standings itself: fetch them when something is followed.
    private func load(_ settings: AppSettings) async {
        drivers = await repository.cachedDriverStandings(season: season)
        constructors = await repository.cachedConstructorStandings(season: season)
        guard settings.hasFavorites, let fresh = try? await repository.refreshStandings(season: season) else { return }
        drivers = fresh.0
        constructors = fresh.1
    }
}

/// Star in the navigation bar of a driver or team page: follow / unfollow.
struct FollowButton: View {
    @Environment(\.strings) private var strings
    let isFollowed: Bool
    let toggle: () -> Void

    var body: some View {
        Button(action: toggle) {
            Image(systemName: isFollowed ? "star.fill" : "star")
                .foregroundStyle(isFollowed ? ApexColor.gold : ApexColor.floodlight)
        }
        .accessibilityLabel(isFollowed ? strings.unfollow : strings.follow)
    }
}
