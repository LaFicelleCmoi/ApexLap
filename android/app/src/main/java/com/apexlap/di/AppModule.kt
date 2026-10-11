package com.apexlap.di

import android.content.Context
import androidx.room.Room
import com.apexlap.BuildConfig
import com.apexlap.data.local.ApexLapDatabase
import com.apexlap.data.local.F1Dao
import com.apexlap.data.remote.JolpicaApi
import com.apexlap.data.remote.RateLimitInterceptor
import com.apexlap.data.repository.OfflineFirstF1Repository
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(RateLimitInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideJolpicaApi(client: OkHttpClient, json: Json): JolpicaApi = Retrofit.Builder()
        .baseUrl(JolpicaApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(JolpicaApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ApexLapDatabase =
        Room.databaseBuilder(context, ApexLapDatabase::class.java, ApexLapDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideF1Dao(database: ApexLapDatabase): F1Dao = database.f1Dao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock { Instant.now() }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindF1Repository(impl: OfflineFirstF1Repository): F1Repository
}
