// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later
package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.fontPairings
import orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales

/**
 * The language flag: a tap shows the next language; holding it lists every language to pick one,
 * as holding a favorites arrow lists the places.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun LanguageFlagButton(
    flag: String,
    fontSize: TextUnit,
    padding: PaddingValues,
    onCycleLanguage: () -> Unit,
    onSelectLanguage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val haptic = LocalHapticFeedback.current
    var pickerOpen by remember { mutableStateOf(false) }
    val currentIndex = supportedLocales.indexOfFirst { it.flag == flag }
    Box(modifier) {
        Text(
            text = flag,
            fontSize = fontSize,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .alpha(alpha)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.cd_change_language),
                    onClick = onCycleLanguage,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pickerOpen = true
                    },
                )
                .padding(padding),
        )
        if (pickerOpen) {
            PickerPopup(
                count = supportedLocales.size,
                currentIndex = currentIndex,
                alignEnd = true,
                onDismiss = { pickerOpen = false },
                onSelect = onSelectLanguage,
                leading = { i -> Text(supportedLocales[i].flag) },
                label = { i, current -> PickerLabel(supportedLocales[i].name, current) },
            )
        }
    }
}

/**
 * The font button's list: holding the button lists every font pairing to pick one, each name in its own
 * display font. Put it in a Box around the button, next to it.
 */
@Composable
internal fun FontPicker(currentIndex: Int, onSelectFont: (Int) -> Unit, onDismiss: () -> Unit) {
    PickerPopup(
        count = fontPairings.size,
        currentIndex = currentIndex,
        alignEnd = false,
        onDismiss = onDismiss,
        onSelect = onSelectFont,
        leading = null,
        label = { i, current ->
            Text(
                text = fontPairings[i].name,
                fontFamily = fontPairings[i].display,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (current) 1f else 0.85f),
            )
        },
    )
}

@Composable
private fun PickerLabel(text: String, current: Boolean) {
    Text(
        text = text,
        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (current) 1f else 0.85f),
    )
}

/**
 * A list next to its button, in the favorites list's look. Not a DropdownMenu: that keeps 48 dp from the screen
 * edges, so over a button at the bottom it opened far above it. A long list scrolls, and starts at [currentIndex].
 */
@Composable
private fun PickerPopup(
    count: Int,
    currentIndex: Int,
    alignEnd: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    leading: (@Composable (Int) -> Unit)?,
    label: @Composable (Int, Boolean) -> Unit,
) {
    val scroll = rememberScrollState()
    var currentTop by remember { mutableIntStateOf(-1) }
    // Show the current item with about two items above it
    val twoItems = with(LocalDensity.current) { 96.dp.roundToPx() }
    LaunchedEffect(currentTop) {
        if (currentTop > 0) scroll.scrollTo((currentTop - twoItems).coerceAtLeast(0))
    }
    Popup(
        popupPositionProvider = PickerPosition(alignEnd),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            color = Color(0xFF1A1565), // same as the favorites list
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Hairline),
            shadowElevation = 8.dp,
        ) {
            Column(
                Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.6f).dp)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(scroll)
                    .padding(vertical = 8.dp),
            ) {
                for (i in 0 until count) {
                    val current = i == currentIndex
                    DropdownMenuItem(
                        text = { label(i, current) },
                        leadingIcon = leading?.let { { it(i) } },
                        onClick = {
                            onDismiss()
                            if (!current) onSelect(i)
                        },
                        modifier = if (current) Modifier.onPlaced { currentTop = it.positionInParent().y.toInt() } else Modifier,
                    )
                }
            }
        }
    }
}

/**
 * Right under the button when the list fits there, else right above it. Its end lines up with the button's end
 * ([alignEnd], for the flag) or its start with the button's start (for the font).
 */
private class PickerPosition(private val alignEnd: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val fromRight = alignEnd == (layoutDirection == LayoutDirection.Ltr)
        val x = if (fromRight) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val y = if (anchorBounds.bottom + popupContentSize.height <= windowSize.height) anchorBounds.bottom
        else anchorBounds.top - popupContentSize.height
        return IntOffset(
            x.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
            y.coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height)),
        )
    }
}
