import ApexLapDomain
import Foundation
import Testing

@Suite("Calendar logic")
struct CalendarLogicTests {
    private func date(_ iso: String) -> Date { ISO8601DateFormatter().date(from: iso)! }

    private func session(_ type: SessionType, _ iso: String) -> Session {
        Session(type: type, startsAt: date(iso), day: date(iso))
    }

    private var calendar: [Race] {
        let circuit = Circuit(id: "c", name: "Circuit", locality: "City", country: "Country")
        return [
            Race(season: 2026, round: 2, name: "GP 2", circuit: circuit, sessions: [
                session(.practice1, "2026-03-13T03:30:00Z"), session(.race, "2026-03-15T07:00:00Z"),
            ]),
            Race(season: 2026, round: 1, name: "GP 1", circuit: circuit, sessions: [
                session(.qualifying, "2026-03-07T05:00:00Z"), session(.race, "2026-03-08T04:00:00Z"),
            ]),
        ]
    }

    @Test func nextSessionIsEarliestStrictlyAfterNow() {
        let next = CalendarLogic.nextSession(in: calendar, now: date("2026-03-07T06:00:00Z"))
        #expect(next?.race.round == 1)
        #expect(next?.session.type == .race)
        #expect(CalendarLogic.nextSession(in: calendar, now: date("2026-12-31T00:00:00Z")) == nil)
    }

    @Test func nextRaceStaysCurrentWhileRaceIsRunning() {
        #expect(CalendarLogic.nextRace(in: calendar, now: date("2026-03-08T05:00:00Z"))?.round == 1)
        #expect(CalendarLogic.nextRace(in: calendar, now: date("2026-03-08T07:00:00Z"))?.round == 2)
    }

    @Test func countdownSplitsDurationAndNeverGoesNegative() {
        let now = date("2026-03-06T00:00:00Z")
        let countdown = CalendarLogic.countdown(to: date("2026-03-08T04:05:06Z"), now: now)
        #expect(countdown == Countdown(days: 2, hours: 4, minutes: 5, seconds: 6))
        #expect(CalendarLogic.countdown(to: now, now: now.addingTimeInterval(60)).isOver)
    }

    @Test func currentSeasonUsesUTCYear() {
        #expect(CalendarLogic.currentSeason(now: date("2026-10-11T18:00:00Z")) == 2026)
    }
}
