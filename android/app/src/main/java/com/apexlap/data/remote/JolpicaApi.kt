package com.apexlap.data.remote

import com.apexlap.data.remote.dto.CircuitTableData
import com.apexlap.data.remote.dto.ConstructorTableData
import com.apexlap.data.remote.dto.DriverTableData
import com.apexlap.data.remote.dto.JolpicaResponse
import com.apexlap.data.remote.dto.RaceTableData
import com.apexlap.data.remote.dto.StandingsData
import com.apexlap.data.remote.dto.TotalData
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Jolpica-F1 (Ergast successor): https://github.com/jolpica/jolpica-f1 */
interface JolpicaApi {
    @GET("{season}.json")
    suspend fun calendar(
        @Path("season") season: Int,
        @Query("limit") limit: Int = 100,
    ): JolpicaResponse<RaceTableData>

    @GET("{season}/driverstandings.json")
    suspend fun driverStandings(
        @Path("season") season: Int,
        @Query("limit") limit: Int = 100,
    ): JolpicaResponse<StandingsData>

    @GET("{season}/constructorstandings.json")
    suspend fun constructorStandings(
        @Path("season") season: Int,
        @Query("limit") limit: Int = 100,
    ): JolpicaResponse<StandingsData>

    @GET("{season}/{round}/results.json")
    suspend fun raceResults(
        @Path("season") season: Int,
        @Path("round") round: Int,
        @Query("limit") limit: Int = 100,
    ): JolpicaResponse<RaceTableData>

    @GET("{season}/{round}/sprint.json")
    suspend fun sprintResults(
        @Path("season") season: Int,
        @Path("round") round: Int,
        @Query("limit") limit: Int = 100,
    ): JolpicaResponse<RaceTableData>

    @GET("{season}/{round}/qualifying.json")
    suspend fun qualifying(
        @Path("season") season: Int,
        @Path("round") round: Int,
        @Query("limit") limit: Int = 100,
    ): JolpicaResponse<RaceTableData>

    /** Every race result of a season, paginated (the API caps `limit` at 100). */
    @GET("{season}/results.json")
    suspend fun seasonResults(
        @Path("season") season: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = PAGE_SIZE,
    ): JolpicaResponse<RaceTableData>

    @GET("{season}/sprint.json")
    suspend fun seasonSprintResults(
        @Path("season") season: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = PAGE_SIZE,
    ): JolpicaResponse<RaceTableData>

    /** All-time drivers (≈900), paginated. */
    @GET("drivers.json")
    suspend fun allDrivers(@Query("offset") offset: Int, @Query("limit") limit: Int = PAGE_SIZE): JolpicaResponse<DriverTableData>

    @GET("constructors.json")
    suspend fun allConstructors(@Query("offset") offset: Int, @Query("limit") limit: Int = PAGE_SIZE): JolpicaResponse<ConstructorTableData>

    @GET("circuits.json")
    suspend fun allCircuits(@Query("limit") limit: Int = PAGE_SIZE): JolpicaResponse<CircuitTableData>

    /** Winners of every Grand Prix held at a circuit. */
    @GET("circuits/{circuitId}/results/1.json")
    suspend fun circuitWinners(@Path("circuitId") circuitId: String, @Query("limit") limit: Int = PAGE_SIZE): JolpicaResponse<RaceTableData>

    /** Winner of each Grand Prix of a season. */
    @GET("{season}/results/1.json")
    suspend fun seasonWinners(@Path("season") season: Int, @Query("limit") limit: Int = PAGE_SIZE): JolpicaResponse<RaceTableData>

    @GET("drivers/{driverId}.json")
    suspend fun driver(@Path("driverId") driverId: String): JolpicaResponse<DriverTableData>

    @GET("constructors/{constructorId}.json")
    suspend fun constructor(@Path("constructorId") constructorId: String): JolpicaResponse<ConstructorTableData>

    /**
     * Counts drivers or constructors entries: `{kind}/{id}/{what}` where `what` is e.g. `results`, `results/1`,
     * `grid/1/results` or `seasons`. Only `total` is read, so we request a single row.
     */
    @GET("{kind}/{id}/{what}.json")
    suspend fun count(
        @Path("kind") kind: String,
        @Path("id") id: String,
        @Path("what", encoded = true) what: String,
        @Query("limit") limit: Int = 1,
    ): JolpicaResponse<TotalData>

    companion object {
        const val BASE_URL = "https://api.jolpi.ca/ergast/f1/"
        const val PAGE_SIZE = 100
    }
}
