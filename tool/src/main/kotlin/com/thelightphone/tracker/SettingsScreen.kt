package com.thelightphone.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val invertColors: Boolean = false,
    val waterTrackingEnabled: Boolean = true,
    val movementTrackingEnabled: Boolean = true,
    val primaryMovementCategory: MovementCategory = MovementCategory.STEPS,
    val visibleMovementCategories: List<MovementCategory> = MovementCategory.entries.toList(),
    val sleepTrackingEnabled: Boolean = true,
    val cycleTrackingEnabled: Boolean = false,
    val weightTrackingEnabled: Boolean = false,
    val moodTrackingEnabled: Boolean = false,
)

class SettingsViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = SettingsState(
                invertColors = repo.getInvertColors(),
                waterTrackingEnabled = repo.getWaterFeatureEnabled(),
                movementTrackingEnabled = repo.getMovementFeatureEnabled(),
                // Self-correcting: if Movement Type just hid whatever was
                // primary, this falls back (and persists that fallback)
                // rather than showing a now-invalid selection.
                primaryMovementCategory = repo.getPrimaryMovementCategorySelfCorrected(),
                visibleMovementCategories = repo.getVisibleMovementCategories(),
                sleepTrackingEnabled = repo.getSleepFeatureEnabled(),
                cycleTrackingEnabled = repo.getCycleFeatureEnabled(),
                weightTrackingEnabled = repo.getWeightFeatureEnabled(),
                moodTrackingEnabled = repo.getMoodFeatureEnabled(),
            )
        }
    }

    fun toggleInvertColors() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.invertColors
            repo.setInvertColors(newValue)
            _state.value = _state.value.copy(invertColors = newValue)
            if (newValue) LightThemeController.setLightTheme() else LightThemeController.setDarkTheme()
        }
    }

    fun toggleWaterTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.waterTrackingEnabled
            repo.setWaterFeatureEnabled(newValue)
            _state.value = _state.value.copy(waterTrackingEnabled = newValue)
        }
    }

    fun toggleMovementTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.movementTrackingEnabled
            repo.setMovementFeatureEnabled(newValue)
            _state.value = _state.value.copy(movementTrackingEnabled = newValue)
        }
    }

    fun setPrimaryMovementCategory(category: MovementCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setPrimaryMovementCategory(category)
            _state.value = _state.value.copy(primaryMovementCategory = category)
        }
    }

    fun toggleSleepTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.sleepTrackingEnabled
            repo.setSleepFeatureEnabled(newValue)
            _state.value = _state.value.copy(sleepTrackingEnabled = newValue)
        }
    }

    fun toggleCycleTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.cycleTrackingEnabled
            repo.setCycleFeatureEnabled(newValue)
            _state.value = _state.value.copy(cycleTrackingEnabled = newValue)
        }
    }

    fun toggleWeightTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.weightTrackingEnabled
            repo.setWeightFeatureEnabled(newValue)
            _state.value = _state.value.copy(weightTrackingEnabled = newValue)
        }
    }

    fun toggleMoodTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.moodTrackingEnabled
            repo.setMoodFeatureEnabled(newValue)
            _state.value = _state.value.copy(moodTrackingEnabled = newValue)
        }
    }

    fun resetAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.resetAll()
        }
    }
}

class SettingsScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, SettingsViewModel>(sealedActivity) {

    override val viewModelClass: Class<SettingsViewModel>
        get() = SettingsViewModel::class.java

    override fun createViewModel() = SettingsViewModel(repo)

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
                    center = LightTopBarCenter.Text("Settings"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    // Invert colors
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleInvertColors() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Invert colors",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        // TOGGLE_ON's artwork has its knob on the left; TOGGLE_OFF's
                        // knob is on the right. The screen is black (dark theme) when
                        // invertColors is false, so the knob should sit on the left
                        // in that state — i.e. show TOGGLE_ON when NOT inverted.
                        LightIcon(
                            icon = if (state.invertColors) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    // Track water/sleep — these default ON, unlike the
                    // trackers below, since they're the app's original,
                    // near-universal features. Controls whether their tiles
                    // show up on the Home screen. (Steps lives inside the
                    // Movement block below now, alongside Laps/Distance/Time.)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleWaterTracking() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Track water",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.waterTrackingEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    // Track movement — replaces the old standalone "Track
                    // steps" toggle. Turning this ON immediately opens the
                    // primary-category picker, since Movement can't show a
                    // sensible Home tile without one chosen. This is the one
                    // toggle in Settings that navigates elsewhere on tap —
                    // every other toggle here just flips in place.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val turningOn = !state.movementTrackingEnabled
                                viewModel.toggleMovementTracking()
                                if (turningOn) {
                                    navigateTo(
                                        screenFactory = {
                                            MovementCategoryPickerScreen(
                                                it,
                                                state.primaryMovementCategory,
                                                state.visibleMovementCategories,
                                            )
                                        },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setPrimaryMovementCategory(result)
                                        },
                                    )
                                }
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Track movement",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.movementTrackingEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    if (state.movementTrackingEnabled) {
                        // Displayed primary movement — which category's value
                        // shows on the Home tile. Only ever offers categories
                        // that are actually visible (see Movement Type).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateTo(
                                        screenFactory = {
                                            MovementCategoryPickerScreen(
                                                it,
                                                state.primaryMovementCategory,
                                                state.visibleMovementCategories,
                                            )
                                        },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setPrimaryMovementCategory(result)
                                        },
                                    )
                                }
                                .padding(
                                    start = 1.5f.gridUnitsAsDp(),
                                    top = 0.75f.gridUnitsAsDp(),
                                    bottom = 0.75f.gridUnitsAsDp(),
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                LightText(
                                    text = "Displayed primary movement",
                                    variant = LightTextVariant.Copy,
                                )
                                LightText(
                                    text = state.primaryMovementCategory.label,
                                    variant = LightTextVariant.Fine,
                                    lighten = true,
                                )
                            }
                            LightIcon(icon = LightIcons.ARROW_RIGHT)
                        }

                        // Movement Type — which of Steps/Laps/Distance/Time
                        // are tracked at all, moved to its own screen so this
                        // list doesn't get crowded with four separate toggles.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateTo(screenFactory = { MovementTypeScreen(it, repo) })
                                }
                                .padding(
                                    start = 1.5f.gridUnitsAsDp(),
                                    top = 0.75f.gridUnitsAsDp(),
                                    bottom = 0.75f.gridUnitsAsDp(),
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LightText(
                                text = "Movement Type",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.weight(1f),
                            )
                            LightIcon(icon = LightIcons.ARROW_RIGHT)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleSleepTracking() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Track sleep",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.sleepTrackingEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    // Track mood — opt-in, hidden by default. Controls whether
                    // the Mood tile shows up on the Home screen.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleMoodTracking() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Track mood",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.moodTrackingEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    // Track weight — opt-in, hidden by default, same reasoning
                    // as Cycle. Controls whether the Weight tile shows up on
                    // the Home screen.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleWeightTracking() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Track weight",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.weightTrackingEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    // Track menstrual cycle — opt-in, hidden by default since not
                    // everyone tracks a cycle. Controls whether the Cycle tile
                    // shows up on the Home screen.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleCycleTracking() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Track menstrual cycle",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.cycleTrackingEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    // Units & Formats — water/distance units, date/time
                    // formats, all grouped in one place rather than crowding
                    // this list with four separate rows.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateTo(screenFactory = { UnitsAndFormatsScreen(it, repo) })
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Units & Formats",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(icon = LightIcons.ARROW_RIGHT)
                    }

                    // Reset all data
                    LightText(
                        text = "Reset all data",
                        variant = LightTextVariant.Copy,
                        lighten = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateTo(
                                    screenFactory = {
                                        ConfirmResetScreen(
                                            it,
                                            "Reset all data? This will permanently clear all water, step, and sleep data.",
                                        )
                                    },
                                    resultCallback = { confirmed ->
                                        if (confirmed == true) viewModel.resetAll()
                                    },
                                )
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                    )
                }

                LightBottomBar(items = listOf())
            }
        }
    }
}
