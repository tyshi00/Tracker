package com.thelightphone.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SleepState(
    // Non-null once an in-progress entry (bedtime logged, wake not yet) is
    // either loaded on open or started during this visit — completing that
    // same entry rather than starting a new one. Mirrors Cycle's editingId.
    val editingId: Long? = null,

    // Sleep and wake each get their own explicit date, rather than one
    // shared date with an inferred "next day" — clearer to read back, and
    // it also just works for same-day naps without any special-casing.
    val sleepDate: String = previousDayStr(),
    val sleepHour: String = "",
    val sleepMinute: String = "",
    val sleepAmPm: AmPm? = null, // only used when timeFormat is AM_PM
    val wakeDate: String = todayStr(),
    val wakeHour: String = "",
    val wakeMinute: String = "",
    val wakeAmPm: AmPm? = null,
    // Refreshed every time the screen shows (an external setting, not
    // something edited here), same reasoning as Cycle's moodFeatureEnabled.
    val timeFormat: TimeFormat = TimeFormat.AM_PM,
    val weeklyAvgDisplay: String = "0h 0m",
    val monthlyAvgDisplay: String = "0h 0m",
    val showSaved: Boolean = false,
    // One-shot signal for the Composable to scroll down and reveal the
    // Saved confirmation — only for a bedtime-only save, since the fields
    // stay filled in (not cleared) in that case, so without this the person
    // has no visual sign anything happened unless they scroll manually.
    val justLoggedBedtimeOnly: Boolean = false,
)

class SleepViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    private val dbMutex = Mutex()
    private var savedToken = 0L

    // Only check for an in-progress entry to resume the FIRST time this
    // screen is shown — later onScreenShow calls (returning from the
    // date/hour/minute/AM-PM pickers) must not re-run this, or a stale DB
    // read would clobber whatever's mid-edit. Same "load once" reasoning as
    // Cycle/Weight.
    private var loadedInitialState = false

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                val format = repo.getTimeFormat()
                if (!loadedInitialState) {
                    loadedInitialState = true
                    loadIncompleteEntryIfAny(format)
                }
                _state.value = _state.value.copy(
                    timeFormat = format,
                    weeklyAvgDisplay = formatSleep(repo.getWeeklyAvgSleepMinutes()),
                    monthlyAvgDisplay = formatSleep(repo.getMonthlyAvgSleepMinutes()),
                )
            }
        }
    }

    private suspend fun loadIncompleteEntryIfAny(format: TimeFormat) {
        val incomplete = repo.getIncompleteSleepEntry() ?: return
        val (hour, minute, ampm) = decomposeClockMinutes(incomplete.bedTimeMinutes, format)
        _state.value = _state.value.copy(
            editingId = incomplete.id,
            sleepDate = incomplete.bedDate,
            sleepHour = hour,
            sleepMinute = minute,
            sleepAmPm = ampm,
        )
    }

    fun setSleepDate(date: String) {
        _state.value = _state.value.copy(sleepDate = date)
    }

    fun setSleepHour(value: String) {
        _state.value = _state.value.copy(sleepHour = value)
    }

    fun setSleepMinute(value: String) {
        _state.value = _state.value.copy(sleepMinute = value)
    }

    fun setSleepAmPm(value: AmPm) {
        _state.value = _state.value.copy(sleepAmPm = value)
    }

    fun setWakeDate(date: String) {
        _state.value = _state.value.copy(wakeDate = date)
    }

    fun setWakeHour(value: String) {
        _state.value = _state.value.copy(wakeHour = value)
    }

    fun setWakeMinute(value: String) {
        _state.value = _state.value.copy(wakeMinute = value)
    }

    fun setWakeAmPm(value: AmPm) {
        _state.value = _state.value.copy(wakeAmPm = value)
    }

    fun save() {
        val s = _state.value
        val sleepHour = s.sleepHour.toIntOrNull()
        val sleepMinute = s.sleepMinute.toIntOrNull()
        val sleepFilled = sleepHour != null && sleepMinute != null &&
            (s.timeFormat == TimeFormat.HOUR_24 || s.sleepAmPm != null)

        val wakeHour = s.wakeHour.toIntOrNull()
        val wakeMinute = s.wakeMinute.toIntOrNull()
        val wakeFilled = wakeHour != null && wakeMinute != null &&
            (s.timeFormat == TimeFormat.HOUR_24 || s.wakeAmPm != null)

        if (!sleepFilled && !wakeFilled) return

        val sleepMinutesOfDay = if (sleepFilled) {
            if (s.timeFormat == TimeFormat.AM_PM) {
                to24HourMinutes(sleepHour!!, sleepMinute!!, s.sleepAmPm!!)
            } else {
                to24HourFormatMinutes(sleepHour!!, sleepMinute!!)
            }
        } else {
            null
        }
        val wakeMinutesOfDay = if (wakeFilled) {
            if (s.timeFormat == TimeFormat.AM_PM) {
                to24HourMinutes(wakeHour!!, wakeMinute!!, s.wakeAmPm!!)
            } else {
                to24HourFormatMinutes(wakeHour!!, wakeMinute!!)
            }
        } else {
            null
        }

        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                if (sleepFilled && wakeFilled) {
                    val wakeDateResolved = if (wakeMinutesOfDay!! <= sleepMinutesOfDay!!) {
                        LocalDate.parse(s.sleepDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            .plusDays(1)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } else {
                        s.sleepDate
                    }
                    val duration = computeSleepDurationMinutes(
                        s.sleepDate, sleepMinutesOfDay, wakeDateResolved, wakeMinutesOfDay,
                    )
                    if (duration <= 0) return@withLock // wake wasn't actually after sleep

                    val editingId = s.editingId
                    if (editingId != null) {
                        repo.completeSleepEntry(editingId, s.sleepDate, sleepMinutesOfDay, wakeDateResolved, wakeMinutesOfDay)
                    } else {
                        repo.addSleepSession(s.sleepDate, sleepMinutesOfDay, wakeDateResolved, wakeMinutesOfDay)
                    }

                    // Complete — clear the whole form back to a fresh state.
                    _state.value = _state.value.copy(
                        editingId = null,
                        sleepDate = previousDayStr(),
                        sleepHour = "",
                        sleepMinute = "",
                        sleepAmPm = null,
                        wakeDate = todayStr(),
                        wakeHour = "",
                        wakeMinute = "",
                        wakeAmPm = null,
                        weeklyAvgDisplay = formatSleep(repo.getWeeklyAvgSleepMinutes()),
                        monthlyAvgDisplay = formatSleep(repo.getMonthlyAvgSleepMinutes()),
                    )
                } else if (sleepFilled) {
                    // Bedtime only — start a new in-progress entry, or correct
                    // one already in progress. Wake stays untouched either way.
                    val editingId = s.editingId
                    if (editingId != null) {
                        repo.updateSleepEntryBedSide(editingId, s.sleepDate, sleepMinutesOfDay!!)
                    } else {
                        val newId = repo.startSleepEntry(s.sleepDate, sleepMinutesOfDay!!)
                        _state.value = _state.value.copy(editingId = newId)
                    }
                    // Leave the Sleep fields as-is — they now reflect what's
                    // saved. Signal the Composable to scroll down so the
                    // Saved confirmation (and the fact wake time can be
                    // added later) is actually visible without having to
                    // scroll manually.
                    _state.value = _state.value.copy(justLoggedBedtimeOnly = true)
                } else {
                    // Wake filled with no bedtime on record at all — nothing to
                    // complete (this shouldn't normally happen, since an
                    // in-progress entry's bedtime gets loaded automatically).
                    return@withLock
                }

                val myToken = ++savedToken
                _state.value = _state.value.copy(showSaved = true)
                viewModelScope.launch {
                    delay(1500)
                    if (savedToken == myToken) {
                        _state.value = _state.value.copy(showSaved = false)
                    }
                }
            }
        }
    }

    /** Called once the Composable has handled the scroll — avoids re-triggering on later recompositions. */
    fun consumeScrollSignal() {
        _state.value = _state.value.copy(justLoggedBedtimeOnly = false)
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                repo.resetSleep()
                _state.value = SleepState(
                    timeFormat = _state.value.timeFormat,
                    weeklyAvgDisplay = "0h 0m",
                    monthlyAvgDisplay = "0h 0m",
                )
                loadedInitialState = false
            }
        }
    }
}

class SleepScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, SleepViewModel>(sealedActivity) {

    override val viewModelClass: Class<SleepViewModel>
        get() = SleepViewModel::class.java

    override fun createViewModel() = SleepViewModel(repo)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.state.collectAsState()
        val is24Hour = state.timeFormat == TimeFormat.HOUR_24
        val scrollState = androidx.compose.foundation.rememberScrollState()

        androidx.compose.runtime.LaunchedEffect(state.justLoggedBedtimeOnly) {
            if (state.justLoggedBedtimeOnly) {
                // "Saved" itself is a fixed element above the bottom bar,
                // always visible regardless of scroll position — no scroll
                // is needed to see it. What actually needs fixing here is
                // showing what was just logged: scroll back to the top so
                // the Sleep section (which now shows the saved bedtime) is
                // immediately in view, rather than leaving the person
                // wherever they happened to be scrolled to.
                scrollState.animateScrollTo(0)
                viewModel.consumeScrollSignal()
            }
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Sleep"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                    scrollState = scrollState,
                ) {
                    LightText(text = "Sleep", variant = LightTextVariant.Detail)
                    if (state.editingId != null) {
                        Spacer(modifier = Modifier.height(0.25f.gridUnitsAsDp()))
                        LightText(
                            text = "Bedtime already logged — add a wake time below to complete it.",
                            variant = LightTextVariant.Fine,
                            lighten = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))

                    LightTextField(
                        label = "Date",
                        value = dateLabel(state.sleepDate),
                        placeholder = "",
                        onClick = {
                            navigateTo(
                                screenFactory = { DatePickerScreen(it, state.sleepDate) },
                                resultCallback = { result -> if (result != null) viewModel.setSleepDate(result) },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        LightTextField(
                            label = if (is24Hour) "Hour (0-23)" else "Hour",
                            value = state.sleepHour,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(it, title = "Sleep hour", initialValue = state.sleepHour)
                                    },
                                    resultCallback = { result -> if (result != null) viewModel.setSleepHour(result) },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.weight(0.2f))
                        LightTextField(
                            label = "Minute",
                            value = state.sleepMinute,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(it, title = "Sleep minute", initialValue = state.sleepMinute)
                                    },
                                    resultCallback = { result -> if (result != null) viewModel.setSleepMinute(result) },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (!is24Hour) {
                            Spacer(modifier = Modifier.weight(0.2f))
                            LightTextField(
                                label = "AM/PM",
                                value = state.sleepAmPm?.label ?: "",
                                placeholder = "-",
                                onClick = {
                                    navigateTo(
                                        screenFactory = { AmPmPickerScreen(it, state.sleepAmPm) },
                                        resultCallback = { result -> if (result != null) viewModel.setSleepAmPm(result) },
                                    )
                                },
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))

                    LightText(text = "Wake", variant = LightTextVariant.Detail)
                    Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))

                    LightTextField(
                        label = "Date",
                        value = dateLabel(state.wakeDate),
                        placeholder = "",
                        onClick = {
                            navigateTo(
                                screenFactory = { DatePickerScreen(it, state.wakeDate) },
                                resultCallback = { result -> if (result != null) viewModel.setWakeDate(result) },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        LightTextField(
                            label = if (is24Hour) "Hour (0-23)" else "Hour",
                            value = state.wakeHour,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(it, title = "Wake hour", initialValue = state.wakeHour)
                                    },
                                    resultCallback = { result -> if (result != null) viewModel.setWakeHour(result) },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.weight(0.2f))
                        LightTextField(
                            label = "Minute",
                            value = state.wakeMinute,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(it, title = "Wake minute", initialValue = state.wakeMinute)
                                    },
                                    resultCallback = { result -> if (result != null) viewModel.setWakeMinute(result) },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (!is24Hour) {
                            Spacer(modifier = Modifier.weight(0.2f))
                            LightTextField(
                                label = "AM/PM",
                                value = state.wakeAmPm?.label ?: "",
                                placeholder = "-",
                                onClick = {
                                    navigateTo(
                                        screenFactory = { AmPmPickerScreen(it, state.wakeAmPm) },
                                        resultCallback = { result -> if (result != null) viewModel.setWakeAmPm(result) },
                                    )
                                },
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))

                    val sleepHourInt = state.sleepHour.toIntOrNull()
                    val sleepMinuteInt = state.sleepMinute.toIntOrNull()
                    val sleepAmPmValue = state.sleepAmPm
                    val wakeHourInt = state.wakeHour.toIntOrNull()
                    val wakeMinuteInt = state.wakeMinute.toIntOrNull()
                    val wakeAmPmValue = state.wakeAmPm

                    val sleepTimeReady = sleepHourInt != null && sleepMinuteInt != null &&
                        (is24Hour || sleepAmPmValue != null)
                    val wakeTimeReady = wakeHourInt != null && wakeMinuteInt != null &&
                        (is24Hour || wakeAmPmValue != null)

                    val sessionPreview = if (sleepTimeReady && wakeTimeReady) {
                        val sleepMinutesOfDay = if (is24Hour) {
                            to24HourFormatMinutes(sleepHourInt!!, sleepMinuteInt!!)
                        } else {
                            to24HourMinutes(sleepHourInt!!, sleepMinuteInt!!, sleepAmPmValue!!)
                        }
                        val wakeMinutesOfDay = if (is24Hour) {
                            to24HourFormatMinutes(wakeHourInt!!, wakeMinuteInt!!)
                        } else {
                            to24HourMinutes(wakeHourInt!!, wakeMinuteInt!!, wakeAmPmValue!!)
                        }
                        val duration = computeSleepDurationMinutes(
                            state.sleepDate, sleepMinutesOfDay, state.wakeDate, wakeMinutesOfDay,
                        )
                        if (duration > 0) formatSleep(duration) else "Check dates and times"
                    } else {
                        "Incomplete"
                    }
                    StatRow(label = "This session", value = sessionPreview)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    StatRow(label = "Weekly avg", value = state.weeklyAvgDisplay)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    StatRow(label = "Monthly avg", value = state.monthlyAvgDisplay)
                }

                if (state.showSaved) {
                    LightText(
                        text = "Saved",
                        variant = LightTextVariant.Fine,
                        lighten = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp(), vertical = 0.5f.gridUnitsAsDp()),
                    )
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SAVE,
                            onClick = { viewModel.save() },
                            contentDescription = "Save",
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.LIST,
                            onClick = { navigateTo(screenFactory = { SleepHistoryScreen(it, repo) }) },
                            contentDescription = "History",
                        ),
                        LightBarButton.Text(
                            text = "RESET",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        ConfirmResetScreen(
                                            it,
                                            "Reset all sleep data? This will clear weekly and monthly averages.",
                                        )
                                    },
                                    resultCallback = { confirmed ->
                                        if (confirmed == true) viewModel.reset()
                                    },
                                )
                            },
                        ),
                    ),
                )
            }
        }
    }
}
