package com.thelightphone.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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

data class MovementHistoryState(
    val stepsVisible: Boolean = true,
    val stepsPreviousMonth: String = "0",
    val stepsYearlyAvg: String = "0",
    val lapsVisible: Boolean = true,
    val lapsPreviousMonth: String = "0",
    val lapsYearlyAvg: String = "0",
    val distanceVisible: Boolean = true,
    val distancePreviousMonth: String = "0",
    val distanceYearlyAvg: String = "0",
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val timeVisible: Boolean = true,
    val timePreviousMonth: String = "0h 0m",
    val timeYearlyAvg: String = "0h 0m",
)

class MovementHistoryViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(MovementHistoryState())
    val state: StateFlow<MovementHistoryState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            val unit = repo.getDistanceUnit()
            _state.value = MovementHistoryState(
                stepsVisible = repo.getStepsFeatureEnabled(),
                stepsPreviousMonth = "%,d".format(repo.getPreviousMonthSteps()),
                stepsYearlyAvg = "%,d".format(repo.getYearlyAverageSteps()),
                lapsVisible = repo.getLapsFeatureEnabled(),
                lapsPreviousMonth = "%,d".format(repo.getPreviousMonthLaps()),
                lapsYearlyAvg = "%,d".format(repo.getYearlyAverageLaps()),
                distanceVisible = repo.getDistanceFeatureEnabled(),
                distancePreviousMonth = DistanceConversion.format(repo.getPreviousMonthDistanceMeters(), unit),
                distanceYearlyAvg = DistanceConversion.format(repo.getYearlyAverageDistanceMeters(), unit),
                distanceUnit = unit,
                timeVisible = repo.getTimeFeatureEnabled(),
                timePreviousMonth = formatSleep(repo.getPreviousMonthTimeMinutes()),
                timeYearlyAvg = formatSleep(repo.getYearlyAverageTimeMinutes()),
            )
        }
    }
}

class MovementHistoryScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, MovementHistoryViewModel>(sealedActivity) {

    override val viewModelClass: Class<MovementHistoryViewModel>
        get() = MovementHistoryViewModel::class.java

    override fun createViewModel() = MovementHistoryViewModel(repo)

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
                    center = LightTopBarCenter.Text("Movement history"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    if (state.stepsVisible) {
                        LightText(text = "Steps", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Previous month total", value = state.stepsPreviousMonth)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Yearly avg (per month)", value = state.stepsYearlyAvg)
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                    }

                    if (state.lapsVisible) {
                        LightText(text = "Laps", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Previous month total", value = state.lapsPreviousMonth)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Yearly avg (per month)", value = state.lapsYearlyAvg)
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                    }

                    if (state.distanceVisible) {
                        LightText(text = "Distance", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(
                            label = "Previous month total",
                            value = "${state.distancePreviousMonth} ${state.distanceUnit.label}",
                        )
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(
                            label = "Yearly avg (per month)",
                            value = "${state.distanceYearlyAvg} ${state.distanceUnit.label}",
                        )
                        Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))
                    }

                    if (state.timeVisible) {
                        LightText(text = "Time", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Previous month total", value = state.timePreviousMonth)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                        StatRow(label = "Yearly avg (per month)", value = state.timeYearlyAvg)
                    }
                }

                LightBottomBar(items = listOf())
            }
        }
    }
}
