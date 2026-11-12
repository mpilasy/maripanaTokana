// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.mobile.settings

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.common.about.Credit
import orinasa.njarasoa.maripanatokana.ui.common.about.creditSections
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline

/**
 * The credits for the data, images, fonts and libraries, opened from About. A row with a bundled licence text
 * opens it on a page of its own; the other rows open their licence page in the browser.
 * The web has the same page in `web/src/lib/components/LicensesScreen.svelte`.
 */
@Composable
fun LicensesScreen(onBack: () -> Unit, weatherModelName: String? = null) {
    val uriHandler = LocalUriHandler.current
    var openTextRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var openTextTitle by rememberSaveable { mutableStateOf("") }
    val textRes = openTextRes
    if (textRes != null) {
        LicenseTextScreen(title = openTextTitle, textRes = textRes, onBack = { openTextRes = null })
        return
    }
    SettingsPage(title = stringResource(R.string.licenses_title), onBack = onBack) {
        creditSections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(section.titleRes))
            SettingsGroup {
                section.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                    }
                    val name = item.nameRes?.let { stringResource(it) } ?: item.name
                    val onOpen: (() -> Unit)? = when {
                        item.textRes != null -> { { openTextTitle = name; openTextRes = item.textRes } }
                        item.url != null -> { { uriHandler.openUri(item.url) } }
                        else -> null
                    }
                    CreditRow(item, name, onOpen = onOpen, weatherModelName = weatherModelName)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CreditRow(item: Credit, name: String, onOpen: (() -> Unit)?, weatherModelName: String?) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen, role = Role.Button) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name + (weatherModelName?.takeIf { item.showsWeatherModel }?.let { " · $it" } ?: ""),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = onSurface,
            )
            val note = item.noteRes?.let { stringResource(it) } ?: item.note
            if (note != null) {
                Text(text = note, fontSize = 12.sp, lineHeight = 16.sp, color = onSurface.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.width(12.dp))
        // › opens the licence text in the app, ↗ opens a web page
        val mark = when {
            item.textRes != null -> " ›"
            onOpen != null -> " ↗"
            else -> ""
        }
        Text(
            text = item.license + mark,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.End,
            color = onSurface.copy(alpha = 0.65f),
            modifier = Modifier.width(110.dp),
        )
    }
}

/** A bundled licence text, on a page of its own. */
@Composable
internal fun LicenseTextScreen(title: String, @RawRes textRes: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val text by produceState<String?>(null, textRes) {
        value = withContext(Dispatchers.IO) {
            context.resources.openRawResource(textRes).bufferedReader().use { it.readText() }
        }
    }
    SettingsPage(title = title, onBack = onBack) {
        SettingsGroup {
            val loaded = text
            if (loaded == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                Text(
                    text = loaded,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
