@testable import ApexLapData
import ApexLapDomain
import Foundation
import Testing

@Suite("Season statistics, weekend results and reminders")
struct SeasonAndRemindersTests {
    let mercedes = Constructor(id: "mercedes", name: "Mercedes", nationality: "German")
    let ferrari = Constructor(id: "ferrari", name: "Ferrari", nationality: "Italian")
    let ant = Driver(id: "antonelli", code: "ANT", number: "12", givenName: "Kimi", familyName: "Antonelli", nationality: "Italian", dateOfBirth: nil)
    let lec = Driver(id: "leclerc", code: "LEC", number: "16", givenName: "Charles", familyName: "Leclerc", nationality: "Monegasque", dateOfBirth: nil)

    func result(_ round: Int, _ driver: Driver, _ position: Int, _ points: Double, grid: Int? = nil, sprint: Bool = false,
                text: String? = nil, fastest: Bool = false) -> ScoredResult {
        ScoredResult(round: round, isSprint: sprint, result: RaceResult(
            position: position, positionText: text ?? "\(position)", points: points, grid: grid ?? position, laps: 50,
            status: "Finished", time: nil, fastestLapRank: fastest ? 1 : 5, fastestLapTime: nil, driver: driver,
            constructor: driver == ant ? mercedes : ferrari))
    }

    // Same scenario as Android's SeasonStatsTest, so both platforms are held to identical numbers.
    var season: [ScoredResult] {
        [
            result(1, ant, 1, 25, grid: 2, fastest: true), result(1, lec, 2, 18, grid: 1),
            result(2, ant, 1, 8, sprint: true), result(2, lec, 2, 7, sprint: true),
            result(2, ant, 15, 0, grid: 3, text: "R"), result(2, lec, 1, 25, grid: 5),
            result(3, ant, 3, 15, grid: 1), result(3, lec, 4, 12, grid: 2),
        ]
    }

    @Test func progressionAccumulatesRaceAndSprintPoints() {
        let progression = SeasonStats.progression(season)
        #expect(progression.rounds == [1, 2, 3])
        #expect(progression.drivers.map(\.driver.id) == ["leclerc", "antonelli"])
        #expect(progression.drivers[0].cumulativePoints == [18, 50, 62])
        #expect(progression.drivers[1].cumulativePoints == [25, 33, 48])
    }

    @Test func leadersCountGrandsPrixOnly() {
        let leaders = Dictionary(uniqueKeysWithValues: SeasonStats.leaders(season).map { ($0.category, $0) })
        #expect(leaders[.wins]?.entries.map(\.value) == [1, 1])
        #expect(leaders[.fastestLaps]?.entries.map(\.driver.id) == ["antonelli"])
        #expect(leaders[.dnfs]?.entries.map(\.driver.id) == ["antonelli"])
        #expect(leaders[.positionsGained]?.entries.map(\.driver.id) == ["leclerc"])
    }

    @Test func headToHeadComparesSharedGrandsPrix() throws {
        let duel = try #require(SeasonStats.headToHead(season, first: "antonelli", second: "leclerc"))
        #expect(duel.sharedRaces == 3)
        #expect(duel.first.racesAhead == 2 && duel.second.racesAhead == 1)
        #expect(duel.first.startsAhead == 2 && duel.second.startsAhead == 1)
        #expect(duel.first.points == 48 && duel.first.dnfs == 1 && duel.first.averageFinish == 2)
        #expect(SeasonStats.headToHead(season, first: "antonelli", second: "antonelli") == nil)
    }

    @Test func weekendAndSeasonResultsAreFetchedAndCached() async throws {
        let http = StubHTTPClient()
        let repository = OfflineFirstF1Repository(client: JolpicaClient(http: http), store: InMemoryF1Store())
        try await repository.refreshRaceWeekend(season: 2026, round: 17, includeSprint: true)
        #expect(await repository.cachedQualifying(season: 2026, round: 17).map(\.driver.id) == ["max_verstappen", "leclerc", "hamilton"])
        #expect(await repository.cachedQualifying(season: 2026, round: 17).first?.bestTime == "1:31.373")
        #expect(await repository.cachedSprintResults(season: 2026, round: 17).count == 3)

        let season = try await repository.refreshSeasonResults(season: 2026)
        #expect(http.requests.filter { $0.path.hasSuffix("2026/results.json") }.count == 2)
        #expect(season.filter { !$0.isSprint }.count == 3)
        #expect(season.filter(\.isSprint).count == 3)
    }

    @Test func remindersSkipPracticeAndPastTriggers() throws {
        let f = ISO8601DateFormatter()
        func session(_ type: SessionType, _ iso: String) -> Session { Session(type: type, startsAt: f.date(from: iso), day: f.date(from: iso)!) }
        let austin = Race(season: 2026, round: 18, name: "United States Grand Prix",
                          circuit: Circuit(id: "cota", name: "COTA", locality: "Austin", country: "USA"), sessions: [
            session(.practice1, "2026-10-23T17:30:00Z"), session(.sprint, "2026-10-24T17:00:00Z"),
            session(.qualifying, "2026-10-24T21:00:00Z"), session(.race, "2026-10-25T19:00:00Z"),
        ])
        let now = try #require(f.date(from: "2026-10-20T12:00:00Z"))
        let plan = ReminderPlanner.plan([austin], now: now, settings: ReminderSettings(enabled: true, leadMinutes: 15))
        #expect(plan.map(\.session.type) == [.sprint, .qualifying, .race])
        #expect(plan.last?.fireAt == f.date(from: "2026-10-25T18:45:00Z"))
        #expect(ReminderPlanner.plan([austin], now: f.date(from: "2026-10-25T18:50:00Z")!, settings: ReminderSettings(enabled: true, scope: .racesOnly)).isEmpty)
        #expect(ReminderPlanner.plan([austin], now: now, settings: ReminderSettings()).isEmpty)
        #expect(ReminderPlanner.relativeStart(from: now, to: now.addingTimeInterval(2 * 86400 + 4 * 3600 + 59), strings: Strings(.fr)) == "dans 2 j 4 h")
        #expect(ReminderPlanner.relativeStart(from: now, to: now.addingTimeInterval(3 * 3600 + 5 * 60), strings: Strings(.fr)) == "dans 3 h 05")
        #expect(ReminderPlanner.relativeStart(from: now, to: now.addingTimeInterval(45 * 60), strings: Strings(.en)) == "in 45 min")
        #expect(ReminderPlanner.relativeStart(from: now, to: now.addingTimeInterval(2 * 86400), strings: Strings(.es)) == "en 2 d")
    }
}
