package com.thelightphone.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

private const val HISTORY_PREVIEW_LENGTH = 100

data class NoteHistoryItem(
    val id: Long,
    val date: String,
    val dateDisplay: String,
    val notePreview: String,
)

data class NoteHistoryState(
    val entries: List<NoteHistoryItem> = emptyList(),
    val loaded: Boolean = false,
)

class NoteHistoryViewModel(private val repo: TrackerRepository) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(NoteHistoryState())
    val state: StateFlow<NoteHistoryState> = _state.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val history = repo.getNoteHistory()
            val items = history.map { entry ->
                NoteHistoryItem(
                    id = entry.id,
                    date = entry.date,
                    dateDisplay = dateLabel(entry.date),
                    // Truncated so a long entry doesn't blow out the list —
                    // tap through to see the full note.
                    notePreview = if (entry.note.length > HISTORY_PREVIEW_LENGTH) {
                        entry.note.take(HISTORY_PREVIEW_LENGTH) + "…"
                    } else {
                        entry.note
                    },
                )
            }
            _state.value = NoteHistoryState(
                entries = items.sortedWith(compareByDescending<NoteHistoryItem> { it.date }.thenByDescending { it.id }),
                loaded = true,
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteNoteEntry(id)
            reload()
        }
    }
}

class NotesHistoryScreen(
    sealedActivity: SealedLightActivity,
    private val repo: TrackerRepository,
) : LightScreen<Unit, NoteHistoryViewModel>(sealedActivity) {

    override val viewModelClass: Class<NoteHistoryViewModel>
        get() = NoteHistoryViewModel::class.java

    override fun createViewModel() = NoteHistoryViewModel(repo)

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
                    center = LightTopBarCenter.Text("Notes history"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                if (state.loaded && state.entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "No notes logged yet.",
                            variant = LightTextVariant.Copy,
                            lighten = true,
                        )
                    }
                } else {
                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        state.entries.forEachIndexed { index, item ->
                            NoteHistoryRow(
                                item = item,
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            ConfirmResetScreen(
                                                it,
                                                "Delete this note?",
                                                title = "Confirm deletion",
                                                confirmLabel = "DELETE",
                                            )
                                        },
                                        resultCallback = { confirmed ->
                                            if (confirmed == true) viewModel.deleteEntry(item.id)
                                        },
                                    )
                                },
                            )
                            if (index != state.entries.lastIndex) {
                                Spacer(modifier = Modifier.height(1.25f.gridUnitsAsDp()))
                            }
                        }
                    }
                }

                LightBottomBar(items = listOf())
            }
        }
    }
}

@Composable
private fun NoteHistoryRow(item: NoteHistoryItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        LightText(
            text = item.dateDisplay,
            variant = LightTextVariant.Copy,
        )
        Spacer(modifier = Modifier.height(0.25f.gridUnitsAsDp()))
        LightText(
            text = item.notePreview,
            variant = LightTextVariant.Fine,
            lighten = true,
        )
    }
}
