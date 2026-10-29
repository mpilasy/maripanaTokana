// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont

private class HelpSection(@StringRes val titleRes: Int, val items: List<Pair<Int, Int>>)

private val helpSections = listOf(
    HelpSection(
        R.string.help_section_places,
        listOf(
            R.string.help_q_search to R.string.help_a_search,
            R.string.help_q_favorite_add to R.string.help_a_favorite_add,
            R.string.help_q_favorite_move to R.string.help_a_favorite_move,
            R.string.help_q_favorite_edit to R.string.help_a_favorite_edit,
            R.string.help_q_place_details to R.string.help_a_place_details,
        ),
    ),
    HelpSection(
        R.string.help_section_display,
        listOf(
            R.string.help_q_language to R.string.help_a_language,
            R.string.help_q_font to R.string.help_a_font,
            R.string.help_q_units to R.string.help_a_units,
        ),
    ),
    HelpSection(
        R.string.help_section_weather,
        listOf(
            R.string.help_q_refresh to R.string.help_a_refresh,
            R.string.help_q_cut_title to R.string.help_a_cut_title,
            R.string.help_q_share to R.string.help_a_share,
        ),
    ),
)

/** How to use the app: short questions and answers, in the same shell as Settings and About. */
@Composable
fun HelpScreen(onBack: () -> Unit, languageFlag: String, onCycleLanguage: () -> Unit, onSelectLanguage: (Int) -> Unit) {
    SettingsPage(title = stringResource(R.string.help_title), onBack = onBack, languageFlag = languageFlag, onCycleLanguage = onCycleLanguage, onSelectLanguage = onSelectLanguage) {
        helpSections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(section.titleRes))
            SettingsGroup {
                section.items.forEachIndexed { index, (question, answer) ->
                    if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            stringResource(question),
                            fontFamily = LocalBodyFont.current,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(answer),
                            fontFamily = LocalBodyFont.current,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }
}
