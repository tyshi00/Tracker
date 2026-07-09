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

data class CycleDailyDisplay(
    val id: Long,
    val dateDisplay: String,
    val flowLabel: String,
    val energyLabel: String,
    val moodLabel: String,
)

data class CycleState(
    val editingId: Long? = null,
    val startDate: String = todayStr(),
    val endDate: String? = null,
    val nextExpectedDisplay: String = "Not enough data yet",
    val lastCycleDisplay: String = "None logged",
    // Whether the standalone Mood tracker is on — checked every screen show
    // (not just once) since it's an external setting, not something this
    // screen edits itself. When true, the daily Mood field is replaced by a
    // shortcut into the Mood tracker instead, to avoid asking someone who
    // tracks both to log mood twice during the same days.
    val moodFeatureEnabled: Boolean = false,
    // Flow/Energy/Mood are all logged per day within the ongoing cycle,
    // since flow especially can vary a lot day to day — these fields are
    // only usable (and only shown) while a cycle is actually in progress.
    val dailyDate: String = todayStr(),
    val dailyFlow: FlowLevel? = null,
    val dailyEnergy: EnergyLevel? = null,
    val dailyMoods: List<Mood> = emptyList(), // only used when moodFeatureEnabled is false
    val dailyEntries: List<CycleDailyDisplay> = emptyList(),
    val showSaved: Boolean = false,
)

class CycleViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(CycleState())
    val state: StateFlow<CycleState> = _state.asStateFlow()

    // Guards save/reset/reload so they can't race each other.
    private val dbMutex = Mutex()

    // Only load the editable fields (start/end date, mood) from the database
    // the FIRST time this screen is shown. On every later onScreenShow (e.g.
    // returning from the date/mood/flow/energy pickers), we only refresh the
    // read-only stat displays — otherwise a stale DB read would stomp over
    // the selection the person just made before they had a chance to tap
    // SAVE. The daily-entry input fields (date/flow/energy) always start
    // fresh, same reasoning as Weight's Current-weight field.
    private var loadedInitialState = false

    // Guards against a slower, earlier save's delayed "hide" wiping out a
    // more recent save's still-visible confirmation.
    private var savedToken = 0L

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                if (!loadedInitialState) {
                    loadEditableFieldsFromDb()
                    loadedInitialState = true
                }
                refreshStatsOnly()
            }
        }
    }

    private suspend fun loadEditableFieldsFromDb() {
        val recent = repo.getMostRecentCycle()
        // If the most recent cycle has no end date yet, treat it as still in
        // progress and keep editing that same record rather than logging a
        // brand new one every time details are added.
        val editing = recent?.takeIf { it.endDate == null }

        _state.value = _state.value.copy(
            editingId = editing?.id,
            startDate = editing?.startDate ?: todayStr(),
            endDate = editing?.endDate,
        )
    }

    private suspend fun refreshStatsOnly() {
        val recent = repo.getMostRecentCycle()
        val next = repo.predictNextCycleStart()
        val editingId = _state.value.editingId
        _state.value = _state.value.copy(
            nextExpectedDisplay = next?.let { dateLabel(it) } ?: "Not enough data yet",
            lastCycleDisplay = recent?.let {
                dateLabel(it.startDate) + (it.endDate?.let { end -> " – ${dateLabel(end)}" } ?: " (ongoing)")
            } ?: "None logged",
            moodFeatureEnabled = repo.getMoodFeatureEnabled(),
            dailyEntries = if (editingId != null) buildDailyDisplayList(editingId) else emptyList(),
        )
    }

    private suspend fun buildDailyDisplayList(cycleId: Long): List<CycleDailyDisplay> {
        return repo.getDailyCycleEntries(cycleId).map { entry ->
            CycleDailyDisplay(
                id = entry.id,
                dateDisplay = dateLabel(entry.date),
                flowLabel = entry.flow
                    ?.let { name -> FlowLevel.entries.firstOrNull { it.name == name }?.label }
                    ?: "Not set",
                energyLabel = entry.energy
                    ?.let { name -> EnergyLevel.entries.firstOrNull { it.name == name }?.label }
                    ?: "Not set",
                moodLabel = decodeMoods(entry.moods)
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { it.label }
                    ?: "Not set",
            )
        }
    }

    fun setStartDate(date: String) {
        _state.value = _state.value.copy(startDate = date)
    }

    fun setEndDate(date: String) {
        _state.value = _state.value.copy(endDate = date)
    }

    fun setDailyMoods(moods: List<Mood>) {
        _state.value = _state.value.copy(dailyMoods = moods)
    }

    fun setDailyDate(date: String) {
        _state.value = _state.value.copy(dailyDate = date)
    }

    fun setDailyFlow(flow: FlowLevel) {
        _state.value = _state.value.copy(dailyFlow = flow)
    }

    fun setDailyEnergy(energy: EnergyLevel) {
        _state.value = _state.value.copy(dailyEnergy = energy)
    }

    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            dbMutex.withLock {
                val s = _state.value
                repo.saveCycle(
                    id = s.editingId,
                    startDate = s.startDate,
                    endDate = s.endDate,
                )

                // Pick up the id if this save just inserted a new row (so
                // further edits before leaving the screen update it rather
                // than inserting duplicates).
                val recent = repo.getMostRecentCycle()
                val editing = recent?.takeIf { it.endDate == null }
                val cycleId = editing?.id

                // Daily Flow/Energy/Mood only make sense while the cycle is
                // still ongoing — if this same save just set an end date,
                // skip it. Mood only saves here when the standalone tracker
                // is off — when it's on, dailyMoods is never populated in
                // the first place (the field is a shortcut elsewhere then).
                val dailyMoodsToSave = if (!s.moodFeatureEnabled) s.dailyMoods else emptyList()
                if (cycleId != null && (s.dailyFlow != null || s.dailyEnergy != null || dailyMoodsToSave.isNotEmpty())) {
                    repo.saveDailyCycleEntry(cycleId, s.dailyDate, s.dailyFlow, s.dailyEnergy, dailyMoodsToSave)
                }

                _state.value = _state.value.copy(
                    editingId = cycleId,
                    dailyDate = todayStr(),
                    dailyFlow = null,
                    dailyEnergy = null,
                    dailyMoods = emptyList(),
                )
                refreshStatsOnly()

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
                repo.resetCycles()
                _state.value = CycleState()
                // Force the next onScreenShow to re-derive from the (now
                // empty) database instead of treating this as still loaded.
                loadedInitialState = false
            }
        }
    }
}

class CycleScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, CycleViewModel>(sealedActivity) {

    override val viewModelClass: Class<CycleViewModel>
        get() = CycleViewModel::class.java

    override fun createViewModel() = CycleViewModel(repo)

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
                    center = LightTopBarCenter.Text("Cycle"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    LightTextField(
                        label = "Start date",
                        value = dateLabel(state.startDate),
                        placeholder = "",
                        onClick = {
                            navigateTo(
                                screenFactory = { DatePickerScreen(it, state.startDate) },
                                resultCallback = { result ->
                                    if (result != null) viewModel.setStartDate(result)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))

                    LightTextField(
                        label = "End date",
                        value = state.endDate?.let { dateLabel(it) } ?: "Not set",
                        placeholder = "",
                        onClick = {
                            navigateTo(
                                screenFactory = {
                                    DatePickerScreen(it, state.endDate ?: state.startDate)
                                },
                                resultCallback = { result ->
                                    if (result != null) viewModel.setEndDate(result)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))

                    // Daily Flow/Energy/Mood — only usable (and only shown)
                    // while this cycle is actually in progress, same rule as
                    // the rest of the cycle's editable fields. Shown any time
                    // the cycle isn't ended yet — including before the very
                    // first save, so Start date and everything below can all
                    // be filled in together on day one, same as everywhere
                    // else in the app (one Save covers whatever's filled in,
                    // not just already-persisted fields).
                    if (state.endDate == null) {
                        LightText(text = "Current cycle daily log", variant = LightTextVariant.Detail)
                        Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))

                        LightTextField(
                            label = "Date",
                            value = dateLabel(state.dailyDate),
                            placeholder = "",
                            onClick = {
                                navigateTo(
                                    screenFactory = { DatePickerScreen(it, state.dailyDate) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setDailyDate(result)
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))

                        LightTextField(
                            label = "Flow",
                            value = state.dailyFlow?.label ?: "Not set",
                            placeholder = "",
                            onClick = {
                                navigateTo(
                                    screenFactory = { FlowPickerScreen(it, state.dailyFlow) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setDailyFlow(result)
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))

                        LightTextField(
                            label = "Energy",
                            value = state.dailyEnergy?.label ?: "Not set",
                            placeholder = "",
                            onClick = {
                                navigateTo(
                                    screenFactory = { EnergyPickerScreen(it, state.dailyEnergy) },
                                    resultCallback = { result ->
                                        if (result != null) viewModel.setDailyEnergy(result)
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))

                        LightTextField(
                            label = "Mood",
                            value = if (state.moodFeatureEnabled) {
                                "Log in Mood tracker"
                            } else if (state.dailyMoods.isEmpty()) {
                                "How are you today?"
                            } else {
                                state.dailyMoods.joinToString(", ") { it.label }
                            },
                            placeholder = "",
                            onClick = {
                                if (state.moodFeatureEnabled) {
                                    // Mood tracking is on — log there instead
                                    // of keeping a second, separate mood field
                                    // here, so tracking both isn't a double
                                    // entry. Defaults to today (not the
                                    // cycle's start date) since this is meant
                                    // for logging however you feel right now —
                                    // backdating is still available inside
                                    // Mood itself.
                                    navigateTo(
                                        screenFactory = {
                                            MoodScreen(it, repo, initialDate = todayStr())
                                        },
                                    )
                                } else {
                                    navigateTo(
                                        screenFactory = { MoodMultiPickerScreen(it, state.dailyMoods) },
                                        resultCallback = { result ->
                                            if (result != null) viewModel.setDailyMoods(result)
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                        )

                        if (state.dailyEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(1.5f.gridUnitsAsDp()))
                            LightText(text = "Logged so far this cycle", variant = LightTextVariant.Detail)
                            Spacer(modifier = Modifier.height(0.5f.gridUnitsAsDp()))
                            state.dailyEntries.forEachIndexed { index, day ->
                                LightText(
                                    text = if (state.moodFeatureEnabled) {
                                        "${day.dateDisplay}: ${day.flowLabel} flow, ${day.energyLabel} energy"
                                    } else {
                                        "${day.dateDisplay}: ${day.flowLabel} flow, ${day.energyLabel} energy, ${day.moodLabel} mood"
                                    },
                                    variant = LightTextVariant.Fine,
                                    lighten = true,
                                )
                                if (index != state.dailyEntries.lastIndex) {
                                    Spacer(modifier = Modifier.height(0.25f.gridUnitsAsDp()))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2f.gridUnitsAsDp()))

                    StatRow(label = "Next expected", value = state.nextExpectedDisplay)
                    Spacer(modifier = Modifier.height(1f.gridUnitsAsDp()))
                    StatRow(label = "Last cycle", value = state.lastCycleDisplay)
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
                            onClick = { navigateTo(screenFactory = { CycleHistoryScreen(it, repo) }) },
                            contentDescription = "History",
                        ),
                        LightBarButton.Text(
                            text = "RESET",
                            onClick = {
                                navigateTo(
                                    screenFactory = {
                                        ConfirmResetScreen(
                                            it,
                                            "Reset all cycle data? This will permanently clear every logged cycle.",
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
