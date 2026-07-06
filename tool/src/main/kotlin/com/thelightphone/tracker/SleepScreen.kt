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
import com.thelightphone.sdk.ui.LightTextField
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

data class SleepState(
    val hoursValue: String = "",
    val minutesValue: String = "",
    val weeklyAvgDisplay: String = "0h 0m",
    val monthlyAvgDisplay: String = "0h 0m",
)

class SleepViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                weeklyAvgDisplay = formatSleep(repo.getWeeklyAvgSleepMinutes()),
                monthlyAvgDisplay = formatSleep(repo.getMonthlyAvgSleepMinutes()),
            )
        }
    }

    fun setHours(value: String) {
        _state.value = _state.value.copy(hoursValue = value)
    }

    fun setMinutes(value: String) {
        _state.value = _state.value.copy(minutesValue = value)
    }

    fun save() {
        val h = _state.value.hoursValue.toIntOrNull() ?: 0
        val m = _state.value.minutesValue.toIntOrNull() ?: 0
        if (h == 0 && m == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            repo.addSleep(h, m)
            _state.value = _state.value.copy(hoursValue = "", minutesValue = "")
            reload()
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.resetSleep()
            _state.value = _state.value.copy(
                hoursValue = "",
                minutesValue = "",
                weeklyAvgDisplay = "0h 0m",
                monthlyAvgDisplay = "0h 0m",
            )
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
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        LightTextField(
                            label = "Hours",
                            value = state.hoursValue,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(
                                            it,
                                            title = "Hours",
                                            initialValue = state.hoursValue,
                                        )
                                    },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setHours(result)
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )

                        Spacer(modifier = Modifier.weight(0.3f))

                        LightTextField(
                            label = "Minutes",
                            value = state.minutesValue,
                            placeholder = "0",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        NumberEditorScreen(
                                            it,
                                            title = "Minutes",
                                            initialValue = state.minutesValue,
                                        )
                                    },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setMinutes(result)
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))

                    StatRow(label = "Weekly avg", value = state.weeklyAvgDisplay)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    StatRow(label = "Monthly avg", value = state.monthlyAvgDisplay)
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = "SAVE",
                            onClick = { viewModel.save() },
                        ),
                        LightBarButton.Text(
                            text = "RESET SLEEP",
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
