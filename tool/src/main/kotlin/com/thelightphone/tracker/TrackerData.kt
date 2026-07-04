package com.thelightphone.tracker

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

// ── Entities ─────────────────────────────────────────────────────────────────

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val totalMl: Double = 0.0,
)

@Entity(tableName = "step_entries")
data class StepEntry(
    @PrimaryKey val date: String,
    val totalSteps: Int = 0,
)

@Entity(tableName = "sleep_entries")
data class SleepEntry(
    @PrimaryKey val date: String,
    val totalMinutes: Int = 0,
)

@Entity(tableName = "preferences")
data class PreferenceEntry(
    @PrimaryKey val key: String,
    val value: String,
)

// ── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): WaterEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WaterEntry)

    @Query("SELECT * FROM water_entries WHERE date >= :from ORDER BY date DESC")
    suspend fun getFrom(from: String): List<WaterEntry>

    @Query("UPDATE water_entries SET totalMl = 0")
    suspend fun resetAll()
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): StepEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: StepEntry)

    @Query("SELECT * FROM step_entries WHERE date >= :from ORDER BY date DESC")
    suspend fun getFrom(from: String): List<StepEntry>

    @Query("UPDATE step_entries SET totalSteps = 0")
    suspend fun resetAll()
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): SleepEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SleepEntry)

    @Query("SELECT * FROM sleep_entries WHERE date >= :from ORDER BY date DESC")
    suspend fun getFrom(from: String): List<SleepEntry>

    @Query("UPDATE sleep_entries SET totalMinutes = 0")
    suspend fun resetAll()
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE key = :key LIMIT 1")
    suspend fun get(key: String): PreferenceEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entry: PreferenceEntry)
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities = [WaterEntry::class, StepEntry::class, SleepEntry::class, PreferenceEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
    abstract fun stepDao(): StepDao
    abstract fun sleepDao(): SleepDao
    abstract fun preferenceDao(): PreferenceDao
}
