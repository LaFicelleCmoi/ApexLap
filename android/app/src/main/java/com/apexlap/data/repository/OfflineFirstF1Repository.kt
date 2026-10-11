package com.apexlap.data.repository

import com.apexlap.data.local.F1Dao
import com.apexlap.data.local.toDomain
import com.apexlap.data.local.toEntity
import com.apexlap.data.local.toScored
import com.apexlap.data.remote.dto.JolpicaResponse
import com.apexlap.data.remote.dto.RaceDto
import com.apexlap.data.remote.dto.RaceTableData
import com.apexlap.data.remote.JolpicaApi
import com.apexlap.data.remote.toDomain
import com.apexlap.domain.model.CareerStats
import com.apexlap.domain.model.ConstructorProfile
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverProfile
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.ScoredResult
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstF1Repository @Inject constructor(
    private val api: JolpicaApi,
    private val dao: F1Dao,
    private val clock: Clock,
) : F1Repository {

    private val seasonRefreshedAt = ConcurrentHashMap<Int, Instant>()

    override fun observeCalendar(season: Int): Flow<List<Race>> =
        dao.observeRaces(season).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshCalendar(season: Int) {
        val races = api.calendar(season).data.raceTable.races.map { it.toDomain() }
        dao.replaceRaces(season, races.map { it.toEntity() })
    }

    override fun observeDriverStandings(season: Int): Flow<List<DriverStanding>> =
        dao.observeDriverStandings(season).map { rows -> rows.map { it.toDomain() } }

    override fun observeConstructorStandings(season: Int): Flow<List<ConstructorStanding>> =
        dao.observeConstructorStandings(season).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshStandings(season: Int) = coroutineScope {
        val drivers = async { api.driverStandings(season) }
        val constructors = async { api.constructorStandings(season) }
        val driverRows = drivers.await().data.standingsTable.lists.firstOrNull()?.driverStandings.orEmpty()
            .mapIndexed { index, dto -> dto.toDomain(fallbackPosition = index + 1).toEntity(season) }
        val constructorRows = constructors.await().data.standingsTable.lists.firstOrNull()?.constructorStandings.orEmpty()
            .mapIndexed { index, dto -> dto.toDomain(fallbackPosition = index + 1).toEntity(season) }
        dao.replaceStandings(season, driverRows, constructorRows)
    }

    override fun observeRaceResults(season: Int, round: Int): Flow<List<RaceResult>> =
        dao.observeRaceResults(season, round, isSprint = false).map { rows -> rows.map { it.toDomain() } }

    override fun observeSprintResults(season: Int, round: Int): Flow<List<RaceResult>> =
        dao.observeRaceResults(season, round, isSprint = true).map { rows -> rows.map { it.toDomain() } }

    override fun observeQualifying(season: Int, round: Int): Flow<List<QualifyingResult>> =
        dao.observeQualifying(season, round).map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshRaceWeekend(season: Int, round: Int, includeSprint: Boolean) {
        val race = api.raceResults(season, round).data.raceTable.races.firstOrNull()?.results.orEmpty()
        dao.replaceRaceResults(season, round, isSprint = false, race.map { it.toDomain().toEntity(season, round) })

        val qualifying = api.qualifying(season, round).data.raceTable.races.firstOrNull()?.qualifyingResults.orEmpty()
        dao.replaceQualifying(season, round, qualifying.map { it.toDomain().toEntity(season, round) })

        if (includeSprint) {
            val sprint = api.sprintResults(season, round).data.raceTable.races.firstOrNull()?.sprintResults.orEmpty()
            dao.replaceRaceResults(season, round, isSprint = true, sprint.map { it.toDomain().toEntity(season, round, isSprint = true) })
        }
    }

    override fun observeSeasonResults(season: Int): Flow<List<ScoredResult>> =
        dao.observeSeasonResults(season).map { rows -> rows.map { it.toScored() } }

    override suspend fun refreshSeasonResults(season: Int, force: Boolean) {
        val last = seasonRefreshedAt[season]
        if (!force && last != null && Duration.between(last, clock.now()) < SEASON_TTL) return
        val races = allPages { api.seasonResults(season, it) }
            .flatMap { race -> race.results.map { race.round.toInt() to it } }
        val sprints = allPages { api.seasonSprintResults(season, it) }
            .flatMap { race -> race.sprintResults.map { race.round.toInt() to it } }
        val rows = races.map { (round, dto) -> dto.toDomain().toEntity(season, round) } +
            sprints.map { (round, dto) -> dto.toDomain().toEntity(season, round, isSprint = true) }
        // A race split across two pages shows up twice; keep one row per (round, session, driver).
        dao.replaceSeasonResults(season, rows.distinctBy { Triple(it.round, it.isSprint, it.driver.driverId) })
        seasonRefreshedAt[season] = clock.now()
    }

    private suspend fun allPages(page: suspend (offset: Int) -> JolpicaResponse<RaceTableData>): List<RaceDto> {
        val races = mutableListOf<RaceDto>()
        var offset = 0
        do {
            val response = page(offset).data
            races += response.raceTable.races
            offset += JolpicaApi.PAGE_SIZE
        } while (offset < (response.total.toIntOrNull() ?: 0))
        return races
    }

    override fun observeDriverProfile(driverId: String): Flow<DriverProfile?> =
        dao.observeDriverProfile(driverId).map { it?.toDomain() }

    override suspend fun refreshDriverProfile(driverId: String, force: Boolean) {
        if (!force && isFresh(dao.driverProfileFetchedAt(driverId))) return
        coroutineScope {
            val driver = async { api.driver(driverId).data.driverTable.drivers.first().toDomain() }
            val stats = async { careerStats(kind = "drivers", id = driverId) }
            dao.upsertDriverProfile(DriverProfile(driver.await(), stats.await()).toEntity(clock.now()))
        }
    }

    override fun observeConstructorProfile(constructorId: String): Flow<ConstructorProfile?> =
        dao.observeConstructorProfile(constructorId).map { it?.toDomain() }

    override suspend fun refreshConstructorProfile(constructorId: String, force: Boolean) {
        if (!force && isFresh(dao.constructorProfileFetchedAt(constructorId))) return
        coroutineScope {
            val constructor = async {
                api.constructor(constructorId).data.constructorTable.constructors.first().toDomain()
            }
            val stats = async { careerStats(kind = "constructors", id = constructorId) }
            dao.upsertConstructorProfile(ConstructorProfile(constructor.await(), stats.await()).toEntity(clock.now()))
        }
    }

    private fun isFresh(fetchedAtEpochSecond: Long?): Boolean =
        fetchedAtEpochSecond != null &&
            Duration.between(Instant.ofEpochSecond(fetchedAtEpochSecond), clock.now()) < PROFILE_TTL

    /**
     * Jolpica has no "career summary" endpoint, so statistics are derived from result counts.
     * Poles are starts from grid slot 1 (the qualifying endpoint over-counts).
     * For constructors, wins/podiums count car finishes (a 1-2 counts as one win and two podiums).
     */
    private suspend fun careerStats(kind: String, id: String): CareerStats = coroutineScope {
        val paths = listOf("seasons", "results", "results/1", "results/2", "results/3", "grid/1/results")
        val totals = paths.map { path -> async { api.count(kind, id, path).data.total.toIntOrNull() ?: 0 } }
            .awaitAll()
        val (seasons, races, wins, seconds, thirds, poles) = totals
        CareerStats(seasons = seasons, races = races, wins = wins, podiums = wins + seconds + thirds, poles = poles)
    }

    private operator fun <T> List<T>.component6(): T = this[5]

    companion object {
        val PROFILE_TTL: Duration = Duration.ofHours(12)
        val SEASON_TTL: Duration = Duration.ofMinutes(10)
    }
}
