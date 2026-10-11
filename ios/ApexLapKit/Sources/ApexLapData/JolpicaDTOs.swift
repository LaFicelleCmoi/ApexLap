import Foundation

// Jolpica keeps the Ergast response format: every numeric value is a string.

struct JolpicaResponse<T: Decodable>: Decodable {
    let data: T
    enum CodingKeys: String, CodingKey { case data = "MRData" }
}

struct RaceTableData: Decodable {
    let total: String
    let raceTable: RaceTableDTO
    enum CodingKeys: String, CodingKey { case total, raceTable = "RaceTable" }
}

struct RaceTableDTO: Decodable {
    let races: [RaceDTO]
    enum CodingKeys: String, CodingKey { case races = "Races" }
}

struct StandingsData: Decodable {
    let standingsTable: StandingsTableDTO
    enum CodingKeys: String, CodingKey { case standingsTable = "StandingsTable" }
}

struct StandingsTableDTO: Decodable {
    let lists: [StandingsListDTO]
    enum CodingKeys: String, CodingKey { case lists = "StandingsLists" }
}

struct StandingsListDTO: Decodable {
    let driverStandings: [DriverStandingDTO]?
    let constructorStandings: [ConstructorStandingDTO]?
    enum CodingKeys: String, CodingKey {
        case driverStandings = "DriverStandings"
        case constructorStandings = "ConstructorStandings"
    }
}

struct DriverTableData: Decodable {
    let driverTable: DriverTableDTO
    enum CodingKeys: String, CodingKey { case driverTable = "DriverTable" }
}

struct DriverTableDTO: Decodable {
    let drivers: [DriverDTO]
    enum CodingKeys: String, CodingKey { case drivers = "Drivers" }
}

struct ConstructorTableData: Decodable {
    let constructorTable: ConstructorTableDTO
    enum CodingKeys: String, CodingKey { case constructorTable = "ConstructorTable" }
}

struct ConstructorTableDTO: Decodable {
    let constructors: [ConstructorDTO]
    enum CodingKeys: String, CodingKey { case constructors = "Constructors" }
}

/// Any MRData payload; used when only the `total` count matters (career statistics).
struct TotalData: Decodable {
    let total: String
}

struct SessionDTO: Decodable {
    let date: String
    let time: String?
}

struct RaceDTO: Decodable {
    let season: String
    let round: String
    let raceName: String
    let circuit: CircuitDTO
    let date: String
    let time: String?
    let firstPractice: SessionDTO?
    let secondPractice: SessionDTO?
    let thirdPractice: SessionDTO?
    let sprintQualifying: SessionDTO?
    let sprintShootout: SessionDTO?
    let sprint: SessionDTO?
    let qualifying: SessionDTO?
    let results: [ResultDTO]?
    let sprintResults: [ResultDTO]?
    let qualifyingResults: [QualifyingDTO]?

    enum CodingKeys: String, CodingKey {
        case season, round, raceName, date, time
        case circuit = "Circuit"
        case firstPractice = "FirstPractice"
        case secondPractice = "SecondPractice"
        case thirdPractice = "ThirdPractice"
        case sprintQualifying = "SprintQualifying"
        case sprintShootout = "SprintShootout"
        case sprint = "Sprint"
        case qualifying = "Qualifying"
        case results = "Results"
        case sprintResults = "SprintResults"
        case qualifyingResults = "QualifyingResults"
    }
}

struct QualifyingDTO: Decodable {
    let position: String
    let driver: DriverDTO
    let constructor: ConstructorDTO
    let q1: String?
    let q2: String?
    let q3: String?
    enum CodingKeys: String, CodingKey {
        case position
        case driver = "Driver"
        case constructor = "Constructor"
        case q1 = "Q1", q2 = "Q2", q3 = "Q3"
    }
}

struct CircuitDTO: Decodable {
    let circuitId: String
    let circuitName: String
    let location: LocationDTO
    enum CodingKeys: String, CodingKey { case circuitId, circuitName, location = "Location" }
}

struct LocationDTO: Decodable {
    let locality: String
    let country: String
}

struct DriverDTO: Decodable {
    let driverId: String
    let permanentNumber: String?
    let code: String?
    let givenName: String
    let familyName: String
    let dateOfBirth: String?
    let nationality: String
}

struct ConstructorDTO: Decodable {
    let constructorId: String
    let name: String
    let nationality: String
}

struct DriverStandingDTO: Decodable {
    let position: String?
    let points: String
    let wins: String
    let driver: DriverDTO
    let constructors: [ConstructorDTO]?
    enum CodingKeys: String, CodingKey {
        case position, points, wins
        case driver = "Driver"
        case constructors = "Constructors"
    }
}

struct ConstructorStandingDTO: Decodable {
    let position: String?
    let points: String
    let wins: String
    let constructor: ConstructorDTO
    enum CodingKeys: String, CodingKey { case position, points, wins, constructor = "Constructor" }
}

struct ResultDTO: Decodable {
    let position: String
    let positionText: String
    let points: String
    let grid: String?
    let laps: String?
    let status: String
    let driver: DriverDTO
    let constructor: ConstructorDTO
    let time: ResultTimeDTO?
    let fastestLap: FastestLapDTO?
    enum CodingKeys: String, CodingKey {
        case position, positionText, points, grid, laps, status
        case driver = "Driver"
        case constructor = "Constructor"
        case time = "Time"
        case fastestLap = "FastestLap"
    }
}

struct ResultTimeDTO: Decodable {
    let time: String
}

struct FastestLapDTO: Decodable {
    let rank: String?
    let time: ResultTimeDTO?
    enum CodingKeys: String, CodingKey { case rank, time = "Time" }
}
