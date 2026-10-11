import ApexLapData
@testable import ApexLapLive
import Foundation
import Testing

@Suite("Live timing, MQTT and telemetry")
struct LiveTests {
    let t0 = Date(timeIntervalSince1970: 1_791_000_000)
    var session: LiveSession { LiveSession(key: 1, name: "Race", type: "Race", start: t0, end: t0 + 7200, circuit: "Singapore", country: "Singapore") }
    let ver = LiveDriver(number: 3, acronym: "VER", fullName: "Max Verstappen", team: "Red Bull Racing", teamColour: 0x4781D7)
    let lec = LiveDriver(number: 16, acronym: "LEC", fullName: "Charles Leclerc", team: "Ferrari", teamColour: 0xED1131)

    func reduce(_ events: [TimingEvent]) -> LiveTimingState {
        events.reduce(LiveTimingState(session: session, drivers: [ver, lec]), LiveTimingReducer.reduce)
    }
    func lap(_ s: Double, _ driver: Int, _ n: Int, _ time: Double?) -> TimingEvent {
        .lap(date: t0 + s, driver: driver, lapNumber: n, duration: time, sectors: [30, 40, nil])
    }
    func rc(_ message: String, _ flag: String? = nil, _ scope: String? = nil) -> TimingEvent {
        .raceControl(date: t0, flag: flag, scope: scope, message: message, lap: nil)
    }

    @Test func towerOrderBestAndFastestLaps() {
        let state = reduce([
            .position(date: t0, driver: 16, position: 1), .position(date: t0, driver: 3, position: 2),
            lap(100, 16, 1, 101.2), lap(101, 3, 1, 100.8), lap(200, 16, 2, 99.9), lap(201, 3, 2, 102.0),
            .position(date: t0 + 205, driver: 3, position: 1), .position(date: t0 + 205, driver: 16, position: 2),
        ])
        #expect(state.tower.map(\.driver.acronym) == ["VER", "LEC"])
        #expect(state.timings[3]?.bestLap == 100.8)
        #expect(state.fastestLap?.driver == 16 && state.fastestLap?.time == 99.9)
        #expect(state.leaderLap == 2)
    }

    @Test func tyreAgeFollowsStints() {
        let stints: [TimingEvent] = [
            .stint(date: t0, driver: 3, stint: Stint(number: 1, compound: .medium, lapStart: 1, lapEnd: 20, tyreAgeAtStart: 0)),
            .stint(date: t0, driver: 3, stint: Stint(number: 2, compound: .hard, lapStart: 21, lapEnd: nil, tyreAgeAtStart: 2)),
        ]
        let state = reduce(stints + (1...24).map { lap(Double($0) * 100, 3, $0, 100) })
        #expect(state.timings[3]?.currentStint?.compound == .hard)
        #expect(state.timings[3]?.tyreAge == 6)
    }

    @Test func trackStatusHandlesBothWordingsAndSectorYellows() {
        #expect(reduce([rc("VSC DEPLOYED")]).trackStatus == .vsc)
        #expect(reduce([rc("VIRTUAL SAFETY CAR DEPLOYED")]).trackStatus == .vsc)
        #expect(reduce([rc("SAFETY CAR DEPLOYED"), rc("SAFETY CAR IN THIS LAP")]).trackStatus == .safetyCar)
        #expect(reduce([rc("VSC DEPLOYED"), rc("TRACK CLEAR", "CLEAR", "Track")]).trackStatus == .green)
        let yellow = reduce([rc("YELLOW IN TRACK SECTOR 5", "YELLOW", "Sector"), rc("YELLOW IN TRACK SECTOR 6", "YELLOW", "Sector"),
                             rc("CLEAR IN TRACK SECTOR 6", "CLEAR", "Sector")])
        #expect(yellow.trackStatus == .yellow)
        #expect(LiveTimingReducer.reduce(yellow, rc("CLEAR IN TRACK SECTOR 5", "CLEAR", "Sector")).trackStatus == .green)
    }

    @Test func replaySeeksBothWays() {
        let timeline = ReplayTimeline.build(session: session, drivers: [ver, lec], events: [
            .position(date: t0 - 3000, driver: 3, position: 1), .position(date: t0 - 3000, driver: 16, position: 2),
            .position(date: t0 + 600, driver: 16, position: 1), .position(date: t0 + 600, driver: 3, position: 2),
        ])
        #expect(timeline.advance(to: timeline.start).tower.map(\.driver.number) == [3, 16])
        #expect(timeline.advance(to: t0 + 700).tower.map(\.driver.number) == [16, 3])
        #expect(timeline.advance(to: t0 + 100).tower.map(\.driver.number) == [3, 16])
    }

    @Test func mqttRoundTrip() {
        #expect(MqttCodec.encodeRemainingLength(16_384) == Data([0x80, 0x80, 0x01]))
        let connect = MqttCodec.connect(clientId: "c", username: "me", password: "tok", keepAlive: 60)
        #expect(connect.first == 0x10 && Int(connect[1]) == connect.count - 2)
        var publish = Data([0x32])
        let body = Data([0, 11]) + Data("v1/position".utf8) + Data([0, 42]) + Data(String(repeating: "x", count: 300).utf8)
        publish += MqttCodec.encodeRemainingLength(body.count) + body
        var reader = MqttCodec.Reader()
        #expect(reader.feed(Data([0x20, 2, 0, 0]) + publish.prefix(10)) == [.connAck(returnCode: 0)])
        let packets = reader.feed(publish.dropFirst(10))
        guard case let .publish(topic, payload, qos, id)? = packets.first else { Issue.record("no publish"); return }
        #expect(topic == "v1/position" && payload.count == 300 && qos == 1 && id == 42)
    }

    @Test func openF1PayloadsAndDates() throws {
        #expect(OpenF1Date.parse("2026-10-11T12:03:44.511000+00:00") == ISO8601DateFormatter().date(from: "2026-10-11T12:03:44Z")! + 0.511)
        #expect(OpenF1Date.parse("2026-10-11T20:00:00+08:00") == ISO8601DateFormatter().date(from: "2026-10-11T12:00:00Z"))
        let event = OpenF1Live.parse(topic: "v1/intervals", payload: try fixtureRow("openf1_intervals", index: 2), now: t0)
        guard case let .interval(_, driver, gap, _)? = event else { Issue.record("no interval"); return }
        #expect(driver == 77 && gap == .text("+1 LAP"))
        #expect(OpenF1Live.parse(topic: "v1/location", payload: Data("{}".utf8), now: t0) == nil)
    }

    @Test func telemetryIntegratesDistanceAndDelta() throws {
        func trace(_ driver: Int, _ kmh: (Double) -> Double, _ seconds: Double) throws -> LapTrace {
            let lap = LapRef(driver: driver, lapNumber: 1, start: t0, duration: seconds, sectors: [nil, nil, nil])
            let samples = stride(from: 0.0, through: seconds, by: 0.25).map {
                CarSample(date: t0 + $0, speed: kmh($0), throttle: 100, brake: 0, gear: 7)
            }
            return try #require(LapTrace.build(lap: lap, samples: samples))
        }
        let a = try trace(1, { _ in 180 }, 20)
        #expect(abs(a.length - 1000) < 1e-6)
        let same = Telemetry.compare(a, try trace(2, { _ in 180 }, 20))
        #expect(same.delta.allSatisfy { abs($0) < 1e-9 })
        let slower = Telemetry.compare(a, try trace(2, { $0 < 10 ? 180 : 150 }, 22), miniSectors: 4)
        #expect(slower.delta.last! > 1.5)
        #expect(slower.miniSectorWinners.last == 0)
    }
}

func fixtureRow(_ name: String, index: Int) throws -> Data {
    let rows = try JSONSerialization.jsonObject(with: fixture(name)) as! [Any]
    return try JSONSerialization.data(withJSONObject: rows[index])
}
