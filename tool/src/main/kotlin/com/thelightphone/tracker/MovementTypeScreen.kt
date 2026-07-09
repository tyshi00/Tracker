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

data class MovementTypeState(
    val stepsEnabled: Boolean = true,
    val lapsEnabled: Boolean = true,
    val distanceEnabled: Boolean = true,
    val timeEnabled: Boolean = true,
)

class MovementTypeViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(MovementTypeState())
    val state: StateFlow<MovementTypeState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = MovementTypeState(
                stepsEnabled = repo.getStepsFeatureEnabled(),
                lapsEnabled = repo.getLapsFeatureEnabled(),
                distanceEnabled = repo.getDistanceFeatureEnabled(),
                timeEnabled = repo.getTimeFeatureEnabled(),
            )
        }
    }

    fun toggleSteps() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.stepsEnabled
            repo.setStepsFeatureEnabled(newValue)
            _state.value = _state.value.copy(stepsEnabled = newValue)
        }
    }

    fun toggleLaps() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.lapsEnabled
            repo.setLapsFeatureEnabled(newValue)
            _state.value = _state.value.copy(lapsEnabled = newValue)
        }
    }

    fun toggleDistance() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.distanceEnabled
            repo.setDistanceFeatureEnabled(newValue)
            _state.value = _state.value.copy(distanceEnabled = newValue)
        }
    }

    fun toggleTime() {
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = !_state.value.timeEnabled
            repo.setTimeFeatureEnabled(newValue)
            _state.value = _state.value.copy(timeEnabled = newValue)
        }
    }
}

class MovementTypeScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, MovementTypeViewModel>(sealedActivity) {

    override val viewModelClass: Class<MovementTypeViewModel>
        get() = MovementTypeViewModel::class.java

    override fun createViewModel() = MovementTypeViewModel(repo)

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
                    center = LightTopBarCenter.Text("Movement Type"),
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
                            .clickable { viewModel.toggleSteps() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Steps",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.stepsEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleLaps() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Laps",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.lapsEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDistance() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Distance",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.distanceEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleTime() }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LightText(
                            text = "Time",
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.weight(1f),
                        )
                        LightIcon(
                            icon = if (state.timeEnabled) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
                        )
                    }
                }

                LightBottomBar(items = listOf())
            }
        }
    }
}
