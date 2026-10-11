import ApexLapDomain
import Charts
import Observation
import SwiftUI

@MainActor
@Observable
final class StatsViewModel {
    let season = CalendarLogic.currentSeason(now: .now)
    private(set) var results: [ScoredResult] = []
    private(set) var isRefreshing = false
    private(set) var error: String?
    var firstId: String?
    var secondId: String?

    var progression: ChampionshipProgression { SeasonStats.progression(results) }
    var leaders: [LeaderBoard] { SeasonStats.leaders(results) }

    /// Default duel: the two championship leaders.
    var duel: HeadToHead? {
        let drivers = progression.drivers
        guard let first = firstId ?? drivers.first?.driver.id,
              let second = secondId ?? drivers.first(where: { $0.driver.id != first })?.driver.id else { return nil }
        return SeasonStats.headToHead(results, first: first, second: second)
    }

    func load(using repository: any F1Repository) async {
        if results.isEmpty { results = await repository.cachedSeasonResults(season: season) }
        await refresh(using: repository)
    }

    func refresh(using repository: any F1Repository) async {
        guard !isRefreshing else { return }
        isRefreshing = true
        error = nil
        defer { isRefreshing = false }
        do {
            results = try await repository.refreshSeasonResults(season: season)
        } catch {
            self.error = OfflineFirst<Int>.message(for: error)
        }
    }
}

struct StatsView: View {
    @Environment(\.repository) private var repository
    @State private var model = StatsViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let error = model.error {
                    OfflineBanner(message: error) { Task { await model.refresh(using: repository) } }
                }
                if model.results.isEmpty {
                    if model.isRefreshing { ProgressView().frame(maxWidth: .infinity).padding(40) } else {
                        EmptyStateView(message: model.error ?? "Pas encore de course disputée") { Task { await model.refresh(using: repository) } }
                    }
                } else {
                    content
                }
            }
        }
        .background(ApexColor.asphalt)
        .navigationTitle("Stats \(String(model.season))")
        .refreshable { await model.refresh(using: repository) }
        .task { await model.load(using: repository) }
    }

    @ViewBuilder
    private var content: some View {
        let progression = model.progression
        SectionHeader(title: "Évolution du championnat")
        Card {
            Text("Points cumulés des 5 premiers · touchez le graphique pour voir chaque manche")
                .font(.caption).foregroundStyle(ApexColor.telemetry)
            ProgressionChart(rounds: progression.rounds, drivers: Array(progression.drivers.prefix(5)))
        }

        NavigationLink(value: AppRoute.telemetry(sessionKey: nil)) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Comparateur de tours").font(ApexFont.title)
                    Text("Télémétrie de deux pilotes : vitesse, gaz, freins, écart et carte du circuit.")
                        .font(.caption).foregroundStyle(ApexColor.telemetry)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(ApexColor.telemetry)
            }
            .padding(16)
            .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
        .buttonStyle(.plain)

        SectionHeader(title: "Leaders de la saison")
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
            ForEach(model.leaders) { LeaderCard(board: $0) }
        }
        .padding(.horizontal, 16)

        SectionHeader(title: "Duel")
        Card {
            HStack(spacing: 12) {
                driverPicker(selection: Binding(get: { model.duel?.first.driver.id ?? "" }, set: { model.firstId = $0 }), color: ChartPalette.colors[0])
                Text("VS").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                driverPicker(selection: Binding(get: { model.duel?.second.driver.id ?? "" }, set: { model.secondId = $0 }), color: ChartPalette.colors[1])
            }
            if let duel = model.duel {
                DuelTable(duel: duel)
                let pair = [duel.first.driver.id, duel.second.driver.id].compactMap { id in progression.drivers.first { $0.driver.id == id } }
                if pair.count == 2 {
                    ProgressionChart(rounds: progression.rounds, drivers: pair, height: 180)
                }
            } else {
                Text("Choisissez deux pilotes différents.").font(.caption).foregroundStyle(ApexColor.telemetry)
            }
        }
        .padding(.bottom, 24)
    }

    private func driverPicker(selection: Binding<String>, color: Color) -> some View {
        Picker("Pilote", selection: selection) {
            ForEach(model.progression.drivers) { Text($0.driver.familyName).tag($0.driver.id) }
        }
        .pickerStyle(.menu)
        .tint(color)
        .frame(maxWidth: .infinity)
        .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 12))
    }
}

struct Card<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) { content }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(ApexColor.carbon, in: RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 16)
    }
}

/// Cumulative points per round. Series are direct-labelled by the legend and the selection readout, so
/// identity never relies on colour alone.
struct ProgressionChart: View {
    let rounds: [Int]
    let drivers: [DriverProgression]
    var height: CGFloat = 240
    @State private var selectedRound: Int?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            chart
            legend
        }
    }

    private var chart: some View {
        Chart {
            ForEach(Array(drivers.enumerated()), id: \.element.id) { index, driver in
                ForEach(Array(zip(rounds, driver.cumulativePoints)), id: \.0) { round, points in
                    LineMark(x: .value("Manche", round), y: .value("Points", points), series: .value("Pilote", driver.driver.code))
                        .foregroundStyle(ChartPalette.colors[index % ChartPalette.colors.count])
                        .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
                }
                if let last = driver.cumulativePoints.last, let round = rounds.last {
                    PointMark(x: .value("Manche", round), y: .value("Points", last))
                        .foregroundStyle(ChartPalette.colors[index % ChartPalette.colors.count])
                        .annotation(position: .trailing, spacing: 4) {
                            Text(driver.driver.code).font(.caption2.bold()).foregroundStyle(ApexColor.floodlight)
                        }
                }
            }
            if let selectedRound {
                RuleMark(x: .value("Manche", selectedRound))
                    .foregroundStyle(ApexColor.telemetry.opacity(0.6))
                    .annotation(position: .top, overflowResolution: .init(x: .fit(to: .chart), y: .disabled)) {
                        tooltip(round: selectedRound)
                    }
            }
        }
        .chartXSelection(value: $selectedRound)
        .chartXAxis { AxisMarks(values: .automatic(desiredCount: 8)) { value in
            AxisGridLine().foregroundStyle(ApexColor.kerb)
            AxisValueLabel { if let r = value.as(Int.self) { Text("M\(r)") } }
        } }
        .chartYAxis { AxisMarks(position: .leading, values: .automatic(desiredCount: 4)) {
            AxisGridLine().foregroundStyle(ApexColor.kerb)
            AxisValueLabel()
        } }
        .chartLegend(.hidden)
        .frame(height: height)
        .padding(.trailing, 28)
        .accessibilityLabel("Évolution des points sur \(rounds.count) manches")
    }

    private var legend: some View {
        HStack(spacing: 14) {
            ForEach(Array(drivers.enumerated()), id: \.element.id) { index, driver in
                HStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 2).fill(ChartPalette.colors[index % ChartPalette.colors.count]).frame(width: 14, height: 3)
                    Text("\(driver.driver.code) \(Format.points(driver.total))").font(.caption)
                }
            }
        }
    }

    private func tooltip(round: Int) -> some View {
        let index = rounds.firstIndex(of: round) ?? 0
        let rows = drivers.enumerated()
            .map { (code: $0.element.driver.code, color: ChartPalette.colors[$0.offset % ChartPalette.colors.count], points: $0.element.cumulativePoints[index]) }
            .sorted { $0.points > $1.points }
        return VStack(alignment: .leading, spacing: 2) {
            Text("APRÈS LA MANCHE \(round)").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
            ForEach(rows, id: \.code) { row in
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 2).fill(row.color).frame(width: 10, height: 10)
                    Text("\(row.code)  \(Format.points(row.points)) pts").font(.caption)
                }
            }
        }
        .padding(8)
        .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 10))
    }
}

private struct LeaderCard: View {
    let board: LeaderBoard

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(board.category.title.uppercased()).font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
            if board.entries.isEmpty { Text("—").font(ApexFont.title) }
            ForEach(Array(board.entries.enumerated()), id: \.element.id) { index, entry in
                NavigationLink(value: AppRoute.driver(id: entry.driver.id)) {
                    HStack {
                        TeamStripe(color: TeamColor.of(entry.constructor.id), height: 16)
                        Text(entry.driver.code).font(index == 0 ? ApexFont.title : .subheadline)
                        Spacer()
                        Text("\(entry.value)").font(index == 0 ? ApexFont.title : .subheadline)
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(ApexColor.carbon, in: RoundedRectangle(cornerRadius: 14))
    }
}

private struct DuelTable: View {
    let duel: HeadToHead

    private struct Metric: Identifiable {
        let label: String
        let first: Double?
        let second: Double?
        var lowerIsBetter = false
        var bars = true
        var format: (Double) -> String = { "\(Int($0))" }
        var id: String { label }
    }

    private var metrics: [Metric] {
        let a = duel.first, b = duel.second
        return [
            Metric(label: "Points", first: a.points, second: b.points, format: Format.points),
            Metric(label: "Devant en course", first: Double(a.racesAhead), second: Double(b.racesAhead)),
            Metric(label: "Devant au départ", first: Double(a.startsAhead), second: Double(b.startsAhead)),
            Metric(label: "Victoires", first: Double(a.wins), second: Double(b.wins)),
            Metric(label: "Podiums", first: Double(a.podiums), second: Double(b.podiums)),
            Metric(label: "Poles", first: Double(a.poles), second: Double(b.poles)),
            Metric(label: "Meilleur résultat", first: a.bestFinish.map(Double.init), second: b.bestFinish.map(Double.init),
                   lowerIsBetter: true, bars: false, format: { "P\(Int($0))" }),
            Metric(label: "Arrivée moyenne", first: a.averageFinish, second: b.averageFinish,
                   lowerIsBetter: true, bars: false, format: { String(format: "%.1f", $0) }),
            Metric(label: "Abandons", first: Double(a.dnfs), second: Double(b.dnfs), lowerIsBetter: true),
        ]
    }

    var body: some View {
        VStack(spacing: 10) {
            HStack {
                name(duel.first, alignment: .leading)
                Text("\(duel.sharedRaces) GP").font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                name(duel.second, alignment: .trailing)
            }
            ForEach(metrics) { metric in row(metric) }
        }
    }

    private func name(_ side: DuelSide, alignment: HorizontalAlignment) -> some View {
        VStack(alignment: alignment) {
            Text(side.driver.code).font(ApexFont.headline)
            Text(side.constructor.name).font(ApexFont.label).foregroundStyle(ApexColor.telemetry).lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: alignment == .leading ? .leading : .trailing)
    }

    private func row(_ metric: Metric) -> some View {
        let a = metric.first, b = metric.second
        let firstBetter = a != nil && (b == nil || (metric.lowerIsBetter ? a! < b! : a! > b!))
        let secondBetter = b != nil && (a == nil || (metric.lowerIsBetter ? b! < a! : b! > a!))
        let maxValue = max(a ?? 0, b ?? 0)
        return VStack(spacing: 3) {
            HStack {
                value(a.map(metric.format) ?? "—", better: firstBetter, alignment: .leading)
                Text(metric.label).font(.caption).foregroundStyle(ApexColor.telemetry)
                value(b.map(metric.format) ?? "—", better: secondBetter, alignment: .trailing)
            }
            if metric.bars && maxValue > 0 {
                HStack(spacing: 4) {
                    bar(fraction: (a ?? 0) / maxValue, color: ChartPalette.colors[0], fromTrailing: true)
                    bar(fraction: (b ?? 0) / maxValue, color: ChartPalette.colors[1], fromTrailing: false)
                }
                .frame(height: 6)
            }
        }
    }

    private func value(_ text: String, better: Bool, alignment: HorizontalAlignment) -> some View {
        Text(text)
            .font(.headline.weight(better ? .black : .regular))
            .foregroundStyle(better ? ApexColor.floodlight : ApexColor.telemetry)
            .frame(maxWidth: .infinity, alignment: alignment == .leading ? .leading : .trailing)
    }

    /// Bars grow outward from the centre line.
    private func bar(fraction: Double, color: Color, fromTrailing: Bool) -> some View {
        GeometryReader { proxy in
            RoundedRectangle(cornerRadius: 3).fill(color)
                .frame(width: proxy.size.width * fraction)
                .frame(maxWidth: .infinity, alignment: fromTrailing ? .trailing : .leading)
        }
    }
}
