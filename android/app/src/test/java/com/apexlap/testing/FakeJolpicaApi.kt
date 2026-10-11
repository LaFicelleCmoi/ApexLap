package com.apexlap.testing

import com.apexlap.data.remote.JolpicaApi
import com.apexlap.data.remote.dto.ConstructorDto
import com.apexlap.data.remote.dto.ConstructorTableData
import com.apexlap.data.remote.dto.ConstructorTableDto
import com.apexlap.data.remote.dto.DriverTableData
import com.apexlap.data.remote.dto.JolpicaResponse
import com.apexlap.data.remote.dto.RaceTableData
import com.apexlap.data.remote.dto.StandingsData
import com.apexlap.data.remote.dto.StandingsTableDto
import com.apexlap.data.remote.dto.TotalData
import java.io.IOException

/** Serves the recorded Jolpica fixtures; flip [offline] to simulate a network failure. */
class FakeJolpicaApi : JolpicaApi {
    var offline = false
    val countRequests = mutableListOf<String>()
    val pageRequests = mutableListOf<String>()
    /** `total` reported by the paginated season endpoints, to exercise pagination. */
    var seasonTotal = 3
    var counts: Map<String, Int> = emptyMap()

    private fun checkOnline() {
        if (offline) throw IOException("offline")
    }

    override suspend fun calendar(season: Int, limit: Int): JolpicaResponse<RaceTableData> {
        checkOnline()
        return decodeFixture("calendar_2026.json")
    }

    override suspend fun driverStandings(season: Int, limit: Int): JolpicaResponse<StandingsData> {
        checkOnline()
        return decodeFixture("driver_standings_2026.json")
    }

    override suspend fun constructorStandings(season: Int, limit: Int): JolpicaResponse<StandingsData> {
        checkOnline()
        return JolpicaResponse(StandingsData(StandingsTableDto()))
    }

    override suspend fun raceResults(season: Int, round: Int, limit: Int): JolpicaResponse<RaceTableData> {
        checkOnline()
        return decodeFixture("results_singapore_2026.json")
    }

    override suspend fun sprintResults(season: Int, round: Int, limit: Int): JolpicaResponse<RaceTableData> {
        checkOnline()
        return asSprint(decodeFixture("results_singapore_2026.json"))
    }

    override suspend fun qualifying(season: Int, round: Int, limit: Int): JolpicaResponse<RaceTableData> {
        checkOnline()
        return decodeFixture("qualifying_singapore_2026.json")
    }

    override suspend fun seasonResults(season: Int, offset: Int, limit: Int): JolpicaResponse<RaceTableData> {
        checkOnline()
        pageRequests += "results@$offset"
        val page: JolpicaResponse<RaceTableData> = decodeFixture("results_singapore_2026.json")
        return page.copy(data = page.data.copy(total = seasonTotal.toString()))
    }

    override suspend fun seasonSprintResults(season: Int, offset: Int, limit: Int): JolpicaResponse<RaceTableData> {
        checkOnline()
        pageRequests += "sprint@$offset"
        return asSprint(decodeFixture("results_singapore_2026.json"))
    }

    /** Re-labels the race fixture as sprint results (sprints share the result format). */
    private fun asSprint(response: JolpicaResponse<RaceTableData>): JolpicaResponse<RaceTableData> {
        val races = response.data.raceTable.races.map { it.copy(results = emptyList(), sprintResults = it.results) }
        return response.copy(data = response.data.copy(raceTable = response.data.raceTable.copy(races = races)))
    }

    override suspend fun driver(driverId: String): JolpicaResponse<DriverTableData> {
        checkOnline()
        val standings: JolpicaResponse<StandingsData> = decodeFixture("driver_standings_2026.json")
        val dto = standings.data.standingsTable.lists.first().driverStandings.first { it.driver.driverId == driverId }.driver
        return JolpicaResponse(DriverTableData(com.apexlap.data.remote.dto.DriverTableDto(listOf(dto))))
    }

    override suspend fun constructor(constructorId: String): JolpicaResponse<ConstructorTableData> {
        checkOnline()
        return JolpicaResponse(ConstructorTableData(ConstructorTableDto(listOf(ConstructorDto(constructorId, "Mercedes", "German")))))
    }

    override suspend fun count(kind: String, id: String, what: String, limit: Int): JolpicaResponse<TotalData> {
        checkOnline()
        synchronized(countRequests) { countRequests += "$kind/$id/$what" }
        return JolpicaResponse(TotalData((counts[what] ?: 0).toString()))
    }
}
