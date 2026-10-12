package com.apexlap.data.encyclopedia

import android.content.Context
import com.apexlap.data.local.CircuitEntity
import com.apexlap.data.local.F1Dao
import com.apexlap.data.local.HistoricConstructorEntity
import com.apexlap.data.local.HistoricDriverEntity
import com.apexlap.data.local.toColumns
import com.apexlap.data.local.toDomain
import com.apexlap.data.remote.JolpicaApi
import com.apexlap.data.remote.toDomain
import com.apexlap.domain.encyclopedia.ChampionsRegistry
import com.apexlap.domain.encyclopedia.CircuitHistory
import com.apexlap.domain.encyclopedia.CircuitRecords
import com.apexlap.domain.encyclopedia.CircuitWin
import com.apexlap.domain.encyclopedia.LapRecord
import com.apexlap.domain.encyclopedia.parseLapTime
import com.apexlap.domain.encyclopedia.SeasonWinner
import com.apexlap.domain.model.Circuit
import com.apexlap.domain.model.Constructor
import com.apexlap.domain.model.Driver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Historical F1 data. All-time lists live in Room (downloaded once, refreshable); circuit and season
 * histories are fetched on demand and kept in memory; past champions come from a bundled asset.
 */
@Singleton
class EncyclopediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: JolpicaApi,
    private val dao: F1Dao,
    private val json: Json,
) {
    @Volatile private var registry: ChampionsRegistry? = null
    private val circuitHistories = ConcurrentHashMap<String, CircuitHistory>()
    private val seasonWinners = ConcurrentHashMap<Int, List<SeasonWinner>>()
    private val circuitRecords = ConcurrentHashMap<String, CircuitRecords>()

    suspend fun champions(): ChampionsRegistry = registry ?: withContext(Dispatchers.IO) {
        ChampionsRegistry(ChampionsAsset.load(context, json)).also { registry = it }
    }

    fun observeDrivers(): Flow<List<Driver>> = dao.observeHistoricDrivers().map { rows -> rows.map { it.driver.toDomain() } }
    fun observeConstructors(): Flow<List<Constructor>> = dao.observeHistoricConstructors().map { rows -> rows.map { it.constructor.toDomain() } }
    fun observeCircuits(): Flow<List<Circuit>> =
        dao.observeCircuits().map { rows -> rows.map { Circuit(it.circuitId, it.name, it.locality, it.country) } }

    /** The lists barely change: downloaded when empty, or when the user forces a refresh. */
    suspend fun refreshDrivers(force: Boolean) {
        if (!force && dao.historicDriverCount() > 0) return
        var offset = 0
        do {
            val page = api.allDrivers(offset).data
            dao.upsertHistoricDrivers(page.driverTable.drivers.map { HistoricDriverEntity(it.toDomain().toColumns()) })
            offset += JolpicaApi.PAGE_SIZE
        } while (offset < (page.total.toIntOrNull() ?: 0))
    }

    suspend fun refreshConstructors(force: Boolean) {
        if (!force && dao.historicConstructorCount() > 0) return
        var offset = 0
        do {
            val page = api.allConstructors(offset).data
            dao.upsertHistoricConstructors(page.constructorTable.constructors.map { HistoricConstructorEntity(it.toDomain().toColumns()) })
            offset += JolpicaApi.PAGE_SIZE
        } while (offset < (page.total.toIntOrNull() ?: 0))
    }

    suspend fun refreshCircuits(force: Boolean) {
        if (!force && dao.circuitCount() > 0) return
        dao.upsertCircuits(
            api.allCircuits().data.circuitTable.circuits.map {
                CircuitEntity(it.circuitId, it.circuitName, it.location.locality, it.location.country)
            },
        )
    }

    suspend fun circuitHistory(circuitId: String): CircuitHistory =
        circuitHistories[circuitId] ?: api.circuitWinners(circuitId).data.raceTable.races.let { races ->
            val circuit = races.firstOrNull()?.circuit?.toDomain() ?: Circuit(circuitId, circuitId, "", "")
            val wins = races.mapNotNull { race ->
                val winner = race.results.firstOrNull()?.toDomain() ?: return@mapNotNull null
                CircuitWin(race.season.toInt(), race.round.toInt(), race.raceName, winner.driver, winner.constructor)
            }.sortedByDescending { it.season }
            CircuitHistory(circuit, wins)
        }.also { circuitHistories[circuitId] = it }

    suspend fun seasonWinners(season: Int): List<SeasonWinner> =
        seasonWinners[season] ?: api.seasonWinners(season).data.raceTable.races.mapNotNull { dto ->
            val winner = dto.results.firstOrNull()?.toDomain() ?: return@mapNotNull null
            SeasonWinner(dto.toDomain(), winner)
        }.sortedBy { it.race.round }.also { seasonWinners[season] = it }

    suspend fun circuitRecords(circuitId: String): CircuitRecords = circuitRecords[circuitId] ?: run {
        val raceLap = api.circuitFastestLaps(circuitId).data.raceTable.races.mapNotNull { race ->
            val result = race.results.firstOrNull()?.toDomain() ?: return@mapNotNull null
            val time = result.fastestLapTime ?: return@mapNotNull null
            val millis = parseLapTime(time) ?: return@mapNotNull null
            millis to LapRecord(time, race.season.toInt(), result.driver, result.constructor)
        }.minByOrNull { it.first }?.second
        val poleSitters = api.circuitPoleSitters(circuitId).data.raceTable.races
            .mapNotNull { it.results.firstOrNull()?.toDomain()?.driver }
            .groupBy { it.id }.values.map { it.first() to it.size }
            .sortedWith(compareByDescending<Pair<Driver, Int>> { it.second }.thenBy { it.first.familyName }).take(3)
        CircuitRecords(raceLap, poleSitters)
    }.also { circuitRecords[circuitId] = it }
}
