@testable import ApexLapData
import ApexLapDomain
import Foundation
import Testing

@Suite("Encyclopedia, languages, retirements and widget (parity with Android)")
struct EncyclopediaAndI18nTests {
    let mercedes = Constructor(id: "mercedes", name: "Mercedes", nationality: "German")
    let ant = Driver(id: "antonelli", code: "ANT", number: "12", givenName: "Kimi", familyName: "Antonelli", nationality: "Italian", dateOfBirth: nil)

    func race(_ status: String, text: String, position: Int = 14) -> RaceResult {
        RaceResult(position: position, positionText: text, points: 0, grid: 5, laps: 50, status: status, time: nil,
                   fastestLapRank: nil, fastestLapTime: nil, driver: ant, constructor: mercedes)
    }

    @Test func classifiedRetirementsCountAsDNFs() {
        #expect(race("Collision", text: "14").isRetirement)
        #expect(race("Retired", text: "R").isRetirement)
        #expect(race("Retired", text: "N").isRetirement)
        #expect(!race("Finished", text: "3").isRetirement)
        #expect(!race("+1 Lap", text: "12").isRetirement)
        #expect(!race("Lapped", text: "11").isRetirement)
        #expect(!race("Did not start", text: "W").isRetirement)
        #expect(!race("Disqualified", text: "D").isRetirement)
    }

    @Test func retirementRecordTotals73() throws {
        let record = try #require(SeasonRetirementsRecord.of(2026))
        #expect(record.total == 73)
        #expect(record.drivers.first?.driver == "Stroll")
        #expect(record.drivers.first?.count == 12)
        #expect(SeasonRetirementsRecord.of(2025) == nil)
        let parsed = SeasonRetirementsRecord.parse("Australie (NC), Chine, Azerbaïdjan*")
        #expect(parsed == [Retirement("Australie", .notClassified), Retirement("Chine"), Retirement("Azerbaïdjan", .classified)])
        #expect(parsed[2].label(.en) == "Azerbaijan*")
        #expect(parsed[0].label(.es) == "Australia (NC)")
    }

    @Test func languagesResolveAndTranslate() {
        #expect(AppLanguage.fromSystem(["de-DE", "es-MX"]) == .es)
        #expect(AppLanguage.fromSystem(["de-DE"]) == .en)
        #expect(LanguagePreference.fr.resolve(["en-US"]) == .fr)
        #expect(Strings(.fr).session(.race) == "Course")
        #expect(Strings(.en).session(.race) == "Race")
        #expect(Strings(.es).session(.race) == "Carrera")
        #expect(Strings(.en).ordinal(22) == "22nd")
        #expect(Strings(.en).ordinal(11) == "11th")
        #expect(Strings(.fr).compactDuration(minutes: 2 * 1440 + 4 * 60 + 12) == "2 j 4 h")
        #expect(Strings(.en).compactDuration(minutes: 185) == "3 h 05")
    }

    @Test func contentExistsInEveryLanguage() throws {
        #expect(Glossary.terms.count == 36)
        for language in AppLanguage.allCases {
            #expect(Set(Glossary.terms.map { $0.term[language] }).count == Glossary.terms.count)
            #expect(Glossary.terms.allSatisfy { $0.definition[language].count > 20 })
        }
        let spa = try #require(CircuitGuides.of("spa"))
        #expect(spa.corners.contains { $0.name.en.contains("Eau Rouge") })
        #expect(CircuitGuides.of("unknown") == nil)
    }

    @Test func bundledChampionsCover1950To2025() async {
        let registry = await EncyclopediaRepository(client: JolpicaClient(http: StubHTTPClient()), directory: nil).champions()
        #expect(registry.champions.count == 76)
        #expect(registry.champions.first?.season == 2025)
        #expect(registry.driverTitles("hamilton").count == 7)
        #expect(registry.constructorTitles("ferrari").count == 16)
        #expect(registry.season(1950)?.constructor == nil)
    }

    @Test func driverBioIsWrittenInEachLanguage() throws {
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        let birth = try #require(utc.date(from: DateComponents(year: 1997, month: 9, day: 30)))
        let today = try #require(utc.date(from: DateComponents(year: 2026, month: 10, day: 12)))
        let driver = Driver(id: "x", code: "XXX", number: "1", givenName: "Max", familyName: "Test", nationality: "Dutch", dateOfBirth: birth)
        let profile = DriverProfile(driver: driver, stats: CareerStats(seasons: 12, races: 230, wins: 70, podiums: 120, poles: 45))
        let titles = Titles(count: 4, seasons: [2021, 2022, 2023, 2024])
        let fr = Bios.driver(profile, titles: titles, standing: nil, season: 2026, today: today, language: .fr)
        #expect(fr.contains("(29 ans)"))
        #expect(fr.contains("4 fois champion du monde (2021–2024)"))
        #expect(fr.contains("230 Grands Prix en 12 saisons : 70 victoires"))
        let en = Bios.driver(profile, titles: titles, standing: nil, season: 2026, today: today, language: .en)
        #expect(en.contains("230 Grands Prix over 12 seasons: 70 wins"))
        #expect(Bios.formatSeasons([2020, 2008, 2014, 2015, 2017, 2018, 2019]) == "2008, 2014–2015, 2017–2020")
    }

    @Test func circuitHistoryGivesTheGrandPrixContext() async throws {
        let repository = EncyclopediaRepository(client: JolpicaClient(http: StubHTTPClient()), directory: nil)
        let history = try await repository.circuitHistory("marina_bay")
        #expect(history.grandsPrix == 1)
        #expect(history.context(for: 2026).edition == 1)
        #expect(history.context(for: 2027).previousWinner?.season == 2026)
        #expect(LapTime.milliseconds("1:21.046") == 81_046)
        #expect(LapTime.milliseconds("58.123") == 58_123)
        #expect(LapTime.milliseconds("DNF") == nil)
    }

    @Test func widgetFollowsTheWeekend() throws {
        let f = ISO8601DateFormatter()
        func session(_ type: SessionType, _ at: String) -> Session {
            let date = f.date(from: at)!
            return Session(type: type, startsAt: date, day: date)
        }
        let singapore = Race(season: 2026, round: 18, name: "Singapore Grand Prix",
                             circuit: Circuit(id: "marina_bay", name: "Marina Bay", locality: "Singapore", country: "Singapore"),
                             sessions: [session(.practice3, "2026-10-10T09:30:00Z"), session(.qualifying, "2026-10-10T13:00:00Z"),
                                        session(.race, "2026-10-11T12:00:00Z")])
        let austin = Race(season: 2026, round: 19, name: "United States Grand Prix",
                          circuit: Circuit(id: "americas", name: "COTA", locality: "Austin", country: "USA"),
                          sessions: [session(.practice1, "2026-10-23T17:30:00Z")])
        let live = WidgetSchedule.snapshot([austin, singapore], at: try #require(f.date(from: "2026-10-10T13:20:00Z")))
        #expect(live.race?.round == 18)
        #expect(live.live?.type == .qualifying)
        #expect(live.sessions.map(\.phase) == [.done, .live, .next])
        let after = WidgetSchedule.snapshot([austin, singapore], at: try #require(f.date(from: "2026-10-12T08:00:00Z")))
        #expect(after.race?.round == 19)
        let over = WidgetSchedule.snapshot([austin, singapore], at: try #require(f.date(from: "2026-12-01T00:00:00Z")))
        #expect(over.race == nil && over.hasCalendar)
        let changes = WidgetSchedule.timeline([singapore], from: try #require(f.date(from: "2026-10-10T12:00:00Z")))
        #expect(changes.count == 4)
    }
}
