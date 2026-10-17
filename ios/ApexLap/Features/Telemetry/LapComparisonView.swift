import ApexLapDomain
import ApexLapLive
import Charts
import Observation
import SwiftUI

@MainActor
@Observable
final class LapComparisonViewModel {
    private let requested: Int?
    private let repository = OpenF1Account.shared.repository
    private(set) var sessions: [LiveSession] = []
    private(set) var session: LiveSession?
    private(set) var drivers: [LiveDriver] = []
    private(set) var laps: [Int: [LapRef]] = [:]
    private(set) var lapA: LapRef?
    private(set) var lapB: LapRef?
    private(set) var comparison: LapComparison?
    private(set) var track: [Telemetry.MappedPoint] = []
    private(set) var isLoading = false
    private(set) var error: String?

    init(sessionKey: Int?) { requested = sessionKey }

    func driver(_ number: Int?) -> LiveDriver? { drivers.first { $0.number == number } }

    func load() async {
        guard sessions.isEmpty else { return }
        await run {
            let now = Date()
            self.sessions = try await self.repository.sessions(year: CalendarLogic.currentSeason(now: now)).filter { $0.hasEnded(at: now) }
            let initial = self.sessions.first { $0.key == self.requested } ?? self.sessions.first { $0.type == "Qualifying" } ?? self.sessions.first
            if let initial { try await self.loadSession(initial) }
        }
    }

    func select(session: LiveSession) async { await run { try await self.loadSession(session) } }

    func select(driver number: Int, slot: Int) async {
        guard let lap = Self.fastest(laps[number]) else { return }
        await select(lap: lap, slot: slot)
    }

    func select(lap: LapRef, slot: Int) async {
        if slot == 0 { lapA = lap } else { lapB = lap }
        await run { try await self.compare() }
    }

    private func loadSession(_ session: LiveSession) async throws {
        self.session = session
        comparison = nil
        track = []
        let allDrivers = try await repository.drivers(session: session.key)
        laps = try await repository.laps(session: session.key)
        drivers = allDrivers.filter { laps[$0.number] != nil }
        // Default duel: the two fastest drivers of the session, on their best laps.
        let ranked = laps.values.compactMap { Self.fastest($0) }.sorted { $0.duration < $1.duration }
        lapA = ranked.first
        lapB = ranked.dropFirst().first
        try await compare()
    }

    private func compare() async throws {
        guard let session, let lapA, let lapB else { return }
        guard let a = try await repository.trace(session: session.key, lap: lapA),
              let b = try await repository.trace(session: session.key, lap: lapB) else {
            comparison = nil
            error = I18n.strings.telemetryUnavailable
            return
        }
        comparison = Telemetry.compare(a, b)
        track = Telemetry.mapTrack(a, points: try await repository.location(session: session.key, lap: lapA))
    }

    private func run(_ work: @escaping () async throws -> Void) async {
        isLoading = true
        error = nil
        defer { isLoading = false }
        do { try await work() } catch { self.error = OfflineFirst<Int>.message(for: error) }
    }

    static func fastest(_ laps: [LapRef]?) -> LapRef? { laps?.filter { $0.duration > 0 }.min { $0.duration < $1.duration } }
}

struct LapComparisonView: View {
    @Environment(\.strings) private var strings
    @State private var model: LapComparisonViewModel
    @State private var selectedDistance: Double?
    private let colors = [ChartPalette.colors[0], ChartPalette.colors[1]]

    init(sessionKey: Int?) {
        _model = State(initialValue: LapComparisonViewModel(sessionKey: sessionKey))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if model.isLoading { ProgressView().frame(maxWidth: .infinity).padding(.vertical, 8) }
                if let error = model.error { OfflineBanner(message: error) { Task { await model.load() } } }
                pickers
                if let comparison = model.comparison { charts(comparison) }
            }
        }
        .background(ApexColor.asphalt)
        .navigationTitle(strings.lapComparison)
        .navigationBarTitleDisplayMode(.inline)
        .task { await model.load() }
    }

    private var codeA: String { model.driver(model.lapA?.driver)?.acronym ?? "A" }
    private var codeB: String { model.driver(model.lapB?.driver)?.acronym ?? "B" }

    private var pickers: some View {
        VStack(spacing: 10) {
            Menu {
                ForEach(model.sessions.prefix(40)) { session in
                    Button("\(session.circuit) · \(session.localizedName(strings)) — \(Format.day(session.start))") {
                        selectedDistance = nil
                        Task { await model.select(session: session) }
                    }
                }
            } label: {
                pickerLabel(model.session.map { "\($0.circuit) · \($0.localizedName(strings)) — \(Format.day($0.start))" } ?? strings.sessionLabel, color: nil)
            }
            HStack(spacing: 10) {
                lapPicker(slot: 0, lap: model.lapA)
                lapPicker(slot: 1, lap: model.lapB)
            }
        }
        .padding(16)
    }

    private func lapPicker(slot: Int, lap: LapRef?) -> some View {
        let driver = model.driver(lap?.driver)
        let driverLaps = (driver.flatMap { model.laps[$0.number] } ?? []).sorted { $0.duration < $1.duration }
        return VStack(spacing: 6) {
            Menu {
                ForEach(model.drivers) { d in
                    Button("\(d.acronym) — \(d.fullName)") { Task { await model.select(driver: d.number, slot: slot) } }
                }
            } label: { pickerLabel(driver.map { "\($0.acronym) · \($0.team)" } ?? strings.driver, color: colors[slot]) }
            Menu {
                ForEach(driverLaps.prefix(30)) { l in
                    Button(strings.lapOption(l.lapNumber, LapTimeFormat.string(l.duration), best: l == driverLaps.first)) {
                        Task { await model.select(lap: l, slot: slot) }
                    }
                }
            } label: { pickerLabel(lap.map { strings.lapShort($0.lapNumber) + " · \(LapTimeFormat.string($0.duration))" } ?? strings.lapLabel, color: colors[slot]) }
        }
    }

    private func pickerLabel(_ text: String, color: Color?) -> some View {
        HStack {
            if let color { RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 4, height: 18) }
            Text(text).font(.subheadline.weight(.semibold)).lineLimit(1)
            Spacer()
            Image(systemName: "chevron.up.chevron.down").font(.caption).foregroundStyle(ApexColor.telemetry)
        }
        .foregroundStyle(ApexColor.floodlight)
        .padding(10)
        .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 12))
    }

    @ViewBuilder
    private func charts(_ c: LapComparison) -> some View {
        SectionHeader(title: strings.lapTimes)
        sectorTable

        SectionHeader(title: strings.readout)
        readout(c).padding(.horizontal, 16)

        Card {
            Text(strings.speedChart).font(.caption).foregroundStyle(ApexColor.telemetry)
            trace(c, a: c.speedA, b: c.speedB, domain: 50...360, height: 170)
            HStack(spacing: 14) {
                legendItem(codeA, colors[0])
                legendItem(codeB, colors[1])
            }
        }
        .padding(.vertical, 6)

        Card {
            Text(strings.deltaChart(codeA)).font(.caption).foregroundStyle(ApexColor.telemetry)
            let bound = max(0.2, ((c.delta.map(abs).max() ?? 0) * 10).rounded(.up) / 10)
            Chart {
                RuleMark(y: .value("Zéro", 0)).foregroundStyle(ApexColor.telemetry)
                ForEach(Array(zip(c.distance, c.delta)), id: \.0) { d, v in
                    LineMark(x: .value("Distance", d), y: .value("Écart", v))
                        .foregroundStyle(ApexColor.floodlight)
                        .lineStyle(StrokeStyle(lineWidth: 2))
                }
                selectionRule
            }
            .chartYScale(domain: -bound...bound)
            .chartXSelection(value: $selectedDistance)
            .chartXAxis(.hidden)
            .frame(height: 140)
        }
        .padding(.vertical, 6)

        Card {
            Text(strings.throttleChart).font(.caption).foregroundStyle(ApexColor.telemetry)
            trace(c, a: c.throttleA, b: c.throttleB, domain: 0...100, height: 110)
        }
        .padding(.vertical, 6)

        Card {
            Text(strings.brakeChart).font(.caption).foregroundStyle(ApexColor.telemetry)
            // B is drawn slightly lower so overlapping brake zones stay distinguishable.
            trace(c, a: c.brakeA, b: c.brakeB.map { $0 * 0.85 }, domain: 0...100, height: 80)
        }
        .padding(.vertical, 6)

        SectionHeader(title: strings.miniSectors)
        Card {
            DominanceMap(points: model.track, winners: c.miniSectorWinners, colors: colors,
                         selectedFraction: selectedDistance.map { $0 / (c.distance.last ?? 1) })
                .frame(height: 220)
            let winsA = c.miniSectorWinners.filter { $0 == 0 }.count
            HStack(spacing: 14) {
                legendItem(strings.miniSectorsWon(codeA, winsA), colors[0])
                legendItem(strings.miniSectorsWon(codeB, c.miniSectorWinners.count - winsA), colors[1])
            }
        }
        Text(strings.telemetryNote)
            .font(.caption).foregroundStyle(ApexColor.telemetry).padding(16)
    }

    private func trace(_ c: LapComparison, a: [Double], b: [Double], domain: ClosedRange<Double>, height: CGFloat) -> some View {
        Chart {
            ForEach(Array(zip(c.distance, a)), id: \.0) { d, v in
                LineMark(x: .value("Distance", d), y: .value("Valeur", v), series: .value("Pilote", codeA))
                    .foregroundStyle(colors[0]).lineStyle(StrokeStyle(lineWidth: 2))
            }
            ForEach(Array(zip(c.distance, b)), id: \.0) { d, v in
                LineMark(x: .value("Distance", d), y: .value("Valeur", v), series: .value("Pilote", codeB))
                    .foregroundStyle(colors[1]).lineStyle(StrokeStyle(lineWidth: 2))
            }
            selectionRule
        }
        .chartYScale(domain: domain)
        .chartXSelection(value: $selectedDistance)
        .chartXAxis(.hidden)
        .chartLegend(.hidden)
        .frame(height: height)
    }

    @ChartContentBuilder
    private var selectionRule: some ChartContent {
        if let selectedDistance {
            RuleMark(x: .value("Distance", selectedDistance)).foregroundStyle(ApexColor.floodlight.opacity(0.6))
        }
    }

    @ViewBuilder
    private func readout(_ c: LapComparison) -> some View {
        if let d = selectedDistance, let i = c.distance.firstIndex(where: { $0 >= d }) ?? c.distance.indices.last {
            HStack {
                Text("\(Int(c.distance[i])) m").font(.subheadline.weight(.semibold))
                Spacer()
                legendItem("\(codeA) \(Int(c.speedA[i])) km/h", colors[0])
                legendItem("\(codeB) \(Int(c.speedB[i])) km/h", colors[1])
                Spacer()
                Text(String(format: "Δ %+.3f s", c.delta[i])).font(.subheadline.monospacedDigit())
            }
        } else {
            Text(strings.chartTouchHint)
                .font(.caption).foregroundStyle(ApexColor.telemetry)
        }
    }

    private struct SectorRow: Identifiable {
        let label: String
        let mine: [Double?]
        let other: [Double?]
        let color: Color
        var id: String { label }
    }

    private var sectorRows: [SectorRow] {
        guard let a = model.lapA, let b = model.lapB else { return [] }
        let timesA: [Double?] = a.sectors + [a.duration]
        let timesB: [Double?] = b.sectors + [b.duration]
        return [
            SectorRow(label: "\(codeA) " + strings.lapShort(a.lapNumber), mine: timesA, other: timesB, color: colors[0]),
            SectorRow(label: "\(codeB) " + strings.lapShort(b.lapNumber), mine: timesB, other: timesA, color: colors[1]),
        ]
    }

    private var sectorTable: some View {
        Grid(alignment: .leading, horizontalSpacing: 10, verticalSpacing: 6) {
            GridRow {
                Text("")
                ForEach(["S1", "S2", "S3", strings.lapColumn], id: \.self) { Text($0).font(ApexFont.label).foregroundStyle(ApexColor.telemetry) }
            }
            ForEach(sectorRows) { row in
                GridRow {
                    legendItem(row.label, row.color)
                    ForEach(0..<4, id: \.self) { i in sectorCell(row, index: i) }
                }
            }
        }
        .padding(.horizontal, 16)
    }

    private func sectorCell(_ row: SectorRow, index i: Int) -> some View {
        let value: Double? = row.mine[i]
        let rival: Double? = row.other[i]
        var better = false
        if let value, let rival { better = value < rival }
        let text: String
        if let value { text = i == 3 ? LapTimeFormat.string(value) : String(format: "%.3f", value) } else { text = "—" }
        return Text(text)
            .font(.caption.monospacedDigit().weight(better ? .black : .regular))
            .foregroundStyle(better ? ApexColor.floodlight : ApexColor.telemetry)
    }

    private func legendItem(_ text: String, _ color: Color) -> some View {
        HStack(spacing: 4) {
            RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 10, height: 10)
            Text(text).font(.caption)
        }
    }
}

/// Track outline from driver A's location samples, coloured by who was quicker in each mini-sector.
struct DominanceMap: View {
    @Environment(\.strings) private var strings
    let points: [Telemetry.MappedPoint]
    let winners: [Int]
    let colors: [Color]
    let selectedFraction: Double?

    var body: some View {
        Canvas { context, size in
            guard points.count > 10, !winners.isEmpty,
                  let minX = points.map(\.x).min(), let maxX = points.map(\.x).max(),
                  let minY = points.map(\.y).min(), let maxY = points.map(\.y).max() else { return }
            let pad = 12.0
            let scale = min((size.width - 2 * pad) / (maxX - minX), (size.height - 2 * pad) / (maxY - minY))
            let offsetX = (size.width - (maxX - minX) * scale) / 2
            let offsetY = (size.height - (maxY - minY) * scale) / 2
            // Track y grows upwards; screen y grows downwards.
            func at(_ p: Telemetry.MappedPoint) -> CGPoint {
                CGPoint(x: offsetX + (p.x - minX) * scale, y: size.height - offsetY - (p.y - minY) * scale)
            }
            for i in 1..<points.count {
                let segment = min(winners.count - 1, Int(points[i].fraction * Double(winners.count)))
                var path = Path()
                path.move(to: at(points[i - 1]))
                path.addLine(to: at(points[i]))
                context.stroke(path, with: .color(colors[winners[segment]]), style: StrokeStyle(lineWidth: 6, lineCap: .round))
            }
            let start = at(points[0])
            context.fill(Path(ellipseIn: CGRect(x: start.x - 5, y: start.y - 5, width: 10, height: 10)), with: .color(ApexColor.floodlight))
            if let f = selectedFraction, let p = points.min(by: { abs($0.fraction - f) < abs($1.fraction - f) }) {
                let c = at(p)
                context.fill(Path(ellipseIn: CGRect(x: c.x - 8, y: c.y - 8, width: 16, height: 16)), with: .color(ApexColor.carbon))
                context.fill(Path(ellipseIn: CGRect(x: c.x - 5, y: c.y - 5, width: 10, height: 10)), with: .color(ApexColor.floodlight))
            }
        }
        .accessibilityLabel(strings.trackMapDescription)
    }
}
