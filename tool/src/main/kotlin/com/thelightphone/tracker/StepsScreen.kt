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

data class StepsState(
    val stepsValue: String = "",
    val weeklyDisplay: String = "0",
    val monthlyDisplay: String = "0",
)

class StepsViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(StepsState())
    val state: StateFlow<StepsState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                weeklyDisplay = "%,d".format(repo.getWeeklySteps()),
                monthlyDisplay = "%,d".format(repo.getMonthlySteps()),
            )
        }
    }

    fun setSteps(value: String) {
        _state.value = _state.value.copy(stepsValue = value)
    }

    fun save() {
        val steps = _state.value.stepsValue.toIntOrNull() ?: return
        if (steps <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            repo.addSteps(steps)
            _state.value = _state.value.copy(stepsValue = "")
            reload()
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.resetSteps()
            _state.value = _state.value.copy(
                stepsValue = "",
                weeklyDisplay = "0",
                monthlyDisplay = "0",
            )
        }
    }
}

class StepsScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, StepsViewModel>(sealedActivity) {

    override val viewModelClass: Class<StepsViewModel>
        get() = StepsViewModel::class.java

    override fun createViewModel() = StepsViewModel(repo)

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
                    center = LightTopBarCenter.Text("Steps", sizeAdjustment = 15f),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    LightTextField(
                        label = "Steps today",
                        value = state.stepsValue,
                        placeholder = "0",
                        onClick = {
                            navigateTo(
                                screenFactory = {
                                    NumberEditorScreen(
                                        it,
                                        title = "Steps",
                                        initialValue = state.stepsValue,
                                        isDecimal = true,
                                    )
                                },
                                resultCallback = { result ->
                                    if (result != null) viewModel.setSteps(result)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))

                    StatRow(label = "Weekly total", value = state.weeklyDisplay)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    StatRow(label = "Monthly total", value = state.monthlyDisplay)
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = "SAVE",
                            onClick = { viewModel.save() },
                        ),
                        LightBarButton.Text(
                            text = "RESET STEPS",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        ConfirmResetScreen(
                                            it,
                                            "Reset all step data? This will clear weekly and monthly totals.",
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
