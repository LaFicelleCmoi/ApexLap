import ApexLapData
import ApexLapDomain
import Observation
import SwiftData
import SwiftUI
import UserNotifications

@main
struct ApexLapApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
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
                .environment(AppRouter.shared)
                .preferredColorScheme(.dark)
                .tint(ApexColor.racingRed)
        }
        .modelContainer(container)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { Task { await ReminderScheduler.shared.reschedule() } }
        }
    }
}

/// Routes a tapped session reminder to its Grand Prix.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let info = response.notification.request.content.userInfo
        guard let season = info["season"] as? Int, let round = info["round"] as? Int else { return }
        await MainActor.run { AppRouter.shared.open(.race(season: season, round: round)) }
    }
}

enum AppTab: Hashable {
    case calendar, results, live, standings, stats
}

@MainActor
@Observable
final class AppRouter {
    static let shared = AppRouter()
    var tab: AppTab = .calendar
    var calendarPath: [AppRoute] = []

    func open(_ route: AppRoute) {
        tab = .calendar
        calendarPath.append(route)
    }
}

struct RootView: View {
    @Environment(AppRouter.self) private var router

    var body: some View {
        @Bindable var router = router
        TabView(selection: $router.tab) {
            NavigationStack(path: $router.calendarPath) {
                CalendarView().withAppRoutes()
            }
            .tabItem { Label("Calendrier", systemImage: "flag.checkered") }
            .tag(AppTab.calendar)

            NavigationStack {
                ResultsView().withAppRoutes()
            }
            .tabItem { Label("Résultats", systemImage: "list.number") }
            .tag(AppTab.results)

            NavigationStack {
                LiveView().withAppRoutes()
            }
            .tabItem { Label("Live", systemImage: "dot.radiowaves.left.and.right") }
            .tag(AppTab.live)

            NavigationStack {
                StandingsView().withAppRoutes()
            }
            .tabItem { Label("Classements", systemImage: "trophy") }
            .tag(AppTab.standings)

            NavigationStack {
                StatsView().withAppRoutes()
            }
            .tabItem { Label("Stats", systemImage: "chart.xyaxis.line") }
            .tag(AppTab.stats)
        }
    }
}
