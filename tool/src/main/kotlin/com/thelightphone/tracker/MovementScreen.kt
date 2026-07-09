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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MovementState(
    val selectedDate: String = todayStr(),

    // Visibility is refreshed every time the screen shows (external
    // settings, not edited here) — same reasoning as Cycle's
    // moodFeatureEnabled.
    val stepsVisible: Boolean = true,
    val lapsVisible: Boolean = true,
    val distanceVisible: Boolean = true,
    val timeVisible: Boolean = true,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,

    val stepsValue: String = "",
    val stepsWeeklyDisplay: String = "0",
    val stepsMonthlyDisplay: String = "0",

    val lapsValue: String = "",
    val lapsWeeklyDisplay: String = "0",
    val lapsMonthlyDisplay: String = "0",

    val distanceValue: String = "",
    val distanceWeeklyDisplay: String = "0",
    val distanceMonthlyDisplay: String = "0",

    val timeHours: String = "",
    val timeMinutes: String = "",
    val timeWeeklyDisplay: String = "0h 0m",
    val timeMonthlyDisplay: String = "0h 0m",

    val showSaved: Boolean = false,
)

class MovementViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(MovementState())
    val state: StateFlow<MovementState> = _state.asStateFlow()

    private val dbMutex = Mutex()
    private var savedToken = 0L

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                val unit = repo.getDistanceUnit()
                _state.value = _state.value.copy(
                    stepsVisible = repo.getStepsFeatureEnabled(),
                    lapsVisible = repo.getLapsFeatureEnabled(),
                    distanceVisible = repo.getDistanceFeatureEnabled(),
                    timeVisible = repo.getTimeFeatureEnabled(),
                    distanceUnit = unit,
                    stepsWeeklyDisplay = "%,d".format(repo.getWeeklySteps()),
                    stepsMonthlyDisplay = "%,d".format(repo.getMonthlySteps()),
                    lapsWeeklyDisplay = "%,d".format(repo.getWeeklyLaps()),
                    lapsMonthlyDisplay = "%,d".format(repo.getMonthlyLaps()),
                    distanceWeeklyDisplay = DistanceConversion.format(repo.getWeeklyDistanceMeters(), unit),
                    distanceMonthlyDisplay = DistanceConversion.format(repo.getMonthlyDistanceMeters(), unit),
                    timeWeeklyDisplay = formatSleep(repo.getWeeklyTimeMinutes()),
                    timeMonthlyDisplay = formatSleep(repo.getMonthlyTimeMinutes()),
                )
            }
        }
    }

    fun setDate(date: String) {
        _state.value = _state.value.copy(selectedDate = date)
    }

    fun setStepsValue(value: String) {
        _state.value = _state.value.copy(stepsValue = value)
    }

    fun setLapsValue(value: String) {
        _state.value = _state.value.copy(lapsValue = value)
    }

    fun setDistanceValue(value: String) {
        _state.value = _state.value.copy(distanceValue = value)
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                repo.setDistanceUnit(unit)
                _state.value = _state.value.copy(
                    distanceUnit = unit,
                    distanceWeeklyDisplay = DistanceConversion.format(repo.getWeeklyDistanceMeters(), unit),
                    distanceMonthlyDisplay = DistanceConversion.format(repo.getMonthlyDistanceMeters(), unit),
                )
            }
        }
    }

    fun setTimeHours(value: String) {
        _state.value = _state.value.copy(timeHours = value)
    }

    fun setTimeMinutes(value: String) {
        _state.value = _state.value.copy(timeMinutes = value)
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                var savedAnything = false

                s.stepsValue.toIntOrNull()?.takeIf { it > 0 }?.let {
                    repo.addSteps(it, s.selectedDate)
                    savedAnything = true
                }
                s.lapsValue.toIntOrNull()?.takeIf { it > 0 }?.let {
                    repo.addLaps(it, s.selectedDate)
                    savedAnything = true
                }
                s.distanceValue.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                    repo.addDistance(DistanceConversion.toMeters(it, s.distanceUnit), s.selectedDate)
                    savedAnything = true
                }
                val hours = s.timeHours.toIntOrNull() ?: 0
                val minutes = s.timeMinutes.toIntOrNull() ?: 0
                val totalTimeMinutes = hours * 60 + minutes
                if (totalTimeMinutes > 0) {
                    repo.addTime(totalTimeMinutes, s.selectedDate)
                    savedAnything = true
                }

                if (!savedAnything) return@withLock

                val unit = s.distanceUnit
                _state.value = _state.value.copy(
                    selectedDate = todayStr(),
                    stepsValue = "",
                    lapsValue = "",
                    distanceValue = "",
                    timeHours = "",
                    timeMinutes = "",
                    stepsWeeklyDisplay = "%,d".format(repo.getWeeklySteps()),
                    stepsMonthlyDisplay = "%,d".format(repo.getMonthlySteps()),
                    lapsWeeklyDisplay = "%,d".format(repo.getWeeklyLaps()),
                    lapsMonthlyDisplay = "%,d".format(repo.getMonthlyLaps()),
                    distanceWeeklyDisplay = DistanceConversion.format(repo.getWeeklyDistanceMeters(), unit),
                    distanceMonthlyDisplay = DistanceConversion.format(repo.getMonthlyDistanceMeters(), unit),
                    timeWeeklyDisplay = formatSleep(repo.getWeeklyTimeMinutes()),
                    timeMonthlyDisplay = formatSleep(repo.getMonthlyTimeMinutes()),
                )

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

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                repo.resetSteps()
                repo.resetLaps()
                repo.resetDistance()
                repo.resetTime()
                _state.value = MovementState(
                    stepsVisible = _state.value.stepsVisible,
                    lapsVisible = _state.value.lapsVisible,
                    distanceVisible = _state.value.distanceVisible,
                    timeVisible = _state.value.timeVisible,
                    distanceUnit = _state.value.distanceUnit,
                )
            }
        }
    }
}

class MovementScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, MovementViewModel>(sealedActivity) {

    override val viewModelClass: Class<MovementViewModel>
        get() = MovementViewModel::class.java

    override fun createViewModel() = MovementViewModel(repo)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.state.collectAsState()

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
                    center = LightTopBarCenter.Text("Movement"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    LightTextField(
                        label = "Date",
                        value = dateLabel(state.selectedDate),
                        placeholder = "",
                        onClick = {
                            navigateTo(
                                screenFactory = { DatePickerScreen(it, state.selectedDate) },
                                resultCallback = { result -> if (result != null) viewModel.setDate(result) },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (state.stepsVisible) {
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                        LightText(text = "Steps", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        LightTextField(
                            label = "Steps",
                            value = state.stepsValue,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(it, title = "Steps", initialValue = state.stepsValue)
                                    },
                                    resultCallback = { result -> if (result != null) viewModel.setStepsValue(result) },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                        StatRow(label = "Steps weekly total", value = state.stepsWeeklyDisplay)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Steps monthly total", value = state.stepsMonthlyDisplay)
                    }

                    if (state.lapsVisible) {
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                        LightText(text = "Laps", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        LightTextField(
                            label = "Laps",
                            value = state.lapsValue,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(it, title = "Laps", initialValue = state.lapsValue)
                                    },
                                    resultCallback = { result -> if (result != null) viewModel.setLapsValue(result) },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                        StatRow(label = "Laps weekly total", value = state.lapsWeeklyDisplay)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Laps monthly total", value = state.lapsMonthlyDisplay)
                    }

                    if (state.distanceVisible) {
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                        LightText(text = "Distance", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            LightTextField(
                                label = "Distance",
                                value = state.distanceValue,
                                placeholder = "0",
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            NumberEditorScreen(
                                                it,
                                                title = "Distance",
                                                initialValue = state.distanceValue,
                                                isDecimal = true,
                                            )
                                        },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setDistanceValue(result)
                                        },
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.weight(0.2f))
                            LightTextField(
                                label = "Unit",
                                value = state.distanceUnit.label,
                                placeholder = "",
                                onClick = {
                                    navigateTo(
                                        screenFactory = { DistanceUnitPickerScreen(it, state.distanceUnit) },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setDistanceUnit(result)
                                        },
                                    )
                                },
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                        Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                        StatRow(
                            label = "Distance weekly total",
                            value = "${state.distanceWeeklyDisplay} ${state.distanceUnit.label}",
                        )
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(
                            label = "Distance monthly total",
                            value = "${state.distanceMonthlyDisplay} ${state.distanceUnit.label}",
                        )
                    }

                    if (state.timeVisible) {
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                        LightText(text = "Time", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            LightTextField(
                                label = "Hours",
                                value = state.timeHours,
                                placeholder = "0",
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            NumberEditorScreen(it, title = "Hours", initialValue = state.timeHours)
                                        },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setTimeHours(result)
                                        },
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.weight(0.2f))
                            LightTextField(
                                label = "Minutes",
                                value = state.timeMinutes,
                                placeholder = "0",
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            NumberEditorScreen(it, title = "Minutes", initialValue = state.timeMinutes)
                                        },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setTimeMinutes(result)
                                        },
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                        StatRow(label = "Time weekly total", value = state.timeWeeklyDisplay)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Time monthly total", value = state.timeMonthlyDisplay)
                    }
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
                            onClick = { navigateTo(screenFactory = { MovementHistoryScreen(it, repo) }) },
                            contentDescription = "History",
                        ),
                        LightBarButton.Text(
                            text = "RESET",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        ConfirmResetScreen(
                                            it,
                                            "Reset all movement data? This clears Steps, Laps, Distance, and Time — weekly and monthly totals included.",
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
