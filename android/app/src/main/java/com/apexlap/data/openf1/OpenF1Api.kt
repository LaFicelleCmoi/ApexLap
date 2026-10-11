package com.apexlap.data.openf1

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * OpenF1 REST API. Historical data is free (3 req/s, 30 req/min); live data needs a sponsor account,
 * whose OAuth2 token is passed as a Bearer header. Date filters use the `date>`/`date<` query syntax.
 */
interface OpenF1Api {
    @GET("v1/sessions")
    suspend fun sessions(@Query("year") year: Int): List<OpenF1SessionDto>

    @GET("v1/sessions")
    suspend fun session(
        @Query("session_key") sessionKey: String,
        @Header("Authorization") auth: String? = null,
    ): List<OpenF1SessionDto>

    @GET("v1/drivers")
    suspend fun drivers(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1DriverDto>

    @GET("v1/position")
    suspend fun positions(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1PositionDto>

    @GET("v1/intervals")
    suspend fun intervals(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1IntervalDto>

    @GET("v1/laps")
    suspend fun laps(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1LapDto>

    @GET("v1/stints")
    suspend fun stints(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1StintDto>

    @GET("v1/race_control")
    suspend fun raceControl(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1RaceControlDto>

    @GET("v1/pit")
    suspend fun pits(@Query("session_key") sessionKey: String, @Header("Authorization") auth: String? = null): List<OpenF1PitDto>

    @GET("v1/car_data")
    suspend fun carData(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driver: Int,
        @Query("date>") from: String,
        @Query("date<") to: String,
    ): List<OpenF1CarDataDto>

    @GET("v1/location")
    suspend fun location(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driver: Int,
        @Query("date>") from: String,
        @Query("date<") to: String,
    ): List<OpenF1LocationDto>

    @FormUrlEncoded
    @POST("token")
    suspend fun token(@Field("username") username: String, @Field("password") password: String): OpenF1TokenDto

    companion object {
        const val BASE_URL = "https://api.openf1.org/"
        const val LIVE_WEBSOCKET_URL = "wss://mqtt.openf1.org:8084/mqtt"
    }
}
