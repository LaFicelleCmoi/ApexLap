import ApexLapData
import Foundation
import Observation

/// View-model glue for the offline-first pattern: shows the cache instantly, then refreshes from the
/// network and reports failures without discarding what is already on screen.
@MainActor
@Observable
final class OfflineFirst<Value> {
    private(set) var value: Value?
    private(set) var isRefreshing = false
    private(set) var error: String?

    func load(cached: () async -> Value?, refresh: () async throws -> Value) async {
        if value == nil {
            value = await cached()
        }
        await self.refresh(refresh)
    }

    func refresh(_ refresh: () async throws -> Value) async {
        guard !isRefreshing else { return }
        isRefreshing = true
        error = nil
        defer { isRefreshing = false }
        do {
            value = try await refresh()
        } catch is CancellationError {
            // The view went away; keep the current state.
        } catch {
            self.error = Self.message(for: error)
        }
    }

    static func message(for error: Error) -> String {
        switch error {
        case NetworkError.offline: "Hors ligne — affichage des données en cache"
        case let NetworkError.http(status): "Serveur indisponible (HTTP \(status))"
        default: "Impossible de charger les données"
        }
    }
}
