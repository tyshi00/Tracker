package com.thelightphone.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.lp3Keyboard.ui.LowerCaseLayout
import com.thelightphone.lp3Keyboard.ui.NumberLayout
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import java.util.UUID

class NumberEditorScreen(
    sealedActivity: SealedLightActivity,
    private val title: String,
    private val initialValue: String,
    private val isDecimal: Boolean = false,
) : SimpleLightScreen<String>(sealedActivity) {

    private val editorInstanceKey = UUID.randomUUID().toString()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val textState = rememberTextFieldState(initialValue)
        val keyboardOptionsFlow = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            LightTextInputEditor(
                title = title,
                state = textState,
                keyboardOptionsFlow = keyboardOptionsFlow,
                onSubmit = { result -> goBack(result.toString().trim()) },
                onBack = { goBack(null) },
                submitLabel = "DONE",
                initialLayout = if (isDecimal) NumberLayout else LowerCaseLayout,
                editorKey = editorInstanceKey,
                modifier = Modifier.background(LightThemeTokens.colors.background),
            )
        }
    }
}
