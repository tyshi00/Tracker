package com.thelightphone.tracker

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

enum class WaterUnit(val label: String) {
    ML("ml"),
    LITERS("Liters"),
    FL_OZ("fl oz"),
    CUPS("Cups"),
    BOTTLES("Bottles (500ml)"),
}

object WaterConversion {
    // All conversions: amount in given unit → ml
    fun toMl(amount: Double, unit: WaterUnit): Double = when (unit) {
        WaterUnit.ML      -> amount
        WaterUnit.LITERS  -> amount * 1000.0
        WaterUnit.FL_OZ   -> amount / 0.033814
        WaterUnit.CUPS    -> amount / 0.00422675
        WaterUnit.BOTTLES -> amount * 500.0
    }

    // ml → given unit
    fun fromMl(ml: Double, unit: WaterUnit): Double = when (unit) {
        WaterUnit.ML      -> ml
        WaterUnit.LITERS  -> ml * 0.001
        WaterUnit.FL_OZ   -> ml * 0.033814
        WaterUnit.CUPS    -> ml * 0.00422675
        WaterUnit.BOTTLES -> ml * 0.002
    }

    fun format(ml: Double, unit: WaterUnit): String {
        val v = fromMl(ml, unit)
        return when {
            v == 0.0  -> "0"
            v < 10.0  -> "%.2f".format(v).trimEnd('0').trimEnd('.')
            v < 100.0 -> "%.1f".format(v).trimEnd('0').trimEnd('.')
            else      -> "%,.0f".format(v)
        }
    }
}

fun formatSleep(minutes: Int): String {
    if (minutes == 0) return "0h 0m"
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h ${m}m"
}

fun todayStr(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

fun weekStartStr(): String {
    val today = LocalDate.now()
    val wf = WeekFields.of(Locale.getDefault())
    val weekStart = today.with(wf.dayOfWeek(), 1)
    return weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

fun monthStartStr(): String {
    val today = LocalDate.now()
    return today.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
}

class TrackerRepository(private val db: TrackerDatabase) {

    companion object {
        const val PREF_WATER_UNIT = "water_unit"
        const val PREF_INVERT = "invert_colors"
        private const val DB_NAME = "tracker.db"

        @Volatile private var INSTANCE: TrackerRepository? = null

        fun getInstance(factory: () -> TrackerDatabase): TrackerRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackerRepository(factory()).also { INSTANCE = it }
            }
    }

    // ── Preferences ───────────────────────────────────────────────────────────

    suspend fun getWaterUnit(): WaterUnit {
        val raw = db.preferenceDao().get(PREF_WATER_UNIT)?.value ?: return WaterUnit.ML
        return WaterUnit.entries.firstOrNull { it.name == raw } ?: WaterUnit.ML
    }

    suspend fun setWaterUnit(unit: WaterUnit) {
        db.preferenceDao().set(PreferenceEntry(PREF_WATER_UNIT, unit.name))
    }

    suspend fun getInvertColors(): Boolean {
        return db.preferenceDao().get(PREF_INVERT)?.value == "true"
    }

    suspend fun setInvertColors(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_INVERT, value.toString()))
    }

    // ── Water ─────────────────────────────────────────────────────────────────

    suspend fun getTodayWaterMl(): Double =
        db.waterDao().getForDate(todayStr())?.totalMl ?: 0.0

    suspend fun addWater(amount: Double, unit: WaterUnit) {
        val ml = WaterConversion.toMl(amount, unit)
        val today = todayStr()
        val existing = db.waterDao().getForDate(today)?.totalMl ?: 0.0
        db.waterDao().upsert(WaterEntry(date = today, totalMl = existing + ml))
    }

    suspend fun getWeeklyWaterMl(): Double =
        db.waterDao().getFrom(weekStartStr()).sumOf { it.totalMl }

    suspend fun getMonthlyWaterMl(): Double =
        db.waterDao().getFrom(monthStartStr()).sumOf { it.totalMl }

    suspend fun resetWater() = db.waterDao().resetAll()

    // ── Steps ─────────────────────────────────────────────────────────────────

    suspend fun getTodaySteps(): Int =
        db.stepDao().getForDate(todayStr())?.totalSteps ?: 0

    suspend fun addSteps(steps: Int) {
        val today = todayStr()
        val existing = db.stepDao().getForDate(today)?.totalSteps ?: 0
        db.stepDao().upsert(StepEntry(date = today, totalSteps = existing + steps))
    }

    suspend fun getWeeklySteps(): Int =
        db.stepDao().getFrom(weekStartStr()).sumOf { it.totalSteps }

    suspend fun getMonthlySteps(): Int =
        db.stepDao().getFrom(monthStartStr()).sumOf { it.totalSteps }

    suspend fun resetSteps() = db.stepDao().resetAll()

    // ── Sleep ─────────────────────────────────────────────────────────────────

    suspend fun getTodaySleepMinutes(): Int =
        db.sleepDao().getForDate(todayStr())?.totalMinutes ?: 0

    suspend fun addSleep(hours: Int, minutes: Int) {
        val total = hours * 60 + minutes
        val today = todayStr()
        val existing = db.sleepDao().getForDate(today)?.totalMinutes ?: 0
        db.sleepDao().upsert(SleepEntry(date = today, totalMinutes = existing + total))
    }

    private fun avgMinutes(entries: List<SleepEntry>): Int {
        val withSleep = entries.filter { it.totalMinutes > 0 }
        if (withSleep.isEmpty()) return 0
        return (withSleep.sumOf { it.totalMinutes } / withSleep.size)
    }

    suspend fun getWeeklyAvgSleepMinutes(): Int =
        avgMinutes(db.sleepDao().getFrom(weekStartStr()))

    suspend fun getMonthlyAvgSleepMinutes(): Int =
        avgMinutes(db.sleepDao().getFrom(monthStartStr()))

    suspend fun resetSleep() = db.sleepDao().resetAll()

    // ── Reset all ─────────────────────────────────────────────────────────────

    suspend fun resetAll() {
        db.waterDao().resetAll()
        db.stepDao().resetAll()
        db.sleepDao().resetAll()
    }
}
