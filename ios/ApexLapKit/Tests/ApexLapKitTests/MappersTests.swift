@testable import ApexLapData
import ApexLapDomain
import Foundation
import Testing

@Suite("Jolpica mapping")
struct MappersTests {
    let calendar: [Race]

    init() throws {
        let response = try JSONDecoder().decode(JolpicaResponse<RaceTableData>.self, from: fixture("calendar_2026"))
        calendar = response.data.raceTable.races.map { $0.toDomain() }
    }

    @Test func classicWeekendHasFiveSessionsInOrder() throws {
        let australia = try #require(calendar.first)
        #expect(australia.name == "Australian Grand Prix")
        #expect(australia.sessions.map(\.type) == [.practice1, .practice2, .practice3, .qualifying, .race])
        #expect(australia.raceSession?.startsAt == ISO8601DateFormatter().date(from: "2026-03-08T04:00:00Z"))
        #expect(!australia.isSprintWeekend)
    }

    @Test func sprintWeekendOrdersSprintQualifyingBeforeSprint() {
        let china = calendar[1]
        #expect(china.isSprintWeekend)
        #expect(china.sessions.map(\.type) == [.practice1, .sprintQualifying, .sprint, .qualifying, .race])
    }

    @Test func raceResultsMapFastestLapAndGridDelta() throws {
        let response = try JSONDecoder().decode(JolpicaResponse<RaceTableData>.self, from: fixture("results_singapore_2026"))
        let results = try #require(response.data.raceTable.races.first?.results).map { $0.toDomain() }
        #expect(results[0].driver.id == "max_verstappen")
        #expect(results[0].points == 25)
        #expect(results[0].time == "1:42:00.785")
        #expect(!results[0].hasFastestLap)
        #expect(results[1].hasFastestLap)
        #expect(results[2].positionsGained == 1)
    }

    @Test func timesWithoutOffsetAreUTC() throws {
        let day = try #require(JolpicaDate.day("1990-03-11"))
        #expect(JolpicaDate.instant(day: day, time: "14:00:00") == ISO8601DateFormatter().date(from: "1990-03-11T14:00:00Z"))
        #expect(JolpicaDate.instant(day: day, time: nil) == nil)
        #expect(JolpicaDate.instant(day: day, time: "nope") == nil)
    }
}
