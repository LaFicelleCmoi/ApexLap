import ApexLapData
import ApexLapDomain
import SwiftData
import SwiftUI

@main
struct ApexLapApp: App {
    private let container: ModelContainer
    private let repository: any F1Repository

    init() {
        do {
            container = try ModelContainer(for: CachedPayload.self)
        } catch {
            fatalError("Impossible d'ouvrir la base locale : \(error)")
        }
        repository = OfflineFirstF1Repository(store: SwiftDataF1Store(modelContainer: container))
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(\.repository, repository)
                .preferredColorScheme(.dark)
                .tint(ApexColor.racingRed)
        }
        .modelContainer(container)
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            NavigationStack {
                CalendarView()
                    .withAppRoutes()
            }
            .tabItem { Label("Calendrier", systemImage: "flag.checkered") }

            NavigationStack {
                StandingsView()
                    .withAppRoutes()
            }
            .tabItem { Label("Classements", systemImage: "trophy") }
        }
    }
}
