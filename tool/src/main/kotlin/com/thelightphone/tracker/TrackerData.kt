package com.thelightphone.tracker

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

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

@Entity(tableName = "lap_entries")
data class LapEntry(
    @PrimaryKey val date: String,
    val totalLaps: Int = 0,
)

@Entity(tableName = "distance_entries")
data class DistanceEntry(
    @PrimaryKey val date: String,
    val totalMeters: Double = 0.0,
)

@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey val date: String,
    val totalMinutes: Int = 0,
)

/**
 * Sleep is stored as discrete sessions (bedtime → wake time) rather than a
 * single daily total, since a night's sleep almost always spans two
 * calendar dates. Multiple sessions per night are allowed (e.g. a nap),
 * same reasoning as Cycle/Mood.
 */
@Entity(tableName = "sleep_entries")
data class SleepEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bedDate: String, // YYYY-MM-DD — the date bedtime was on
    val bedTimeMinutes: Int, // minutes since midnight, 0-1439
    // Null while the entry is still "in progress" — bedtime logged, wake not
    // yet. Mirrors how an ongoing Cycle has no end date yet.
    val wakeDate: String? = null,
    val wakeTimeMinutes: Int? = null,
    val durationMinutes: Int? = null, // precomputed once both sides are known
)

@Entity(tableName = "cycle_entries")
data class CycleEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String, // YYYY-MM-DD
    val endDate: String? = null, // null while the cycle is still ongoing
)

/**
 * Flow, Energy, and Mood logged per day within a specific cycle — flow
 * especially can genuinely vary day to day (heavy start, light end, or the
 * reverse), so these live here instead of as one summary value on
 * CycleEntry. Mood here is only used when the standalone Mood tracker is
 * off — when it's on, Cycle links to it instead rather than storing
 * anything here (see CycleScreen).
 */
@Entity(tableName = "cycle_daily_entries")
data class CycleDailyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cycleId: Long, // which cycle this day belongs to
    val date: String, // YYYY-MM-DD
    val flow: String? = null, // FlowLevel.name
    val energy: String? = null, // EnergyLevel.name
    val moods: String? = null, // comma-joined Mood.name values, up to 5
)

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val date: String, // YYYY-MM-DD — one reading per day, like water/steps/sleep
    val weightKg: Double = 0.0,
)

/** Singleton row (id is always 0) holding the one-time starting weight reference point. */
@Entity(tableName = "starting_weight")
data class StartingWeightEntry(
    @PrimaryKey val id: Int = 0,
    val weightKg: Double,
    val date: String,
)

/** Multiple entries per day are allowed, like cycles — mood can shift through the day. */
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val moods: String, // comma-joined Mood.name values, up to 5
    val notes: String? = null,
)

/** Multiple entries per day are allowed, same reasoning as Mood — a person may want to log more than once in a day. */
@Entity(tableName = "note_entries")
data class NoteEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val note: String,
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

    @Query("SELECT * FROM water_entries WHERE date >= :from AND date < :to ORDER BY date DESC")
    suspend fun getBetween(from: String, to: String): List<WaterEntry>

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

    @Query("SELECT * FROM step_entries WHERE date >= :from AND date < :to ORDER BY date DESC")
    suspend fun getBetween(from: String, to: String): List<StepEntry>

    @Query("UPDATE step_entries SET totalSteps = 0")
    suspend fun resetAll()
}

@Dao
interface LapDao {
    @Query("SELECT * FROM lap_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): LapEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LapEntry)

    @Query("SELECT * FROM lap_entries WHERE date >= :from ORDER BY date DESC")
    suspend fun getFrom(from: String): List<LapEntry>

    @Query("SELECT * FROM lap_entries WHERE date >= :from AND date < :to ORDER BY date DESC")
    suspend fun getBetween(from: String, to: String): List<LapEntry>

    @Query("UPDATE lap_entries SET totalLaps = 0")
    suspend fun resetAll()
}

@Dao
interface DistanceDao {
    @Query("SELECT * FROM distance_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): DistanceEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DistanceEntry)

    @Query("SELECT * FROM distance_entries WHERE date >= :from ORDER BY date DESC")
    suspend fun getFrom(from: String): List<DistanceEntry>

    @Query("SELECT * FROM distance_entries WHERE date >= :from AND date < :to ORDER BY date DESC")
    suspend fun getBetween(from: String, to: String): List<DistanceEntry>

    @Query("UPDATE distance_entries SET totalMeters = 0")
    suspend fun resetAll()
}

@Dao
interface TimeDao {
    @Query("SELECT * FROM time_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): TimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TimeEntry)

    @Query("SELECT * FROM time_entries WHERE date >= :from ORDER BY date DESC")
    suspend fun getFrom(from: String): List<TimeEntry>

    @Query("SELECT * FROM time_entries WHERE date >= :from AND date < :to ORDER BY date DESC")
    suspend fun getBetween(from: String, to: String): List<TimeEntry>

    @Query("UPDATE time_entries SET totalMinutes = 0")
    suspend fun resetAll()
}

@Dao
interface SleepDao {
    @Insert
    suspend fun insert(entry: SleepEntry): Long

    @Update
    suspend fun update(entry: SleepEntry)

    /** The entry currently "in progress" (bedtime logged, wake not yet) — resume completing this one, if any. */
    @Query("SELECT * FROM sleep_entries WHERE wakeDate IS NULL ORDER BY bedDate DESC, id DESC LIMIT 1")
    suspend fun getMostRecentIncomplete(): SleepEntry?

    @Query(
        "SELECT * FROM sleep_entries WHERE durationMinutes IS NOT NULL " +
            "ORDER BY bedDate DESC, wakeTimeMinutes DESC, id DESC LIMIT 1",
    )
    suspend fun getMostRecentCompleted(): SleepEntry?

    @Query("SELECT * FROM sleep_entries WHERE bedDate >= :from AND durationMinutes IS NOT NULL ORDER BY bedDate DESC")
    suspend fun getFrom(from: String): List<SleepEntry>

    @Query(
        "SELECT * FROM sleep_entries WHERE bedDate >= :from AND bedDate < :to AND durationMinutes IS NOT NULL " +
            "ORDER BY bedDate DESC",
    )
    suspend fun getBetween(from: String, to: String): List<SleepEntry>

    @Query("SELECT * FROM sleep_entries ORDER BY bedDate DESC, wakeTimeMinutes DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SleepEntry>

    @Query("DELETE FROM sleep_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sleep_entries")
    suspend fun resetAll()
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences WHERE key = :key LIMIT 1")
    suspend fun get(key: String): PreferenceEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entry: PreferenceEntry)
}

@Dao
interface CycleDao {
    @Insert
    suspend fun insert(entry: CycleEntry): Long

    @Update
    suspend fun update(entry: CycleEntry)

    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC LIMIT 1")
    suspend fun getMostRecent(): CycleEntry?

    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<CycleEntry>

    @Query("DELETE FROM cycle_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cycle_entries")
    suspend fun resetAll()
}

@Dao
interface CycleDailyDao {
    @Insert
    suspend fun insert(entry: CycleDailyEntry): Long

    @Update
    suspend fun update(entry: CycleDailyEntry)

    @Query("SELECT * FROM cycle_daily_entries WHERE cycleId = :cycleId AND date = :date LIMIT 1")
    suspend fun getForCycleAndDate(cycleId: Long, date: String): CycleDailyEntry?

    @Query("SELECT * FROM cycle_daily_entries WHERE cycleId = :cycleId ORDER BY date ASC")
    suspend fun getForCycle(cycleId: Long): List<CycleDailyEntry>

    @Query("DELETE FROM cycle_daily_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cycle_daily_entries WHERE cycleId = :cycleId")
    suspend fun deleteForCycle(cycleId: Long)

    @Query("DELETE FROM cycle_daily_entries")
    suspend fun resetAll()
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): WeightEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getMostRecent(): WeightEntry?

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<WeightEntry>

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Query("DELETE FROM weight_entries")
    suspend fun resetAll()
}

@Dao
interface StartingWeightDao {
    @Query("SELECT * FROM starting_weight WHERE id = 0 LIMIT 1")
    suspend fun get(): StartingWeightEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entry: StartingWeightEntry)

    @Query("DELETE FROM starting_weight")
    suspend fun reset()
}

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(entry: MoodEntry): Long

    @Query("SELECT * FROM mood_entries ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getMostRecent(): MoodEntry?

    @Query("SELECT * FROM mood_entries WHERE date = :date ORDER BY id DESC LIMIT 1")
    suspend fun getMostRecentForDate(date: String): MoodEntry?

    @Query("SELECT * FROM mood_entries ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MoodEntry>

    @Query("SELECT COUNT(*) FROM mood_entries WHERE date >= :from AND date <= :to")
    suspend fun countInRange(from: String, to: String): Int

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM mood_entries")
    suspend fun resetAll()
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(entry: NoteEntry): Long

    @Query("SELECT * FROM note_entries ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getMostRecent(): NoteEntry?

    @Query("SELECT * FROM note_entries ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<NoteEntry>

    @Query("DELETE FROM note_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM note_entries")
    suspend fun resetAll()
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities = [
        WaterEntry::class, StepEntry::class, LapEntry::class, DistanceEntry::class, TimeEntry::class,
        SleepEntry::class, CycleEntry::class, CycleDailyEntry::class,
        WeightEntry::class, StartingWeightEntry::class, MoodEntry::class, NoteEntry::class, PreferenceEntry::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
    abstract fun stepDao(): StepDao
    abstract fun lapDao(): LapDao
    abstract fun distanceDao(): DistanceDao
    abstract fun timeDao(): TimeDao
    abstract fun sleepDao(): SleepDao
    abstract fun cycleDao(): CycleDao
    abstract fun cycleDailyDao(): CycleDailyDao
    abstract fun weightDao(): WeightDao
    abstract fun startingWeightDao(): StartingWeightDao
    abstract fun moodDao(): MoodDao
    abstract fun noteDao(): NoteDao
    abstract fun preferenceDao(): PreferenceDao
}
