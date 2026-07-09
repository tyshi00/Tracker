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

enum class DistanceUnit(val label: String) {
    MILES("mi"),
    KM("km"),
}

object DistanceConversion {
    private const val METERS_PER_MILE = 1609.344

    fun toMeters(amount: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.MILES -> amount * METERS_PER_MILE
        DistanceUnit.KM -> amount * 1000.0
    }

    fun fromMeters(meters: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.MILES -> meters / METERS_PER_MILE
        DistanceUnit.KM -> meters / 1000.0
    }

    fun format(meters: Double, unit: DistanceUnit): String {
        val v = fromMeters(meters, unit)
        return "%.2f".format(v).trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }
}

/** The four ways Movement can be logged — kept fully independent (no shared math, no conversions between them). */
enum class MovementCategory(val label: String) {
    STEPS("Steps"),
    LAPS("Laps"),
    DISTANCE("Distance"),
    TIME("Time"),
}

enum class FlowLevel(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
}

enum class MoodCategory(val label: String) {
    HIGH_ENERGY_POSITIVE("High-Energy Positive"),
    LOW_ENERGY_POSITIVE("Low-Energy Positive"),
    ANGER_LEANING("Anger-Leaning"),
    SADNESS_LEANING("Sadness-Leaning"),
    FEAR_LEANING("Fear-Leaning"),
}

enum class Mood(val label: String, val category: MoodCategory) {
    // High-Energy Positive
    EXCITEMENT("Excitement", MoodCategory.HIGH_ENERGY_POSITIVE),
    JOY("Joy", MoodCategory.HIGH_ENERGY_POSITIVE),
    CONFIDENT("Confident", MoodCategory.HIGH_ENERGY_POSITIVE),
    CURIOUS("Curious", MoodCategory.HIGH_ENERGY_POSITIVE),
    DETERMINED("Determined", MoodCategory.HIGH_ENERGY_POSITIVE),
    INSPIRED("Inspired", MoodCategory.HIGH_ENERGY_POSITIVE),
    PROUD("Proud", MoodCategory.HIGH_ENERGY_POSITIVE),
    EAGER("Eager", MoodCategory.HIGH_ENERGY_POSITIVE),
    AMUSED("Amused", MoodCategory.HIGH_ENERGY_POSITIVE),
    FOCUSED("Focused", MoodCategory.HIGH_ENERGY_POSITIVE),
    SURPRISED("Surprised", MoodCategory.HIGH_ENERGY_POSITIVE),
    ROMANTIC("Romantic", MoodCategory.HIGH_ENERGY_POSITIVE),

    // Low-Energy Positive
    GRATEFUL("Grateful", MoodCategory.LOW_ENERGY_POSITIVE),
    CALM("Calm", MoodCategory.LOW_ENERGY_POSITIVE),
    CONTENT("Content", MoodCategory.LOW_ENERGY_POSITIVE),
    PEACE("Peace", MoodCategory.LOW_ENERGY_POSITIVE),
    HAPPINESS("Happiness", MoodCategory.LOW_ENERGY_POSITIVE),
    LOVE("Love", MoodCategory.LOW_ENERGY_POSITIVE),
    RELIEF("Relief", MoodCategory.LOW_ENERGY_POSITIVE),
    HOPEFUL("Hopeful", MoodCategory.LOW_ENERGY_POSITIVE),
    COMFORTABLE("Comfortable", MoodCategory.LOW_ENERGY_POSITIVE),
    OPTIMISTIC("Optimistic", MoodCategory.LOW_ENERGY_POSITIVE),
    CHEERFUL("Cheerful", MoodCategory.LOW_ENERGY_POSITIVE),
    WARM("Warm", MoodCategory.LOW_ENERGY_POSITIVE),

    // Anger-Leaning
    ANGER("Anger", MoodCategory.ANGER_LEANING),
    FRUSTRATED("Frustrated", MoodCategory.ANGER_LEANING),
    ANNOYED("Annoyed", MoodCategory.ANGER_LEANING),
    IRRITATED("Irritated", MoodCategory.ANGER_LEANING),
    JEALOUS("Jealous", MoodCategory.ANGER_LEANING),
    RESENTFUL("Resentful", MoodCategory.ANGER_LEANING),
    BITTER("Bitter", MoodCategory.ANGER_LEANING),
    DISGUST("Disgust", MoodCategory.ANGER_LEANING),
    IMPATIENT("Impatient", MoodCategory.ANGER_LEANING),
    OFFENDED("Offended", MoodCategory.ANGER_LEANING),
    COLD("Cold", MoodCategory.ANGER_LEANING),
    DEFENSIVE("Defensive", MoodCategory.ANGER_LEANING),

    // Sadness-Leaning
    SADNESS("Sadness", MoodCategory.SADNESS_LEANING),
    TIRED("Tired", MoodCategory.SADNESS_LEANING),
    LONELY("Lonely", MoodCategory.SADNESS_LEANING),
    DEPRESSED("Depressed", MoodCategory.SADNESS_LEANING),
    DISAPPOINTED("Disappointed", MoodCategory.SADNESS_LEANING),
    BURNT_OUT("Burnt Out", MoodCategory.SADNESS_LEANING),
    HOPELESS("Hopeless", MoodCategory.SADNESS_LEANING),
    INSECURE("Insecure", MoodCategory.SADNESS_LEANING),
    GUILT("Guilt", MoodCategory.SADNESS_LEANING),
    EMBARRASSED("Embarrassed", MoodCategory.SADNESS_LEANING),
    VULNERABLE("Vulnerable", MoodCategory.SADNESS_LEANING),
    MISERABLE("Miserable", MoodCategory.SADNESS_LEANING),

    // Fear-Leaning
    ANXIOUS("Anxious", MoodCategory.FEAR_LEANING),
    WORRIED("Worried", MoodCategory.FEAR_LEANING),
    OVERWHELMED("Overwhelmed", MoodCategory.FEAR_LEANING),
    STRESSED("Stressed", MoodCategory.FEAR_LEANING),
    NERVOUS("Nervous", MoodCategory.FEAR_LEANING),
    FEARFUL("Fearful", MoodCategory.FEAR_LEANING),
    UNCOMFORTABLE("Uncomfortable", MoodCategory.FEAR_LEANING),
    TENSE("Tense", MoodCategory.FEAR_LEANING),
    CONFUSED("Confused", MoodCategory.FEAR_LEANING),
    PANICKED("Panicked", MoodCategory.FEAR_LEANING),
    SUSPICIOUS("Suspicious", MoodCategory.FEAR_LEANING),
    SHY("Shy", MoodCategory.FEAR_LEANING),
}

/** Serializes up to 5 moods as a comma-joined list of enum names for storage. */
fun encodeMoods(moods: List<Mood>): String = moods.joinToString(",") { it.name }

/** Parses a comma-joined mood list back from storage, ignoring unknown/blank entries. */
fun decodeMoods(raw: String?): List<Mood> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(",").mapNotNull { name -> Mood.entries.firstOrNull { it.name == name.trim() } }
}

enum class EnergyLevel(val label: String) {
    HIGH("High"),
    MID_HIGH("Mid-high"),
    MEDIUM("Medium"),
    MID_LOW("Mid-low"),
    LOW("Low"),
    NO_ENERGY("No energy"),
}

enum class WeightUnit(val label: String) {
    LBS("lbs"),
    KG("kg"),
}

object WeightConversion {
    private const val KG_PER_LB = 0.45359237

    fun toKg(value: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.LBS -> value * KG_PER_LB
        WeightUnit.KG -> value
    }

    fun fromKg(kg: Double, unit: WeightUnit): Double = when (unit) {
        WeightUnit.LBS -> kg / KG_PER_LB
        WeightUnit.KG -> kg
    }

    fun format(kg: Double, unit: WeightUnit): String {
        val v = fromKg(kg, unit)
        return "%.1f".format(v).trimEnd('0').trimEnd('.')
    }

    /**
     * Formats a signed weekly rate of change with an explicit "+" for gains
     * so the number reads as neutral fact, not implicitly framed toward loss.
     */
    fun formatWeeklyChange(deltaKg: Double, unit: WeightUnit): String {
        val converted = fromKg(deltaKg, unit)
        val sign = when {
            converted > 0.05 -> "+"
            converted < -0.05 -> "-"
            else -> ""
        }
        val magnitude = kotlin.math.abs(converted)
        val formatted = "%.1f".format(magnitude).trimEnd('0').trimEnd('.')
        return "$sign$formatted ${unit.label}/week"
    }
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
 * Simple in-memory holder for the current date format, kept in sync with
 * the stored preference — same reasoning as LightThemeController for theme:
 * dateLabel() is called synchronously from dozens of places throughout the
 * app, so it can't itself be a suspend function reading the database on
 * every call. Refreshed whenever the format is read or changed.
 */
object DateFormatHolder {
    @Volatile
    var current: DateFormat = DateFormat.MDY
}

/**
 * Formats a YYYY-MM-DD date string using the person's chosen date format —
 * always a full numeric date (including year), never relative words like
 * "Today"/"Yesterday" and never just a weekday/month/day without a year
 * (which made same month-day dates from different years indistinguishable).
 */
fun dateLabel(dateStr: String): String {
    val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    val pattern = when (DateFormatHolder.current) {
        DateFormat.MDY -> "MM/dd/yyyy"
        DateFormat.DMY -> "dd/MM/yyyy"
        DateFormat.YMD -> "yyyy/MM/dd"
    }
    return date.format(DateTimeFormatter.ofPattern(pattern))
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

enum class AmPm(val label: String) {
    AM("AM"),
    PM("PM"),
}

enum class TimeFormat(val label: String) {
    AM_PM("AM/PM"),
    HOUR_24("24-hour"),
}

enum class DateFormat(val label: String) {
    MDY("mm/dd/yyyy"),
    DMY("dd/mm/yyyy"),
    YMD("yyyy/mm/dd"),
}

/** Converts a 12-hour clock reading to minutes since midnight (0-1439). */
fun to24HourMinutes(hour12: Int, minute: Int, ampm: AmPm): Int {
    val clampedHour = hour12.coerceIn(1, 12)
    val clampedMinute = minute.coerceIn(0, 59)
    val hour24 = when {
        ampm == AmPm.AM && clampedHour == 12 -> 0
        ampm == AmPm.PM && clampedHour != 12 -> clampedHour + 12
        else -> clampedHour
    }
    return hour24 * 60 + clampedMinute
}

/** Converts a 24-hour clock reading to minutes since midnight (0-1439). */
fun to24HourFormatMinutes(hour24: Int, minute: Int): Int {
    return hour24.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)
}

/**
 * Duration between an explicit sleep date+time and an explicit wake
 * date+time. Since both dates are entered directly (not inferred), this is
 * just a plain date-time subtraction — can come out negative if wake was
 * set before sleep, which callers should treat as an invalid entry.
 */
fun computeSleepDurationMinutes(
    sleepDate: String,
    sleepMinutesOfDay: Int,
    wakeDate: String,
    wakeMinutesOfDay: Int,
): Int {
    val sleepDateTime = LocalDate.parse(sleepDate, DateTimeFormatter.ISO_LOCAL_DATE)
        .atStartOfDay()
        .plusMinutes(sleepMinutesOfDay.toLong())
    val wakeDateTime = LocalDate.parse(wakeDate, DateTimeFormatter.ISO_LOCAL_DATE)
        .atStartOfDay()
        .plusMinutes(wakeMinutesOfDay.toLong())
    return ChronoUnit.MINUTES.between(sleepDateTime, wakeDateTime).toInt()
}

/**
 * Splits minutes-since-midnight back into Hour/Minute/AmPm for pre-filling
 * editable fields — e.g. when resuming an in-progress sleep entry. Always
 * re-derived from the current time format rather than stored directly, so
 * switching AM/PM vs 24-hour in Settings between logging bedtime and
 * completing it later still displays correctly.
 */
fun decomposeClockMinutes(minutesSinceMidnight: Int, format: TimeFormat): Triple<String, String, AmPm?> {
    val hour24 = minutesSinceMidnight / 60
    val minute = minutesSinceMidnight % 60
    return if (format == TimeFormat.HOUR_24) {
        Triple(hour24.toString(), minute.toString(), null)
    } else {
        val ampm = if (hour24 < 12) AmPm.AM else AmPm.PM
        val hour12 = when (hour24 % 12) {
            0 -> 12
            else -> hour24 % 12
        }
        Triple(hour12.toString(), minute.toString(), ampm)
    }
}

class TrackerRepository(private val db: TrackerDatabase) {

    companion object {
        const val PREF_WATER_UNIT = "water_unit"
        const val PREF_INVERT = "invert_colors"
        const val PREF_CYCLE_ENABLED = "cycle_feature_enabled"
        const val PREF_WEIGHT_UNIT = "weight_unit"
        const val PREF_WEIGHT_ENABLED = "weight_feature_enabled"
        const val PREF_MOOD_ENABLED = "mood_feature_enabled"
        const val PREF_TIME_FORMAT = "time_format"
        const val PREF_DATE_FORMAT = "date_format"
        const val PREF_WATER_ENABLED = "water_feature_enabled"
        const val PREF_STEPS_ENABLED = "steps_feature_enabled"
        const val PREF_SLEEP_ENABLED = "sleep_feature_enabled"
        const val PREF_MOVEMENT_ENABLED = "movement_feature_enabled"
        const val PREF_LAPS_ENABLED = "laps_feature_enabled"
        const val PREF_DISTANCE_ENABLED = "distance_feature_enabled"
        const val PREF_TIME_ENABLED = "time_feature_enabled"
        const val PREF_PRIMARY_MOVEMENT_CATEGORY = "primary_movement_category"
        const val PREF_DISTANCE_UNIT = "distance_unit"
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

    // ── Laps ──────────────────────────────────────────────────────────────────
    // Same pattern as Steps — a fully independent tally, no shared math with
    // any other movement category.

    suspend fun getTodayLaps(): Int =
        db.lapDao().getForDate(todayStr())?.totalLaps ?: 0

    suspend fun addLaps(laps: Int, date: String = todayStr()) {
        val existing = db.lapDao().getForDate(date)?.totalLaps ?: 0
        db.lapDao().upsert(LapEntry(date = date, totalLaps = existing + laps))
    }

    suspend fun getWeeklyLaps(): Int =
        db.lapDao().getFrom(weekStartStr()).sumOf { it.totalLaps }

    suspend fun getMonthlyLaps(): Int =
        db.lapDao().getFrom(monthStartStr()).sumOf { it.totalLaps }

    suspend fun getPreviousMonthLaps(): Int {
        val (from, to) = previousMonthRange()
        return db.lapDao().getBetween(from, to).sumOf { it.totalLaps }
    }

    suspend fun getYearlyAverageLaps(): Int {
        val (from, to) = trailingYearRange()
        val total = db.lapDao().getBetween(from, to).sumOf { it.totalLaps }
        return (total / 12.0).toInt()
    }

    suspend fun resetLaps() = db.lapDao().resetAll()

    // ── Distance ──────────────────────────────────────────────────────────────
    // Stored in meters internally (same "canonical unit" approach as Water's
    // ml), converted for display via DistanceConversion.

    suspend fun getTodayDistanceMeters(): Double =
        db.distanceDao().getForDate(todayStr())?.totalMeters ?: 0.0

    suspend fun addDistance(meters: Double, date: String = todayStr()) {
        val existing = db.distanceDao().getForDate(date)?.totalMeters ?: 0.0
        db.distanceDao().upsert(DistanceEntry(date = date, totalMeters = existing + meters))
    }

    suspend fun getWeeklyDistanceMeters(): Double =
        db.distanceDao().getFrom(weekStartStr()).sumOf { it.totalMeters }

    suspend fun getMonthlyDistanceMeters(): Double =
        db.distanceDao().getFrom(monthStartStr()).sumOf { it.totalMeters }

    suspend fun getPreviousMonthDistanceMeters(): Double {
        val (from, to) = previousMonthRange()
        return db.distanceDao().getBetween(from, to).sumOf { it.totalMeters }
    }

    suspend fun getYearlyAverageDistanceMeters(): Double {
        val (from, to) = trailingYearRange()
        val total = db.distanceDao().getBetween(from, to).sumOf { it.totalMeters }
        return total / 12.0
    }

    suspend fun resetDistance() = db.distanceDao().resetAll()

    // ── Time ──────────────────────────────────────────────────────────────────
    // For activities better described by duration than a count — a hike, a
    // swim, yard work, dancing, anything without a step or lap to tally.

    suspend fun getTodayTimeMinutes(): Int =
        db.timeDao().getForDate(todayStr())?.totalMinutes ?: 0

    suspend fun addTime(minutes: Int, date: String = todayStr()) {
        val existing = db.timeDao().getForDate(date)?.totalMinutes ?: 0
        db.timeDao().upsert(TimeEntry(date = date, totalMinutes = existing + minutes))
    }

    suspend fun getWeeklyTimeMinutes(): Int =
        db.timeDao().getFrom(weekStartStr()).sumOf { it.totalMinutes }

    suspend fun getMonthlyTimeMinutes(): Int =
        db.timeDao().getFrom(monthStartStr()).sumOf { it.totalMinutes }

    suspend fun getPreviousMonthTimeMinutes(): Int {
        val (from, to) = previousMonthRange()
        return db.timeDao().getBetween(from, to).sumOf { it.totalMinutes }
    }

    suspend fun getYearlyAverageTimeMinutes(): Int {
        val (from, to) = trailingYearRange()
        val total = db.timeDao().getBetween(from, to).sumOf { it.totalMinutes }
        return (total / 12.0).toInt()
    }

    suspend fun resetTime() = db.timeDao().resetAll()

    // ── Movement settings ────────────────────────────────────────────────────

    /** Master toggle for the whole Movement feature — defaults on, same as Steps did before this replaced it. */
    suspend fun getMovementFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_MOVEMENT_ENABLED)?.value != "false"
    }

    suspend fun setMovementFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_MOVEMENT_ENABLED, value.toString()))
    }

    // Sub-category visibility — all default visible, so Movement shows every
    // way of logging activity out of the box; trim down from there.
    suspend fun getLapsFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_LAPS_ENABLED)?.value != "false"
    }

    suspend fun setLapsFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_LAPS_ENABLED, value.toString()))
    }

    suspend fun getDistanceFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_DISTANCE_ENABLED)?.value != "false"
    }

    suspend fun setDistanceFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_DISTANCE_ENABLED, value.toString()))
    }

    suspend fun getTimeFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_TIME_ENABLED)?.value != "false"
    }

    suspend fun setTimeFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_TIME_ENABLED, value.toString()))
    }

    suspend fun getDistanceUnit(): DistanceUnit {
        val raw = db.preferenceDao().get(PREF_DISTANCE_UNIT)?.value ?: return DistanceUnit.MILES
        return DistanceUnit.entries.firstOrNull { it.name == raw } ?: DistanceUnit.MILES
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        db.preferenceDao().set(PreferenceEntry(PREF_DISTANCE_UNIT, unit.name))
    }

    suspend fun getPrimaryMovementCategory(): MovementCategory {
        val raw = db.preferenceDao().get(PREF_PRIMARY_MOVEMENT_CATEGORY)?.value ?: return MovementCategory.STEPS
        return MovementCategory.entries.firstOrNull { it.name == raw } ?: MovementCategory.STEPS
    }

    suspend fun setPrimaryMovementCategory(category: MovementCategory) {
        db.preferenceDao().set(PreferenceEntry(PREF_PRIMARY_MOVEMENT_CATEGORY, category.name))
    }

    /**
     * Whichever categories are currently visible, in a fixed display order —
     * used both to populate the primary-category picker (so it only ever
     * offers something actually visible) and to silently fall back the
     * primary category if its current pick gets hidden.
     */
    suspend fun getVisibleMovementCategories(): List<MovementCategory> {
        val result = mutableListOf<MovementCategory>()
        if (getStepsFeatureEnabled()) result.add(MovementCategory.STEPS)
        if (getLapsFeatureEnabled()) result.add(MovementCategory.LAPS)
        if (getDistanceFeatureEnabled()) result.add(MovementCategory.DISTANCE)
        if (getTimeFeatureEnabled()) result.add(MovementCategory.TIME)
        return result
    }

    /**
     * The current primary category, falling back (and persisting that
     * fallback) if it's no longer visible — e.g. its category was just
     * hidden via Movement Type. Shared by the Home tile and Settings so
     * both stay consistent without duplicating the correction logic.
     */
    suspend fun getPrimaryMovementCategorySelfCorrected(): MovementCategory {
        val visible = getVisibleMovementCategories()
        var primary = getPrimaryMovementCategory()
        if (primary !in visible) {
            primary = visible.firstOrNull() ?: primary
            setPrimaryMovementCategory(primary)
        }
        return primary
    }

    /** Today's value for whichever category is currently primary, formatted for the Home tile. */
    suspend fun getPrimaryMovementDisplay(): String {
        val visible = getVisibleMovementCategories()
        val primary = getPrimaryMovementCategorySelfCorrected()
        if (visible.isEmpty()) return ""
        return when (primary) {
            MovementCategory.STEPS -> "%,d steps".format(getTodaySteps())
            MovementCategory.LAPS -> "%,d laps".format(getTodayLaps())
            MovementCategory.DISTANCE -> {
                val unit = getDistanceUnit()
                "${DistanceConversion.format(getTodayDistanceMeters(), unit)} ${unit.label}"
            }
            MovementCategory.TIME -> formatSleep(getTodayTimeMinutes())
        }
    }

    // ── Sleep ─────────────────────────────────────────────────────────────────

    /**
     * Logs a complete sleep session in one shot — both bedtime and wake time
     * known already. [wakeDate] is inferred by the caller: if wake time-of-day
     * is earlier than (or equal to) bed time-of-day, sleep crossed midnight
     * and wake is the next calendar day; otherwise it's a same-day session
     * (e.g. a nap).
     */
    suspend fun addSleepSession(
        bedDate: String,
        bedTimeMinutes: Int,
        wakeDate: String,
        wakeTimeMinutes: Int,
    ) {
        val duration = computeSleepDurationMinutes(bedDate, bedTimeMinutes, wakeDate, wakeTimeMinutes)
            .coerceAtLeast(0)
        db.sleepDao().insert(
            SleepEntry(
                bedDate = bedDate,
                bedTimeMinutes = bedTimeMinutes,
                wakeDate = wakeDate,
                wakeTimeMinutes = wakeTimeMinutes,
                durationMinutes = duration,
            ),
        )
    }

    /**
     * Starts a sleep entry with just bedtime known — wake time gets filled
     * in later, same shape as an ongoing Cycle with no end date yet. Returns
     * the new entry's id so it can be completed afterward.
     */
    suspend fun startSleepEntry(bedDate: String, bedTimeMinutes: Int): Long {
        return db.sleepDao().insert(SleepEntry(bedDate = bedDate, bedTimeMinutes = bedTimeMinutes))
    }

    /** Corrects the bed side of an entry that's still waiting on a wake time — doesn't complete it. */
    suspend fun updateSleepEntryBedSide(id: Long, bedDate: String, bedTimeMinutes: Int) {
        db.sleepDao().update(SleepEntry(id = id, bedDate = bedDate, bedTimeMinutes = bedTimeMinutes))
    }

    /** Fills in the wake side of an entry [startSleepEntry] began earlier, computing its duration. */
    suspend fun completeSleepEntry(id: Long, bedDate: String, bedTimeMinutes: Int, wakeDate: String, wakeTimeMinutes: Int) {
        val duration = computeSleepDurationMinutes(bedDate, bedTimeMinutes, wakeDate, wakeTimeMinutes)
            .coerceAtLeast(0)
        db.sleepDao().update(
            SleepEntry(
                id = id,
                bedDate = bedDate,
                bedTimeMinutes = bedTimeMinutes,
                wakeDate = wakeDate,
                wakeTimeMinutes = wakeTimeMinutes,
                durationMinutes = duration,
            ),
        )
    }

    /** The entry currently waiting on a wake time, if any — resume completing this one rather than starting fresh. */
    suspend fun getIncompleteSleepEntry(): SleepEntry? = db.sleepDao().getMostRecentIncomplete()

    suspend fun getMostRecentSleepDurationMinutes(): Int? = db.sleepDao().getMostRecentCompleted()?.durationMinutes

    suspend fun getSleepHistory(limit: Int = 100): List<SleepEntry> = db.sleepDao().getRecent(limit)

    suspend fun deleteSleepEntry(id: Long) = db.sleepDao().deleteById(id)

    /**
     * Sums session durations per bedDate first, then averages across nights
     * that have any sleep logged. Only ever sees completed entries — the DAO
     * queries already filter out anything still waiting on a wake time, so
     * an unfinished entry can't skew an average with a missing duration.
     */
    private fun avgMinutes(entries: List<SleepEntry>): Int {
        val perNight = entries.groupBy { it.bedDate }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes ?: 0 } }
        val withSleep = perNight.values.filter { it > 0 }
        if (withSleep.isEmpty()) return 0
        return withSleep.sum() / withSleep.size
    }

    suspend fun getWeeklyAvgSleepMinutes(): Int =
        avgMinutes(db.sleepDao().getFrom(weekStartStr()))

    suspend fun getMonthlyAvgSleepMinutes(): Int =
        avgMinutes(db.sleepDao().getFrom(monthStartStr()))

    suspend fun getPreviousMonthSleepMinutes(): Int {
        val (from, to) = previousMonthRange()
        return db.sleepDao().getBetween(from, to).sumOf { it.durationMinutes ?: 0 }
    }

    /** Average monthly total over the trailing 12 months. */
    suspend fun getYearlyAverageSleepMinutes(): Int {
        val (from, to) = trailingYearRange()
        val total = db.sleepDao().getBetween(from, to).sumOf { it.durationMinutes ?: 0 }
        return (total / 12.0).toInt()
    }

    suspend fun resetSleep() = db.sleepDao().resetAll()

    // ── Cycle ─────────────────────────────────────────────────────────────────

    suspend fun getMostRecentCycle(): CycleEntry? = db.cycleDao().getMostRecent()

    /** Full cycle history, most recent first — used by the History screen. */
    suspend fun getCycleHistory(limit: Int = 100): List<CycleEntry> = db.cycleDao().getRecent(limit)

    suspend fun deleteCycleEntry(id: Long) {
        db.cycleDao().deleteById(id)
        db.cycleDailyDao().deleteForCycle(id) // its daily entries belong nowhere without it
    }

    /**
     * Inserts a new cycle if [id] is null, otherwise updates the existing
     * entry with that id (used to keep editing an in-progress cycle rather
     * than creating a duplicate every time details are added).
     */
    suspend fun saveCycle(
        id: Long?,
        startDate: String,
        endDate: String?,
    ) {
        val entry = CycleEntry(
            id = id ?: 0,
            startDate = startDate,
            endDate = endDate,
        )
        if (id != null) {
            db.cycleDao().update(entry)
        } else {
            db.cycleDao().insert(entry)
        }
    }

    suspend fun resetCycles() {
        db.cycleDao().resetAll()
        db.cycleDailyDao().resetAll()
    }

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

    // ── Cycle daily entries (Flow / Energy per day) ─────────────────────────────
    //
    // Only meaningful while a cycle is ongoing (no end date set yet) — once a
    // cycle is closed out, its days are done being logged, same as the rest
    // of the cycle's details.

    /**
     * Logs Flow and/or Energy for a specific day within [cycleId]. Only the
     * fields actually provided are changed — if a day already has, say, an
     * Energy value and this call only supplies Flow, the existing Energy is
     * preserved rather than wiped, since the person won't see what's already
     * there before saving (same "always starts blank" pattern as the rest of
     * the app, so nothing gets silently lost by leaving a field untouched).
     */
    suspend fun saveDailyCycleEntry(
        cycleId: Long,
        date: String,
        flow: FlowLevel?,
        energy: EnergyLevel?,
        moods: List<Mood> = emptyList(),
    ) {
        if (flow == null && energy == null && moods.isEmpty()) return
        val existing = db.cycleDailyDao().getForCycleAndDate(cycleId, date)
        if (existing != null) {
            db.cycleDailyDao().update(
                existing.copy(
                    flow = flow?.name ?: existing.flow,
                    energy = energy?.name ?: existing.energy,
                    moods = if (moods.isNotEmpty()) encodeMoods(moods) else existing.moods,
                ),
            )
        } else {
            db.cycleDailyDao().insert(
                CycleDailyEntry(
                    cycleId = cycleId,
                    date = date,
                    flow = flow?.name,
                    energy = energy?.name,
                    moods = if (moods.isNotEmpty()) encodeMoods(moods) else null,
                ),
            )
        }
    }

    /** All days logged for a given cycle, oldest first — the running list shown on the Cycle screen and in History. */
    suspend fun getDailyCycleEntries(cycleId: Long): List<CycleDailyEntry> = db.cycleDailyDao().getForCycle(cycleId)

    suspend fun deleteDailyCycleEntry(id: Long) = db.cycleDailyDao().deleteById(id)

    // ── Weight ────────────────────────────────────────────────────────────────

    suspend fun getWeightUnit(): WeightUnit {
        val raw = db.preferenceDao().get(PREF_WEIGHT_UNIT)?.value ?: return WeightUnit.LBS
        return WeightUnit.entries.firstOrNull { it.name == raw } ?: WeightUnit.LBS
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        db.preferenceDao().set(PreferenceEntry(PREF_WEIGHT_UNIT, unit.name))
    }

    /** Weight tracking is opt-in and hidden by default, same as Cycle. */
    suspend fun getWeightFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_WEIGHT_ENABLED)?.value == "true"
    }

    suspend fun setWeightFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_WEIGHT_ENABLED, value.toString()))
    }

    suspend fun getStartingWeight(): StartingWeightEntry? = db.startingWeightDao().get()

    suspend fun setStartingWeight(weightKg: Double, date: String) {
        db.startingWeightDao().set(StartingWeightEntry(weightKg = weightKg, date = date))
    }

    suspend fun addWeightEntry(weightKg: Double, date: String = todayStr()) {
        db.weightDao().upsert(WeightEntry(date = date, weightKg = weightKg))
    }

    suspend fun getMostRecentWeightEntry(): WeightEntry? = db.weightDao().getMostRecent()

    suspend fun getWeightHistory(limit: Int = 100): List<WeightEntry> = db.weightDao().getRecent(limit)

    suspend fun deleteWeightEntry(date: String) = db.weightDao().deleteForDate(date)

    suspend fun resetWeight() {
        db.weightDao().resetAll()
        db.startingWeightDao().reset()
    }

    /**
     * Average weekly rate of change (signed — negative trending down,
     * positive trending up) from the starting weight to the most recently
     * logged entry. Deliberately reported as a single neutral "change" value
     * rather than separate "loss"/"gain" figures. Returns null if there
     * isn't enough data yet (no starting weight, no logged entry, or they
     * share the same date).
     */
    suspend fun getAverageWeeklyWeightChangeKg(): Double? {
        val start = db.startingWeightDao().get() ?: return null
        val latest = db.weightDao().getMostRecent() ?: return null
        if (latest.date == start.date) return null
        val startDate = LocalDate.parse(start.date, DateTimeFormatter.ISO_LOCAL_DATE)
        val latestDate = LocalDate.parse(latest.date, DateTimeFormatter.ISO_LOCAL_DATE)
        val days = ChronoUnit.DAYS.between(startDate, latestDate)
        if (days <= 0) return null
        val weeks = days / 7.0
        return (latest.weightKg - start.weightKg) / weeks
    }

    // ── Mood ──────────────────────────────────────────────────────────────────

    /** Mood tracking is opt-in and hidden by default, same as Cycle and Weight. */
    suspend fun getMoodFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_MOOD_ENABLED)?.value == "true"
    }

    // Water/Steps/Sleep are the app's original, near-universal features, so
    // unlike Cycle/Weight/Mood (opt-in, default off), these default to ON —
    // absent a stored preference, treat it as enabled rather than disabled.
    suspend fun getWaterFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_WATER_ENABLED)?.value != "false"
    }

    suspend fun setWaterFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_WATER_ENABLED, value.toString()))
    }

    suspend fun getStepsFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_STEPS_ENABLED)?.value != "false"
    }

    suspend fun setStepsFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_STEPS_ENABLED, value.toString()))
    }

    suspend fun getSleepFeatureEnabled(): Boolean {
        return db.preferenceDao().get(PREF_SLEEP_ENABLED)?.value != "false"
    }

    suspend fun setSleepFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_SLEEP_ENABLED, value.toString()))
    }

    suspend fun getTimeFormat(): TimeFormat {
        val raw = db.preferenceDao().get(PREF_TIME_FORMAT)?.value ?: return TimeFormat.AM_PM
        return TimeFormat.entries.firstOrNull { it.name == raw } ?: TimeFormat.AM_PM
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        db.preferenceDao().set(PreferenceEntry(PREF_TIME_FORMAT, format.name))
    }

    suspend fun getDateFormat(): DateFormat {
        val raw = db.preferenceDao().get(PREF_DATE_FORMAT)?.value
        val format = DateFormat.entries.firstOrNull { it.name == raw } ?: DateFormat.MDY
        DateFormatHolder.current = format
        return format
    }

    suspend fun setDateFormat(format: DateFormat) {
        db.preferenceDao().set(PreferenceEntry(PREF_DATE_FORMAT, format.name))
        DateFormatHolder.current = format
    }

    suspend fun setMoodFeatureEnabled(value: Boolean) {
        db.preferenceDao().set(PreferenceEntry(PREF_MOOD_ENABLED, value.toString()))
    }

    suspend fun addMoodEntry(date: String, moods: List<Mood>, notes: String?) {
        db.moodDao().insert(
            MoodEntry(date = date, moods = encodeMoods(moods), notes = notes?.takeIf { it.isNotBlank() }),
        )
    }

    suspend fun getMostRecentMoodEntry(): MoodEntry? = db.moodDao().getMostRecent()

    suspend fun getMostRecentMoodEntryForDate(date: String): MoodEntry? = db.moodDao().getMostRecentForDate(date)

    suspend fun getMoodHistory(limit: Int = 100): List<MoodEntry> = db.moodDao().getRecent(limit)

    suspend fun deleteMoodEntry(id: Long) = db.moodDao().deleteById(id)

    /**
     * Whether any Mood entries actually exist within a cycle's date range
     * (start through end, or through today if still ongoing) — used so
     * Cycle history can point to Mood history only when there's genuinely
     * something there, rather than guessing from the current toggle state.
     */
    suspend fun hasMoodEntriesInRange(startDate: String, endDate: String?): Boolean {
        val to = endDate ?: todayStr()
        return db.moodDao().countInRange(startDate, to) > 0
    }

    suspend fun resetMood() = db.moodDao().resetAll()

    /**
     * Whether [date] falls within any logged cycle's range (start date
     * through end date, or through today if the cycle is still ongoing).
     * Used to gently cross-reference Mood history entries for people who
     * track both, without requiring a second, duplicate mood entry per
     * cycle — see Cycle's own mood field, which is skipped entirely (in
     * favor of linking to this tracker) once Mood tracking is turned on.
     */
    suspend fun isDateWithinAnyCycle(date: String): Boolean {
        val target = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        val cycles = db.cycleDao().getRecent(50)
        return cycles.any { cycle ->
            val start = LocalDate.parse(cycle.startDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val end = cycle.endDate
                ?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
                ?: LocalDate.now()
            !target.isBefore(start) && !target.isAfter(end)
        }
    }

    // ── Reset all ─────────────────────────────────────────────────────────────

    suspend fun resetAll() {
        db.waterDao().resetAll()
        db.stepDao().resetAll()
        db.lapDao().resetAll()
        db.distanceDao().resetAll()
        db.timeDao().resetAll()
        db.sleepDao().resetAll()
        db.cycleDao().resetAll()
        db.cycleDailyDao().resetAll()
        db.weightDao().resetAll()
        db.startingWeightDao().reset()
        db.moodDao().resetAll()
    }
}
