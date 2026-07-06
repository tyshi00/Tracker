package com.thelightphone.tracker

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

enum class WaterUnit(val label: String) {
    ML("ml"),
    LITERS("Liters"),
    FL_OZ("fl oz"),
    CUPS("Cups"),
    BOTTLES("Bottles (500ml)"),
}

enum class FlowLevel(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
}

enum class Mood(val label: String) {
    NORMAL("Normal"),
    ANGRY("Angry"),
    SAD("Sad"),
    HAPPY("Happy"),
    DEPRESSED("Depressed"),
    DAZED("Dazed"),
    EXCITED("Excited"),
    ANXIOUS("Anxious"),
}

enum class EnergyLevel(val label: String) {
    HIGH("High"),
    MID_HIGH("Mid-high"),
    MEDIUM("Medium"),
    MID_LOW("Mid-low"),
    LOW("Low"),
    NO_ENERGY("No energy"),
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

/** Yesterday's date, used to show "last night's" sleep on the Home screen. */
fun previousDayStr(): String = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

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

/** [from, to) bounds for the previous full calendar month. */
fun previousMonthRange(): Pair<String, String> {
    val today = LocalDate.now()
    val firstOfThisMonth = today.withDayOfMonth(1)
    val firstOfPrevMonth = firstOfThisMonth.minusMonths(1)
    return firstOfPrevMonth.format(DateTimeFormatter.ISO_LOCAL_DATE) to
        firstOfThisMonth.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

/** [from, to) bounds for the trailing 12 months up to today. */
fun trailingYearRange(): Pair<String, String> {
    val today = LocalDate.now()
    val yearAgo = today.minusMonths(12)
    return yearAgo.format(DateTimeFormatter.ISO_LOCAL_DATE) to
        today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
}

/**
 * Human-friendly label for a YYYY-MM-DD date string, e.g. "Today",
 * "Yesterday", or "Wed, Jan 14" for anything older.
 */
fun dateLabel(dateStr: String): String {
    val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
}

/**
 * The most recent [days] dates (including today), newest first, as
 * YYYY-MM-DD strings — used to populate the backdate picker.
 */
fun recentDateOptions(days: Int = 14): List<String> {
    val today = LocalDate.now()
    return (0 until days).map { offset ->
        today.minusDays(offset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

class TrackerRepository(private val db: TrackerDatabase) {

    companion object {
        const val PREF_WATER_UNIT = "water_unit"
        const val PREF_INVERT = "invert_colors"
        const val PREF_CYCLE_ENABLED = "cycle_feature_enabled"
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

    /**
     * Cycle tracking is opt-in and hidden by default — not everyone tracks a
     * menstrual cycle, so the Home screen tile and Settings option for it
     * only appear once explicitly enabled here.
     */
    suspend fun getCycleFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_CYCLE_ENABLED)?.value == "true"
    }

    suspend fun setCycleFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_CYCLE_ENABLED, value.toString()))
    }

    // ── Water ─────────────────────────────────────────────────────────────────

    suspend fun getTodayWaterMl(): Double =
        db.waterDao().getForDate(todayStr())?.totalMl ?: 0.0

    suspend fun addWater(amount: Double, unit: WaterUnit, date: String = todayStr()) {
        val ml = WaterConversion.toMl(amount, unit)
        val existing = db.waterDao().getForDate(date)?.totalMl ?: 0.0
        db.waterDao().upsert(WaterEntry(date = date, totalMl = existing + ml))
    }

    suspend fun getWeeklyWaterMl(): Double =
        db.waterDao().getFrom(weekStartStr()).sumOf { it.totalMl }

    suspend fun getMonthlyWaterMl(): Double =
        db.waterDao().getFrom(monthStartStr()).sumOf { it.totalMl }

    suspend fun getPreviousMonthWaterMl(): Double {
        val (from, to) = previousMonthRange()
        return db.waterDao().getBetween(from, to).sumOf { it.totalMl }
    }

    /** Average monthly total over the trailing 12 months. */
    suspend fun getYearlyAverageWaterMl(): Double {
        val (from, to) = trailingYearRange()
        return db.waterDao().getBetween(from, to).sumOf { it.totalMl } / 12.0
    }

    suspend fun resetWater() = db.waterDao().resetAll()

    // ── Steps ─────────────────────────────────────────────────────────────────

    suspend fun getTodaySteps(): Int =
        db.stepDao().getForDate(todayStr())?.totalSteps ?: 0

    suspend fun addSteps(steps: Int, date: String = todayStr()) {
        val existing = db.stepDao().getForDate(date)?.totalSteps ?: 0
        db.stepDao().upsert(StepEntry(date = date, totalSteps = existing + steps))
    }

    suspend fun getWeeklySteps(): Int =
        db.stepDao().getFrom(weekStartStr()).sumOf { it.totalSteps }

    suspend fun getMonthlySteps(): Int =
        db.stepDao().getFrom(monthStartStr()).sumOf { it.totalSteps }

    suspend fun getPreviousMonthSteps(): Int {
        val (from, to) = previousMonthRange()
        return db.stepDao().getBetween(from, to).sumOf { it.totalSteps }
    }

    /** Average monthly total over the trailing 12 months. */
    suspend fun getYearlyAverageSteps(): Int {
        val (from, to) = trailingYearRange()
        val total = db.stepDao().getBetween(from, to).sumOf { it.totalSteps }
        return (total / 12.0).toInt()
    }

    suspend fun resetSteps() = db.stepDao().resetAll()

    // ── Sleep ─────────────────────────────────────────────────────────────────

    suspend fun getTodaySleepMinutes(): Int =
        db.sleepDao().getForDate(todayStr())?.totalMinutes ?: 0

    /**
     * Sleep is logged the morning after — what you enter "today" is last
     * night's sleep. The Home screen shows this (yesterday's entry) rather
     * than today's, since today's sleep entry won't exist until tomorrow.
     */
    suspend fun getPreviousDaySleepMinutes(): Int =
        db.sleepDao().getForDate(previousDayStr())?.totalMinutes ?: 0

    suspend fun addSleep(hours: Int, minutes: Int, date: String = todayStr()) {
        val total = hours * 60 + minutes
        val existing = db.sleepDao().getForDate(date)?.totalMinutes ?: 0
        db.sleepDao().upsert(SleepEntry(date = date, totalMinutes = existing + total))
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

    suspend fun getPreviousMonthSleepMinutes(): Int {
        val (from, to) = previousMonthRange()
        return db.sleepDao().getBetween(from, to).sumOf { it.totalMinutes }
    }

    /** Average monthly total over the trailing 12 months. */
    suspend fun getYearlyAverageSleepMinutes(): Int {
        val (from, to) = trailingYearRange()
        val total = db.sleepDao().getBetween(from, to).sumOf { it.totalMinutes }
        return (total / 12.0).toInt()
    }

    suspend fun resetSleep() = db.sleepDao().resetAll()

    // ── Cycle ─────────────────────────────────────────────────────────────────

    suspend fun getMostRecentCycle(): CycleEntry? = db.cycleDao().getMostRecent()

    /** Full cycle history, most recent first — used by the History screen. */
    suspend fun getCycleHistory(limit: Int = 100): List<CycleEntry> = db.cycleDao().getRecent(limit)

    /**
     * Inserts a new cycle if [id] is null, otherwise updates the existing
     * entry with that id (used to keep editing an in-progress cycle rather
     * than creating a duplicate every time details are added).
     */
    suspend fun saveCycle(
        id: Long?,
        startDate: String,
        endDate: String?,
        flow: FlowLevel?,
        mood: Mood?,
        energy: EnergyLevel?,
    ) {
        val entry = CycleEntry(
            id = id ?: 0,
            startDate = startDate,
            endDate = endDate,
            flow = flow?.name,
            mood = mood?.name,
            energy = energy?.name,
        )
        if (id != null) {
            db.cycleDao().update(entry)
        } else {
            db.cycleDao().insert(entry)
        }
    }

    suspend fun resetCycles() = db.cycleDao().resetAll()

    /**
     * Predicts the next cycle start date by averaging the gap between the
     * last few logged cycle start dates. Returns null if there isn't enough
     * history yet (fewer than 2 cycles logged).
     */
    suspend fun predictNextCycleStart(): String? {
        val recent = db.cycleDao().getRecent(6)
        if (recent.size < 2) return null
        val starts = recent
            .map { LocalDate.parse(it.startDate, DateTimeFormatter.ISO_LOCAL_DATE) }
            .sortedDescending()
        val gaps = starts.zipWithNext { newer, older -> ChronoUnit.DAYS.between(older, newer) }
        val avgGap = gaps.average()
        if (avgGap <= 0) return null
        return starts.first().plusDays(avgGap.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    // ── Reset all ─────────────────────────────────────────────────────────────

    suspend fun resetAll() {
        db.waterDao().resetAll()
        db.stepDao().resetAll()
        db.sleepDao().resetAll()
        db.cycleDao().resetAll()
    }
}
