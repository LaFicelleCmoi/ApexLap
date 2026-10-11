import ApexLapDomain
import Observation
import SwiftUI

struct RaceSummary: Identifiable {
    let race: Race
    let podium: [RaceResult]
    let sprintWinner: RaceResult?
    var id: Int { race.round }
}

@MainActor
@Observable
final class ResultsViewModel {
    let season = CalendarLogic.currentSeason(now: .now)
    private(set) var summaries: [RaceSummary] = []
    private(set) var isRefreshing = false
    private(set) var error: String?

    func load(using repository: any F1Repository) async {
        if summaries.isEmpty {
            summaries = Self.summarize(await repository.cachedCalendar(season: season), await repository.cachedSeasonResults(season: season))
        }
        await refresh(using: repository)
    }

    func refresh(using repository: any F1Repository) async {
        guard !isRefreshing else { return }
        isRefreshing = true
        error = nil
        defer { isRefreshing = false }
        do {
            let results = try await repository.refreshSeasonResults(season: season)
            var calendar = await repository.cachedCalendar(season: season)
            if calendar.isEmpty { calendar = try await repository.refreshCalendar(season: season) }
            summaries = Self.summarize(calendar, results)
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }

    static func summarize(_ calendar: [Race], _ results: [ScoredResult]) -> [RaceSummary] {
        let byRound = Dictionary(grouping: results, by: \.round)
        return calendar.compactMap { race -> RaceSummary? in
            guard let rows = byRound[race.round] else { return nil }
            let podium = rows.filter { !$0.isSprint && (1...3).contains($0.result.position) }.map(\.result).sorted { $0.position < $1.position }
            guard !podium.isEmpty else { return nil }
            return RaceSummary(race: race, podium: podium, sprintWinner: rows.first { $0.isSprint && $0.result.position == 1 }?.result)
        }
        .sorted { $0.race.round > $1.race.round }
    }
}

struct ResultsView: View {
    @Environment(\.repository) private var repository
    @State private var model = ResultsViewModel()

    var body: some View {
        List {
            if let error = model.error {
                OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }
            ForEach(model.summaries) { summary in
                NavigationLink(value: AppRoute.race(season: summary.race.season, round: summary.race.round)) {
                    SummaryCard(summary: summary)
                }
                .listRowBackground(ApexColor.carbon)
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(ApexColor.asphalt)
        .overlay {
            if model.summaries.isEmpty {
                if model.isRefreshing { ProgressView() } else {
                    EmptyStateView(message: model.error ?? "Aucun Grand Prix terminé") { Task { await model.refresh(using: repository) } }
                }
            }
        }
        .navigationTitle("Résultats \(String(model.season))")
        .refreshable { await model.refresh(using: repository) }
        .task { await model.load(using: repository) }
    }
}

private struct SummaryCard: View {
    let summary: RaceSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(Format.flag(summary.race.circuit.country)).font(.title2)
                VStack(alignment: .leading) {
                    Text(summary.race.name).font(ApexFont.title)
                    Text("MANCHE \(summary.race.round)").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                }
            }
            ForEach(summary.podium) { result in
                HStack(spacing: 8) {
                    PositionText(position: result.position)
                    TeamStripe(color: TeamColor.of(result.constructor.id), height: 18)
                    Text(result.driver.fullName).font(.subheadline)
                    Spacer()
                    if result.hasFastestLap { Text("⏱").foregroundStyle(ApexColor.purpleSector) }
                    Text(result.time ?? result.status).font(.caption).foregroundStyle(ApexColor.telemetry)
                }
            }
            if let winner = summary.sprintWinner {
                Text("SPRINT · \(winner.driver.fullName) (\(winner.constructor.name))")
                    .font(ApexFont.label).foregroundStyle(ApexColor.telemetry).lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}
