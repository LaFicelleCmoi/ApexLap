package com.apexlap.domain.repository

import com.apexlap.domain.model.ConstructorProfile
import com.apexlap.domain.model.ConstructorStanding
import com.apexlap.domain.model.DriverProfile
import com.apexlap.domain.model.DriverStanding
import com.apexlap.domain.model.Race
import com.apexlap.domain.model.QualifyingResult
import com.apexlap.domain.model.RaceResult
import com.apexlap.domain.model.ScoredResult
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first contract: `observe*` streams come from the local database (single source of truth),
 * `refresh*` fetch from the network and write into the database.
 */
interface F1Repository {
    fun observeCalendar(season: Int): Flow<List<Race>>
    suspend fun refreshCalendar(season: Int)

    fun observeDriverStandings(season: Int): Flow<List<DriverStanding>>
    fun observeConstructorStandings(season: Int): Flow<List<ConstructorStanding>>
    suspend fun refreshStandings(season: Int)

    fun observeRaceResults(season: Int, round: Int): Flow<List<RaceResult>>
    fun observeSprintResults(season: Int, round: Int): Flow<List<RaceResult>>
    fun observeQualifying(season: Int, round: Int): Flow<List<QualifyingResult>>

    /** Race, qualifying and (for sprint weekends) sprint results of one Grand Prix. */
    suspend fun refreshRaceWeekend(season: Int, round: Int, includeSprint: Boolean)

    /** Every race and sprint result of the season, the input of the statistics screens. */
    fun observeSeasonResults(season: Int): Flow<List<ScoredResult>>
    /** Several screens share this data (6 requests); a refresh younger than a few minutes is skipped unless [force]d. */
    suspend fun refreshSeasonResults(season: Int, force: Boolean = false)

    fun observeDriverProfile(driverId: String): Flow<DriverProfile?>

    /** Career totals barely change, so a profile younger than the cache TTL is not re-fetched unless [force]d. */
    suspend fun refreshDriverProfile(driverId: String, force: Boolean = false)

    fun observeConstructorProfile(constructorId: String): Flow<ConstructorProfile?>
    suspend fun refreshConstructorProfile(constructorId: String, force: Boolean = false)
}
