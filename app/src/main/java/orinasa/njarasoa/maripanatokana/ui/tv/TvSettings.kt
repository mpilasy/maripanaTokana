// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import orinasa.njarasoa.maripanatokana.ui.common.about.AppQrCode
import orinasa.njarasoa.maripanatokana.ui.common.about.DeviceRow
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
) {
    val shaped = if (localize) LocalTvShaper.current(text) else text
    androidx.compose.material3.Text(
        shaped, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign,
        lineHeight = lineHeight, overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines,
        style = style ?: androidx.compose.material3.LocalTextStyle.current,
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
private fun AboutLine(text: String, size: Int = 18, modifier: Modifier = Modifier) {
    Text(text, localize = false, color = Dim, fontSize = size.sp, modifier = modifier)
}

/** The phone's About text. A TV has no browser to rely on, so the addresses show as text. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AboutPage() {
    val context = LocalContext.current
    val device = remember { deviceInfo(context) }
    var qrCode by remember { mutableStateOf<AppQrCode?>(null) }
    var licensesOpen by remember { mutableStateOf(false) }
    if (licensesOpen) {
        BackHandler { licensesOpen = false }
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
    // On a TV, Compose scrolls a focused item to near the middle of the screen; here the page scrolls only when the link is not visible
    CompositionLocalProvider(LocalBringIntoViewSpec provides object : BringIntoViewSpec {}) { AboutContent(device, onLink = { qrCode = it }, onLicenses = { licensesOpen = true }) }
}

@Composable
private fun AboutContent(device: List<DeviceRow>, onLink: (AppQrCode) -> Unit, onLicenses: () -> Unit) {
    ScrollScreen {
        PageTitle(stringResource(R.string.about_title))
        // The app, its version, hash and commit time, close together
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.app_name), localize = false, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            AboutLine("v${BuildConfig.VERSION_NAME}")
            AboutLine("#${BuildConfig.GIT_HASH} • ${BuildConfig.COMMIT_TIME} UTC • ${BuildConfig.FLAVOR} (${BuildConfig.VERSION_CODE})", 15)
            AboutLine("© 2026 Orinasa Njarasoa", modifier = Modifier.padding(top = 6.dp))
            AboutLine(stringResource(R.string.about_app_license))
        }
        // A TV has no browser to rely on: OK on a link shows its QR code, for a phone to scan.
        // Left and right move between the links; up and down still scroll the page.
        val firstLink = remember { FocusRequester() }
        LaunchedEffect(Unit) { firstLink.requestFocus() }
        // All cards as tall as the tallest (only F-Droid and Obtainium have a badge)
        Row(Modifier.padding(vertical = 8.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            appQrCodes.forEachIndexed { i, code ->
                FocusCard(Modifier.weight(1f).fillMaxHeight().then(if (i == 0) Modifier.focusRequester(firstLink) else Modifier), onClick = { onLink(code) }) {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(code.labelText(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(code.address, localize = false, color = Dim, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
                        // The "Get it on" badge tells that the link gets the app, before OK opens its QR code
                        code.badge?.let {
                            Image(painterResource(it), contentDescription = null, modifier = Modifier.padding(top = 8.dp).height(36.dp))
                        }
                    }
                }
            }
            // In the same row as the links: up and down scroll the page, so only left and right reach it
            // As wide as the links, so a long label ("Fahazoan'alàlana") breaks only between words
            FocusCard(Modifier.weight(1f).fillMaxHeight(), onClick = onLicenses) {
                Box(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.licenses_title) + " ›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
        AboutHeading(stringResource(R.string.about_section_device))
        device.forEach { row -> AboutLine("${stringResource(row.labelRes)}: ${row.value}") }
    }
}

/** The credits of the phone's Licenses page, as text: a TV has no browser for the licence pages. */
@Composable
private fun LicensesPage() {
    ScrollScreen {
        PageTitle(stringResource(R.string.licenses_title))
        creditSections.forEach { section ->
            AboutHeading(stringResource(section.titleRes))
            section.items.forEach { item ->
                val name = item.nameRes?.let { stringResource(it) } ?: item.name
                val note = item.noteRes?.let { stringResource(it) } ?: item.note
                AboutLine(listOfNotNull(name, item.license, note).joinToString(" · "))
            }
        }
    }
}
