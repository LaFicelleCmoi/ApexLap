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
                .environment(SettingsStore.shared)
                .preferredColorScheme(.dark)
                .tint(ApexColor.racingRed)
                // The widget opens a Grand Prix with apexlap://race/<season>/<round>.
                .onOpenURL { url in
                    guard url.scheme == "apexlap", url.host == "race" else { return }
                    let parts = url.pathComponents.compactMap { Int($0) }
                    if parts.count == 2 { AppRouter.shared.open(.race(season: parts[0], round: parts[1])) }
                }
        }
        .modelContainer(container)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                SettingsStore.shared.refreshSystemLanguage()
                Task { await ReminderScheduler.shared.reschedule() }
            }
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
    @Environment(SettingsStore.self) private var settings

    var body: some View {
        @Bindable var router = router
        // Every screen reads its texts from the environment, so picking a language redraws the app in place.
        let strings = settings.strings
        TabView(selection: $router.tab) {
            NavigationStack(path: $router.calendarPath) {
                CalendarView().withAppRoutes()
            }
            .tabItem { Label(strings.navCalendar, systemImage: "flag.checkered") }
            .tag(AppTab.calendar)

            NavigationStack {
                ResultsView().withAppRoutes()
            }
            .tabItem { Label(strings.navResults, systemImage: "list.number") }
            .tag(AppTab.results)

            NavigationStack {
                LiveView().withAppRoutes()
            }
            .tabItem { Label(strings.navLive, systemImage: "dot.radiowaves.left.and.right") }
            .tag(AppTab.live)

            NavigationStack {
                StandingsView().withAppRoutes()
            }
            .tabItem { Label(strings.navStandings, systemImage: "trophy") }
            .tag(AppTab.standings)

            NavigationStack {
                StatsView().withAppRoutes()
            }
            .tabItem { Label(strings.navStats, systemImage: "chart.xyaxis.line") }
            .tag(AppTab.stats)
        }
        .environment(\.strings, strings)
    }
}
