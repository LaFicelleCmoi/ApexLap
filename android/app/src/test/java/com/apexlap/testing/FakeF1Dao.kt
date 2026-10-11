package com.apexlap.testing

import com.apexlap.data.local.ConstructorProfileEntity
import com.apexlap.data.local.ConstructorStandingEntity
import com.apexlap.data.local.DriverProfileEntity
import com.apexlap.data.local.DriverStandingEntity
import com.apexlap.data.local.F1Dao
import com.apexlap.data.local.QualifyingResultEntity
import com.apexlap.data.local.RaceEntity
import com.apexlap.data.local.RaceResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory DAO mirroring the Room queries (filter + sort) so the repository can be tested on the JVM. */
class FakeF1Dao : F1Dao {
    val races = MutableStateFlow<List<RaceEntity>>(emptyList())
    val driverStandings = MutableStateFlow<List<DriverStandingEntity>>(emptyList())
    val constructorStandings = MutableStateFlow<List<ConstructorStandingEntity>>(emptyList())
    val results = MutableStateFlow<List<RaceResultEntity>>(emptyList())
    val qualifying = MutableStateFlow<List<QualifyingResultEntity>>(emptyList())
    val driverProfiles = MutableStateFlow<Map<String, DriverProfileEntity>>(emptyMap())
    val constructorProfiles = MutableStateFlow<Map<String, ConstructorProfileEntity>>(emptyMap())

    override fun observeRaces(season: Int): Flow<List<RaceEntity>> =
        races.map { rows -> rows.filter { it.season == season }.sortedBy { it.round } }

    override suspend fun deleteRaces(season: Int) = races.update { rows -> rows.filterNot { it.season == season } }

    override suspend fun insertRaces(races: List<RaceEntity>) = this.races.update { it + races }

    override fun observeDriverStandings(season: Int): Flow<List<DriverStandingEntity>> =
        driverStandings.map { rows -> rows.filter { it.season == season }.sortedBy { it.position } }

    override fun observeConstructorStandings(season: Int): Flow<List<ConstructorStandingEntity>> =
        constructorStandings.map { rows -> rows.filter { it.season == season }.sortedBy { it.position } }

    override suspend fun deleteDriverStandings(season: Int) =
        driverStandings.update { rows -> rows.filterNot { it.season == season } }

    override suspend fun deleteConstructorStandings(season: Int) =
        constructorStandings.update { rows -> rows.filterNot { it.season == season } }

    override suspend fun insertDriverStandings(rows: List<DriverStandingEntity>) = driverStandings.update { it + rows }

    override suspend fun insertConstructorStandings(rows: List<ConstructorStandingEntity>) =
        constructorStandings.update { it + rows }

    override fun observeRaceResults(season: Int, round: Int, isSprint: Boolean): Flow<List<RaceResultEntity>> =
        results.map { rows ->
            rows.filter { it.season == season && it.round == round && it.isSprint == isSprint }.sortedBy { it.position }
        }

    override fun observeSeasonResults(season: Int): Flow<List<RaceResultEntity>> =
        results.map { rows -> rows.filter { it.season == season }.sortedWith(compareBy({ it.round }, { it.isSprint }, { it.position })) }

    override suspend fun deleteRaceResults(season: Int, round: Int, isSprint: Boolean) =
        results.update { rows -> rows.filterNot { it.season == season && it.round == round && it.isSprint == isSprint } }

    override suspend fun deleteSeasonResults(season: Int) = results.update { rows -> rows.filterNot { it.season == season } }

    override fun observeQualifying(season: Int, round: Int): Flow<List<QualifyingResultEntity>> =
        qualifying.map { rows -> rows.filter { it.season == season && it.round == round }.sortedBy { it.position } }

    override suspend fun deleteQualifying(season: Int, round: Int) =
        qualifying.update { rows -> rows.filterNot { it.season == season && it.round == round } }

    override suspend fun insertQualifying(rows: List<QualifyingResultEntity>) = qualifying.update { it + rows }

    override suspend fun driverProfileFetchedAt(driverId: String): Long? = driverProfiles.value[driverId]?.fetchedAtEpochSecond

    override suspend fun constructorProfileFetchedAt(constructorId: String): Long? =
        constructorProfiles.value[constructorId]?.fetchedAtEpochSecond

    override suspend fun insertRaceResults(rows: List<RaceResultEntity>) = results.update { it + rows }

    override fun observeDriverProfile(driverId: String): Flow<DriverProfileEntity?> = driverProfiles.map { it[driverId] }

    override suspend fun upsertDriverProfile(profile: DriverProfileEntity) =
        driverProfiles.update { it + (profile.driver.driverId to profile) }

    override fun observeConstructorProfile(constructorId: String): Flow<ConstructorProfileEntity?> =
        constructorProfiles.map { it[constructorId] }

    override suspend fun upsertConstructorProfile(profile: ConstructorProfileEntity) =
        constructorProfiles.update { it + (profile.constructor.constructorId to profile) }
}
