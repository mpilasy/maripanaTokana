// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import androidx.compose.foundation.text.TextAutoSize
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.text.TextUtilsCompat
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import orinasa.njarasoa.maripanatokana.ui.common.about.AppQrCode
import orinasa.njarasoa.maripanatokana.ui.common.about.AppQrImage
import orinasa.njarasoa.maripanatokana.ui.common.about.appQrCodes
import orinasa.njarasoa.maripanatokana.ui.common.about.labelText
import orinasa.njarasoa.maripanatokana.ui.common.components.LocalizedDialog
import orinasa.njarasoa.maripanatokana.ui.common.about.creditSections
import orinasa.njarasoa.maripanatokana.ui.common.about.deviceInfo
import orinasa.njarasoa.maripanatokana.ui.common.supportedLocales
import java.util.Locale

/** True when the metric value comes first. */
internal val LocalMetricPrimary = staticCompositionLocalOf { true }

/** Puts a text in the digits of the language, and keeps a text without right-to-left letters in its order. */
internal val LocalTvShaper = staticCompositionLocalOf<(String) -> String> { { it } }

/** Keeps [text] left to right inside a right-to-left line. */
internal fun ltr(text: String) = "⁦$text⁩"

private fun hasRtlLetters(text: String) = text.any {
    val direction = Character.getDirectionality(it)
    direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
}

/** The Text of the TV screens. With [localize] false (names, alert text), the digits stay as they are. */
@Composable
internal fun Text(
    text: String,
    modifier: Modifier = Modifier,
    localize: Boolean = true,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle? = null,
    autoSize: TextAutoSize? = null,
) {
    val shaped = if (localize) LocalTvShaper.current(text) else text
    androidx.compose.material3.Text(
        shaped, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign,
        lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines,
        style = style ?: androidx.compose.material3.LocalTextStyle.current, autoSize = autoSize,
    )
}

/** The unit order and the language, in the same preferences as the phone screens. */
internal class TvDisplayPrefs(private val prefs: SharedPreferences) {
    val metricPrimary: Boolean get() = prefs.getBoolean("metric_primary", true)
    val localeIndex: Int get() = prefs.getInt("locale_index", 0).coerceIn(supportedLocales.indices)

    fun setMetricPrimary(value: Boolean) = prefs.edit { putBoolean("metric_primary", value) }
    fun setLocaleIndex(value: Int) = prefs.edit { putInt("locale_index", value) }
}

/** Gives [content] the language, layout direction, digits and unit order, as the phone screens do. */
@Composable
internal fun TvLocalized(localeIndex: Int, metric: Boolean, content: @Composable () -> Unit) {
    val supported = supportedLocales[localeIndex]
    val base = LocalContext.current
    val current = LocalConfiguration.current
    val context = remember(supported, base, current) {
        val config = Configuration(current).apply { setLocale(Locale.forLanguageTag(supported.tag)) }
        val resources = base.createConfigurationContext(config).resources
        object : ContextWrapper(base) {
            override fun getResources() = resources
        }
    }
    val rtl = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.forLanguageTag(supported.tag)) == View.LAYOUT_DIRECTION_RTL
    val shaper = remember(supported, rtl) {
        val shape: (String) -> String = { text ->
            supported.localizeDigits(if (rtl && !hasRtlLetters(text)) ltr(text) else text)
        }
        shape
    }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides context.resources.configuration,
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalMetricPrimary provides metric,
        LocalTvShaper provides shaper,
        content = content,
    )
}

private enum class SettingsPage { List, Help }

/** The title of a page without weather, so without the place name. */
@Composable
private fun PageTitle(title: String) {
    Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
}

/** OK on a row changes it at once, or opens its page. The other phone settings are not on the TV for now. */
@Composable
internal fun SettingsScreen(
    display: TvDisplayPrefs,
    metric: Boolean,
    localeIndex: Int,
    onMetric: (Boolean) -> Unit,
    onLocale: (Int) -> Unit,
) {
    var page by remember { mutableStateOf(SettingsPage.List) }
    if (page != SettingsPage.List) BackHandler { page = SettingsPage.List }
    when (page) {
        SettingsPage.Help -> HelpPage()
        SettingsPage.List -> {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            val locale = supportedLocales[localeIndex]
            Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PageTitle(stringResource(R.string.settings_title))
                Spacer(Modifier.height(4.dp))
                SettingRow(Modifier.focusRequester(focus), stringResource(R.string.cd_toggle_units), if (metric) "°C · m/s · hPa" else "°F · mph · inHg") {
                    display.setMetricPrimary(!metric)
                    onMetric(!metric)
                }
                SettingRow(Modifier, stringResource(R.string.cd_change_language), "${locale.flag} ${locale.name}") {
                    val next = (localeIndex + 1) % supportedLocales.size
                    display.setLocaleIndex(next)
                    onLocale(next)
                }
                SettingRow(Modifier, stringResource(R.string.help_title), "›") { page = SettingsPage.Help }
            }
        }
    }
}

@Composable
private fun SettingRow(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    FocusCard(modifier, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color.White, fontSize = 22.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private val HELP = listOf(
    R.string.tv_help_q_move to R.string.tv_help_a_move,
    R.string.tv_help_q_chart to R.string.tv_help_a_chart,
    R.string.tv_help_q_place to R.string.tv_help_a_place,
    R.string.tv_help_q_settings to R.string.tv_help_a_settings,
    R.string.tv_help_q_alerts to R.string.tv_help_a_alerts,
)

@Composable
private fun HelpPage() {
    ScrollScreen {
        PageTitle(stringResource(R.string.help_title))
        HELP.forEach { (question, answer) ->
            Column {
                Text(stringResource(question), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(answer), color = Dim, fontSize = 20.sp, lineHeight = 28.sp)
            }
        }
    }
}

@Composable
private fun AboutHeading(text: String) {
    Text(text, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun AboutLine(text: String, size: Int = 18) {
    Text(text, localize = false, color = Dim, fontSize = size.sp)
}

/**
 * About on the TV, laid out like Fijerena's: one list of full-width rows that Up and Down move through.
 * The app and its build; the copyright and licence; the sharing links as buttons side by side, where OK shows
 * the QR code for a phone to scan (a TV has no browser to rely on); this device; and the Licenses row, which
 * opens [LicensesPage].
 * Focus starts on the first row and comes back to the Licenses row from its page.
 */
@Composable
internal fun AboutPage() {
    val context = LocalContext.current
    val device = remember { deviceInfo(context) }
    var qrCode by remember { mutableStateOf<AppQrCode?>(null) }
    var licensesOpen by rememberSaveable { mutableStateOf(false) }
    var backFromLicenses by remember { mutableStateOf(false) }
    // Kept while the Licenses page shows, so the list comes back where it was
    val listState = rememberLazyListState()
    if (licensesOpen) {
        BackHandler { licensesOpen = false; backFromLicenses = true }
        LicensesPage()
        return
    }
    qrCode?.let { code ->
        // Back closes it
        LocalizedDialog(onDismissRequest = { qrCode = null }) {
            Column(
                Modifier.background(Card, RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(code.labelText(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                AppQrImage(code, 300.dp)
                Text(code.address, localize = false, color = Dim, fontSize = 18.sp)
            }
        }
    }
    val firstFocus = remember { FocusRequester() }
    val licensesFocus = remember { FocusRequester() }
    // Down from the copyright row and Up from this device go to the first link, not the nearest one
    val firstLink = remember { FocusRequester() }
    LaunchedEffect(Unit) { requestFocusAfterLayout(if (backFromLicenses) licensesFocus else firstFocus) }
    RowList(listState) {
        item { PageTitle(stringResource(R.string.about_title)) }
        item {
            InfoRow(
                title = stringResource(R.string.app_name),
                lines = listOf("v${BuildConfig.VERSION_NAME}", "#${BuildConfig.GIT_HASH} • ${BuildConfig.COMMIT_TIME} UTC • ${BuildConfig.FLAVOR}"),
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
        item {
            InfoRow(
                title = "© 2026 Orinasa Njarasoa",
                lines = listOf(stringResource(R.string.about_app_license)),
                modifier = Modifier.focusProperties { down = firstLink },
            )
        }
        item {
            // The sharing links stay buttons, side by side as before: OK shows the QR code, for a phone to scan.
            // All cards as tall as the tallest (only F-Droid and Obtainium have a badge)
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                appQrCodes.forEachIndexed { i, code ->
                    FocusCard(Modifier.weight(1f).fillMaxHeight().then(if (i == 0) Modifier.focusRequester(firstLink) else Modifier), onClick = { qrCode = code }) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(code.labelText(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(code.address, localize = false, color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                            // The "Get it on" badge tells that the link gets the app, before OK opens its QR code
                            code.badge?.let {
                                Image(painterResource(it), contentDescription = null, modifier = Modifier.padding(top = 8.dp).height(36.dp))
                            }
                        }
                    }
                }
            }
        }
        item { AboutHeading(stringResource(R.string.about_section_device)) }
        item {
            // One block, label and value in two columns
            FocusCard(Modifier.fillMaxWidth().focusProperties { up = firstLink }, onClick = {}) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    device.forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(stringResource(row.labelRes), color = Dim, fontSize = 18.sp, modifier = Modifier.width(240.dp))
                            Text(row.value, localize = false, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            InfoRow(
                title = stringResource(R.string.licenses_title),
                lines = listOf(stringResource(R.string.about_row_desc)),
                opens = true,
                modifier = Modifier.padding(top = 8.dp).focusRequester(licensesFocus),
                onClick = { licensesOpen = true },
            )
        }
    }
}

/**
 * The credits of the phone's Licenses page: one row for each data source, image, font or library, under its
 * section. OK on a row with a bundled licence text opens [LicenseTextPage]; the others name their licence only,
 * as a TV has no browser for their pages. Focus comes back to the row that was opened.
 */
@Composable
private fun LicensesPage() {
    val listState = rememberLazyListState()
    val credits = creditSections.flatMap { section -> section.items.map { section.titleRes to it } }
    var openIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var backFrom by remember { mutableStateOf(0) }
    val focus = remember { mutableMapOf<Int, FocusRequester>() }
    fun focusOf(i: Int) = focus.getOrPut(i) { FocusRequester() }
    val open = openIndex?.let { credits.getOrNull(it) }?.second
    val openName = open?.let { it.nameRes?.let { res -> stringResource(res) } ?: it.name }
    if (open?.textRes != null && openName != null) {
        BackHandler { backFrom = openIndex ?: 0; openIndex = null }
        LicenseTextPage(openName, open.textRes)
        return
    }
    LaunchedEffect(Unit) { requestFocusAfterLayout(focusOf(backFrom)) }
    RowList(listState) {
        item { PageTitle(stringResource(R.string.licenses_title)) }
        credits.forEachIndexed { i, (sectionRes, credit) ->
            if (i == 0 || credits[i - 1].first != sectionRes) item { AboutHeading(stringResource(sectionRes)) }
            item {
                val name = credit.nameRes?.let { stringResource(it) } ?: credit.name
                val note = credit.noteRes?.let { stringResource(it) } ?: credit.note
                InfoRow(
                    title = name,
                    lines = listOfNotNull(credit.license, note),
                    opens = credit.textRes != null,
                    modifier = Modifier.focusRequester(focusOf(i)),
                    onClick = { if (credit.textRes != null) openIndex = i },
                )
            }
        }
    }
}

/** A bundled licence text, one card per paragraph: Up and Down cannot scroll through a card taller than the screen. */
@Composable
private fun LicenseTextPage(title: String, @RawRes textRes: Int) {
    val context = LocalContext.current
    val text by produceState<String?>(null, textRes) {
        value = withContext(Dispatchers.IO) { context.resources.openRawResource(textRes).bufferedReader().use { it.readText() } }
    }
    val paragraphs = remember(text) {
        text.orEmpty().split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(paragraphs.isNotEmpty()) { if (paragraphs.isNotEmpty()) requestFocusAfterLayout(firstFocus) }
    RowList(rememberLazyListState()) {
        item { PageTitle(title) }
        paragraphs.forEachIndexed { i, paragraph ->
            item {
                // "==== name ====" starts each font's licence in the fonts file: a heading, not a card
                if (paragraph.startsWith("====")) {
                    AboutHeading(paragraph.trim('=', ' '))
                } else {
                    FocusCard(Modifier.fillMaxWidth().then(if (i == 0 || (i == 1 && paragraphs[0].startsWith("===="))) Modifier.focusRequester(firstFocus) else Modifier), onClick = {}) {
                        Text(paragraph, localize = false, color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp, lineHeight = 26.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp))
                    }
                }
            }
        }
    }
}

/** The list of About and Licenses: the TV safe margins, with the vertical one inside the list so a focused border is not cut. */
@Composable
private fun RowList(state: LazyListState, content: LazyListScope.() -> Unit) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp),
        contentPadding = PaddingValues(vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * One focusable full-width row: the [title], then [lines] in the dim colour; a [badge] image and, when OK opens
 * something ([opens]), a chevron at the end.
 */
@Composable
private fun InfoRow(
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier,
    @DrawableRes badge: Int? = null,
    opens: Boolean = false,
    onClick: () -> Unit = {},
) {
    FocusCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, localize = false, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                lines.forEach { Text(it, localize = false, color = Dim, fontSize = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            }
            badge?.let { Image(painterResource(it), contentDescription = null, modifier = Modifier.height(36.dp)) }
            if (opens) Text("›", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Focus [requester] once its row is laid out; a row that is not composed (scrolled away) is skipped. */
private suspend fun requestFocusAfterLayout(requester: FocusRequester) {
    withFrameNanos {}
    runCatching { requester.requestFocus() }
}
