// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog

/**
 * [Dialog] that keeps the app language. A dialog gets its own window, whose root resets
 * LocalContext/LocalConfiguration to the Activity's — and the Activity keeps the language it
 * was launched with — so strings would ignore a language switched mid-session.
 */
@Composable
fun LocalizedDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            content = content,
        )
    }
}
