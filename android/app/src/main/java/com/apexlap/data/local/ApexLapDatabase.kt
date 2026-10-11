package com.apexlap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [
        RaceEntity::class,
        DriverStandingEntity::class,
        ConstructorStandingEntity::class,
        RaceResultEntity::class,
        QualifyingResultEntity::class,
        HistoricDriverEntity::class,
        HistoricConstructorEntity::class,
        CircuitEntity::class,
        DriverProfileEntity::class,
        ConstructorProfileEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ApexLapDatabase : RoomDatabase() {
    abstract fun f1Dao(): F1Dao

    companion object {
        const val NAME = "apex-lap.db"
    }
}

class Converters {
    @TypeConverter
    fun sessionsToJson(sessions: List<SessionRow>): String = Json.encodeToString(sessions)

    @TypeConverter
    fun sessionsFromJson(json: String): List<SessionRow> = Json.decodeFromString(json)
}
