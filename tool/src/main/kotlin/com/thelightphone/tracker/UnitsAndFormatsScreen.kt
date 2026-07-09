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

data class UnitsAndFormatsState(
    val waterUnit: WaterUnit = WaterUnit.ML,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val dateFormat: DateFormat = DateFormat.MDY,
    val timeFormat: TimeFormat = TimeFormat.AM_PM,
)

class UnitsAndFormatsViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(UnitsAndFormatsState())
    val state: StateFlow<UnitsAndFormatsState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = UnitsAndFormatsState(
                waterUnit = repo.getWaterUnit(),
                distanceUnit = repo.getDistanceUnit(),
                dateFormat = repo.getDateFormat(),
                timeFormat = repo.getTimeFormat(),
            )
        }
    }

    fun setWaterUnit(unit: WaterUnit) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setWaterUnit(unit)
            _state.value = _state.value.copy(waterUnit = unit)
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setDistanceUnit(unit)
            _state.value = _state.value.copy(distanceUnit = unit)
        }
    }

    fun setDateFormat(format: DateFormat) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setDateFormat(format)
            _state.value = _state.value.copy(dateFormat = format)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setTimeFormat(format)
            _state.value = _state.value.copy(timeFormat = format)
        }
    }
}

class UnitsAndFormatsScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, UnitsAndFormatsViewModel>(sealedActivity) {

    override val viewModelClass: Class<UnitsAndFormatsViewModel>
        get() = UnitsAndFormatsViewModel::class.java

    override fun createViewModel() = UnitsAndFormatsViewModel(repo)

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
                    center = LightTopBarCenter.Text("Units & Formats"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateTo(
                                    screenFactory = { UnitPickerScreen(it, state.waterUnit) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setWaterUnit(result)
                                    },
                                )
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LightText(
                                text = "Default water unit",
                                variant = LightTextVariant.Copy,
                            )
                            LightText(
                                text = state.waterUnit.label,
                                variant = LightTextVariant.Fine,
                                lighten = true,
                            )
                        }
                        LightIcon(icon = LightIcons.ARROW_RIGHT)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateTo(
                                    screenFactory = { DistanceUnitPickerScreen(it, state.distanceUnit) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setDistanceUnit(result)
                                    },
                                )
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LightText(
                                text = "Default distance unit",
                                variant = LightTextVariant.Copy,
                            )
                            LightText(
                                text = state.distanceUnit.label,
                                variant = LightTextVariant.Fine,
                                lighten = true,
                            )
                        }
                        LightIcon(icon = LightIcons.ARROW_RIGHT)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateTo(
                                    screenFactory = { DateFormatPickerScreen(it, state.dateFormat) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setDateFormat(result)
                                    },
                                )
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LightText(
                                text = "Default date format",
                                variant = LightTextVariant.Copy,
                            )
                            LightText(
                                text = state.dateFormat.label,
                                variant = LightTextVariant.Fine,
                                lighten = true,
                            )
                        }
                        LightIcon(icon = LightIcons.ARROW_RIGHT)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateTo(
                                    screenFactory = { TimeFormatPickerScreen(it, state.timeFormat) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setTimeFormat(result)
                                    },
                                )
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            LightText(
                                text = "Default time format",
                                variant = LightTextVariant.Copy,
                            )
                            LightText(
                                text = state.timeFormat.label,
                                variant = LightTextVariant.Fine,
                                lighten = true,
                            )
                        }
                        LightIcon(icon = LightIcons.ARROW_RIGHT)
                    }
                }

                LightBottomBar(items = listOf())
            }
        }
    }
}
