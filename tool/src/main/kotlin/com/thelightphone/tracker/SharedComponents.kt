package com.thelightphone.tracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp

/**
 * Displays a label above a value — used for weekly/monthly stat blocks.
 */
@Composable
fun StatRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LightText(
            text = label.uppercase(),
            variant = LightTextVariant.Superfine,
            lighten = true,
        )
        Spacer(modifier = Modifier.height(0.25f.gridUnitsAsDp()))
        LightText(
            text = value,
            variant = LightTextVariant.Subheading,
        )
    }
}
