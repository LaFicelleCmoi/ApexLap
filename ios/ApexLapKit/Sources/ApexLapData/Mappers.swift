import ApexLapDomain
import Foundation

enum JolpicaDate {
    private static let utcCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }()

    /// Parses `yyyy-MM-dd` as midnight UTC.
    static func day(_ value: String) -> Date? {
        let parts = value.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return utcCalendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    /// Ergast times look like `05:00:00Z`; some historical entries omit the `Z`. Both are UTC.
    static func instant(day: Date, time: String?) -> Date? {
        guard let time, !time.isEmpty else { return nil }
        let clock = time.hasSuffix("Z") ? String(time.dropLast()) : time
        let parts = clock.split(separator: ":").compactMap { Int($0) }
        guard parts.count >= 2, parts.count <= 3 else { return nil }
        let seconds = parts[0] * 3_600 + parts[1] * 60 + (parts.count == 3 ? parts[2] : 0)
        return day.addingTimeInterval(TimeInterval(seconds))
    }
}

extension RaceDTO {
    func toDomain() -> Race {
        let candidates: [(SessionDTO?, SessionType)] = [
            (firstPractice, .practice1),
            (secondPractice, .practice2),
            (thirdPractice, .practice3),
            (sprintQualifying ?? sprintShootout, .sprintQualifying),
            (sprint, .sprint),
            (qualifying, .qualifying),
            (SessionDTO(date: date, time: time), .race),
        ]
        let sessions = candidates
            .compactMap { dto, type in dto?.toSession(type) }
            .sorted { ($0.startsAt ?? $0.day) < ($1.startsAt ?? $1.day) }
        return Race(
            season: Int(season) ?? 0,
            round: Int(round) ?? 0,
            name: raceName,
            circuit: Circuit(id: circuit.circuitId, name: circuit.circuitName,
                             locality: circuit.location.locality, country: circuit.location.country),
            sessions: sessions
        )
    }
}

extension SessionDTO {
    func toSession(_ type: SessionType) -> Session? {
        guard let day = JolpicaDate.day(date) else { return nil }
        return Session(type: type, startsAt: JolpicaDate.instant(day: day, time: time), day: day)
    }
}

extension DriverDTO {
    func toDomain() -> Driver {
        Driver(
            id: driverId,
            code: code ?? String(familyName.prefix(3)).uppercased(),
            number: permanentNumber,
            givenName: givenName,
            familyName: familyName,
            nationality: nationality,
            dateOfBirth: dateOfBirth.flatMap(JolpicaDate.day)
        )
    }
}

extension ConstructorDTO {
    func toDomain() -> Constructor {
        Constructor(id: constructorId, name: name, nationality: nationality)
    }
}

extension DriverStandingDTO {
    func toDomain(fallbackPosition: Int) -> DriverStanding {
        DriverStanding(
            position: position.flatMap { Int($0) } ?? fallbackPosition,
            points: Double(points) ?? 0,
            wins: Int(wins) ?? 0,
            driver: driver.toDomain(),
            constructor: constructors?.last?.toDomain()
        )
    }
}

extension ConstructorStandingDTO {
    func toDomain(fallbackPosition: Int) -> ConstructorStanding {
        ConstructorStanding(
            position: position.flatMap { Int($0) } ?? fallbackPosition,
            points: Double(points) ?? 0,
            wins: Int(wins) ?? 0,
            constructor: constructor.toDomain()
        )
    }
}

extension ResultDTO {
    func toDomain() -> RaceResult {
        RaceResult(
            position: Int(position) ?? 0,
            positionText: positionText,
            points: Double(points) ?? 0,
            grid: grid.flatMap { Int($0) } ?? 0,
            laps: laps.flatMap { Int($0) } ?? 0,
            status: status,
            time: time?.time,
            fastestLapRank: fastestLap?.rank.flatMap { Int($0) },
            fastestLapTime: fastestLap?.time?.time,
            driver: driver.toDomain(),
            constructor: constructor.toDomain()
        )
    }
}

extension QualifyingDTO {
    func toDomain() -> QualifyingResult {
        func clean(_ value: String?) -> String? { value?.isEmpty == false ? value : nil }
        return QualifyingResult(position: Int(position) ?? 0, driver: driver.toDomain(), constructor: constructor.toDomain(),
                                q1: clean(q1), q2: clean(q2), q3: clean(q3))
    }
}
