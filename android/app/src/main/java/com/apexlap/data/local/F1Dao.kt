package com.apexlap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface F1Dao {
    @Query("SELECT * FROM races WHERE season = :season ORDER BY round")
    fun observeRaces(season: Int): Flow<List<RaceEntity>>

    @Query("DELETE FROM races WHERE season = :season")
    suspend fun deleteRaces(season: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaces(races: List<RaceEntity>)

    @Transaction
    suspend fun replaceRaces(season: Int, races: List<RaceEntity>) {
        deleteRaces(season)
        insertRaces(races)
    }

    @Query("SELECT * FROM driver_standings WHERE season = :season ORDER BY position")
    fun observeDriverStandings(season: Int): Flow<List<DriverStandingEntity>>

    @Query("SELECT * FROM constructor_standings WHERE season = :season ORDER BY position")
    fun observeConstructorStandings(season: Int): Flow<List<ConstructorStandingEntity>>

    @Query("DELETE FROM driver_standings WHERE season = :season")
    suspend fun deleteDriverStandings(season: Int)

    @Query("DELETE FROM constructor_standings WHERE season = :season")
    suspend fun deleteConstructorStandings(season: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriverStandings(rows: List<DriverStandingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConstructorStandings(rows: List<ConstructorStandingEntity>)

    @Transaction
    suspend fun replaceStandings(
        season: Int,
        drivers: List<DriverStandingEntity>,
        constructors: List<ConstructorStandingEntity>,
    ) {
        deleteDriverStandings(season)
        deleteConstructorStandings(season)
        insertDriverStandings(drivers)
        insertConstructorStandings(constructors)
    }

    @Query("SELECT * FROM race_results WHERE season = :season AND round = :round AND isSprint = :isSprint ORDER BY position")
    fun observeRaceResults(season: Int, round: Int, isSprint: Boolean): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE season = :season ORDER BY round, isSprint, position")
    fun observeSeasonResults(season: Int): Flow<List<RaceResultEntity>>

    @Query("DELETE FROM race_results WHERE season = :season AND round = :round AND isSprint = :isSprint")
    suspend fun deleteRaceResults(season: Int, round: Int, isSprint: Boolean)

    @Query("DELETE FROM race_results WHERE season = :season")
    suspend fun deleteSeasonResults(season: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceResults(rows: List<RaceResultEntity>)

    @Transaction
    suspend fun replaceRaceResults(season: Int, round: Int, isSprint: Boolean, rows: List<RaceResultEntity>) {
        deleteRaceResults(season, round, isSprint)
        insertRaceResults(rows)
    }

    @Transaction
    suspend fun replaceSeasonResults(season: Int, rows: List<RaceResultEntity>) {
        deleteSeasonResults(season)
        insertRaceResults(rows)
    }

    @Query("SELECT * FROM qualifying_results WHERE season = :season AND round = :round ORDER BY position")
    fun observeQualifying(season: Int, round: Int): Flow<List<QualifyingResultEntity>>

    @Query("DELETE FROM qualifying_results WHERE season = :season AND round = :round")
    suspend fun deleteQualifying(season: Int, round: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQualifying(rows: List<QualifyingResultEntity>)

    @Transaction
    suspend fun replaceQualifying(season: Int, round: Int, rows: List<QualifyingResultEntity>) {
        deleteQualifying(season, round)
        insertQualifying(rows)
    }

    @Query("SELECT * FROM historic_drivers ORDER BY familyName, givenName")
    fun observeHistoricDrivers(): Flow<List<HistoricDriverEntity>>

    @Query("SELECT COUNT(*) FROM historic_drivers")
    suspend fun historicDriverCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoricDrivers(rows: List<HistoricDriverEntity>)

    @Query("SELECT * FROM historic_constructors ORDER BY name")
    fun observeHistoricConstructors(): Flow<List<HistoricConstructorEntity>>

    @Query("SELECT COUNT(*) FROM historic_constructors")
    suspend fun historicConstructorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoricConstructors(rows: List<HistoricConstructorEntity>)

    @Query("SELECT * FROM circuits ORDER BY name")
    fun observeCircuits(): Flow<List<CircuitEntity>>

    @Query("SELECT COUNT(*) FROM circuits")
    suspend fun circuitCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCircuits(rows: List<CircuitEntity>)

    @Query("SELECT * FROM driver_profiles WHERE driverId = :driverId")
    fun observeDriverProfile(driverId: String): Flow<DriverProfileEntity?>

    @Query("SELECT fetchedAtEpochSecond FROM driver_profiles WHERE driverId = :driverId")
    suspend fun driverProfileFetchedAt(driverId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDriverProfile(profile: DriverProfileEntity)

    @Query("SELECT * FROM constructor_profiles WHERE constructorId = :constructorId")
    fun observeConstructorProfile(constructorId: String): Flow<ConstructorProfileEntity?>

    @Query("SELECT fetchedAtEpochSecond FROM constructor_profiles WHERE constructorId = :constructorId")
    suspend fun constructorProfileFetchedAt(constructorId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConstructorProfile(profile: ConstructorProfileEntity)
}
