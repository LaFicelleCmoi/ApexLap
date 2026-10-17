import ApexLapData
import ApexLapDomain
import SwiftUI

/// Global settings: language, favourites, reminders and about.
struct SettingsView: View {
    @Environment(\.repository) private var repository
    @Environment(\.strings) private var strings
    @Environment(SettingsStore.self) private var store
    @State private var drivers: [DriverStanding] = []
    @State private var constructors: [ConstructorStanding] = []
    @State private var error: String?
    private let season = CalendarLogic.currentSeason(now: .now)

    var body: some View {
        Form {
            Section(strings.languageLabel) {
                Picker(strings.languageLabel, selection: Binding(get: { store.settings.language }, set: { store.setLanguage($0) })) {
                    ForEach(LanguagePreference.allCases, id: \.self) { option in
                        Text(option.language?.nativeName ?? strings.systemLanguage).tag(option)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            }

            Section {
                Picker(strings.favoriteDriver, selection: Binding(get: { store.settings.favoriteDriverId }, set: { store.setFavoriteDriver($0) })) {
                    Text(strings.none).tag(String?.none)
                    ForEach(drivers) { standing in
                        Text("\(standing.driver.fullName) · \(standing.constructor?.name ?? "")").tag(Optional(standing.driver.id))
                    }
                    // Keeps a followed driver selectable even before the standings load.
                    if let id = store.settings.favoriteDriverId, !drivers.contains(where: { $0.driver.id == id }) {
                        Text(id).tag(Optional(id))
                    }
                }
                Picker(strings.favoriteTeam, selection: Binding(get: { store.settings.favoriteConstructorId }, set: { store.setFavoriteConstructor($0) })) {
                    Text(strings.none).tag(String?.none)
                    ForEach(constructors) { standing in
                        Text(standing.constructor.name).tag(Optional(standing.constructor.id))
                    }
                    if let id = store.settings.favoriteConstructorId, !constructors.contains(where: { $0.constructor.id == id }) {
                        Text(id).tag(Optional(id))
                    }
                }
                if let error, drivers.isEmpty {
                    Text(error).font(.footnote).foregroundStyle(.red)
                }
            } header: {
                Text(strings.myFavorites)
            } footer: {
                Text(strings.favoriteHint)
            }

            Section(strings.notifications) {
                NavigationLink(value: AppRoute.reminders) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(strings.remindersAndWidget)
                        Text(strings.remindersAndWidgetHint).font(.caption).foregroundStyle(ApexColor.telemetry)
                    }
                }
            }

            Section(strings.about) {
                let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
                Text("Apex Lap · \(strings.version(version))")
                Text(strings.dataSources).font(.footnote).foregroundStyle(ApexColor.telemetry)
            }
        }
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .navigationTitle(strings.settings)
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    /// Current-season standings are the lists to pick favourites from.
    private func load() async {
        drivers = await repository.cachedDriverStandings(season: season)
        constructors = await repository.cachedConstructorStandings(season: season)
        do {
            let fresh = try await repository.refreshStandings(season: season)
            drivers = fresh.0
            constructors = fresh.1
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }
}
