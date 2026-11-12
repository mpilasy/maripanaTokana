// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.mobile.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import orinasa.njarasoa.maripanatokana.ui.common.components.LocalizedDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.common.about.AppQrCode
import orinasa.njarasoa.maripanatokana.ui.common.about.appQrCodes
import orinasa.njarasoa.maripanatokana.ui.common.about.GITHUB_RELEASES_URL
import orinasa.njarasoa.maripanatokana.ui.common.about.AppQrImage
import orinasa.njarasoa.maripanatokana.ui.common.about.labelText
import orinasa.njarasoa.maripanatokana.ui.common.about.deviceInfo

@Composable
private fun AppLink(code: AppQrCode, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(code.labelText() + " ›", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(code.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
    }
}

/** A "Get it on" badge image. */
@Composable
private fun RowScope.AppBadge(@DrawableRes image: Int, description: String, onClick: () -> Unit) {
    Image(
        painter = painterResource(image),
        contentDescription = description,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(646f / 250f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
    )
}

/** The QR code of a link, large; a tap on the address opens it. */
@Composable
private fun AppQrDialog(code: AppQrCode, onOpen: () -> Unit, onDismiss: () -> Unit) {
    LocalizedDialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(code.labelText(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                AppQrImage(code, 240.dp)
                Text(
                    code.address,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onOpen).padding(12.dp),
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cd_close), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenHelp: () -> Unit, languageFlag: String, onCycleLanguage: () -> Unit, onSelectLanguage: (Int) -> Unit, weatherModelName: String? = null) {
    val uriHandler = LocalUriHandler.current
    var openTextRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var openTextTitle by rememberSaveable { mutableStateOf("") }
    var licensesOpen by rememberSaveable { mutableStateOf(false) }
    val licenseRes = openTextRes
    if (licenseRes != null) {
        LicenseTextScreen(title = openTextTitle, textRes = licenseRes, onBack = { openTextRes = null })
    } else if (licensesOpen) {
        LicensesScreen(onBack = { licensesOpen = false }, weatherModelName = weatherModelName)
    } else SettingsPage(title = stringResource(R.string.about_title), onBack = onBack, languageFlag = languageFlag, onCycleLanguage = onCycleLanguage, onSelectLanguage = onSelectLanguage) {
        // App identity
        SettingsGroup {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = LocalDisplayFont.current,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
                Text(
                    // The build details for bug reports: hash, commit time, flavor and version code. Twin: the web About
                    text = "#${BuildConfig.GIT_HASH}${if (BuildConfig.DEBUG) "-d" else ""} • ${BuildConfig.COMMIT_TIME} UTC • ${BuildConfig.FLAVOR} (${BuildConfig.VERSION_CODE})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "© 2026 Orinasa Njarasoa",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                // Opens the full GPL text, with the Play services permission, in-app
                Text(
                    text = stringResource(R.string.about_app_license) + " ›",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            openTextTitle = "GNU GPL v3"
                            openTextRes = R.raw.gpl_license
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                // How to use the app, and the credits for the data, images, fonts and libraries with their licences
                Row(Modifier.padding(top = 4.dp)) {
                    listOf(
                        stringResource(R.string.help_title) to onOpenHelp,
                        stringResource(R.string.licenses_title) to { licensesOpen = true },
                    ).forEach { (label, onClick) ->
                        Text(
                            text = "$label ›",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = onClick)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                var qrCode by remember { mutableStateOf<AppQrCode?>(null) }
                appQrCodes.filter { it.badge == null }.forEach { code -> AppLink(code) { qrCode = code } }
                // The badges, like on the web: F-Droid and Obtainium show their QR code, for another phone to scan;
                // GitHub opens the releases page
                Spacer(Modifier.height(12.dp))
                Row(Modifier.widthIn(max = 470.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    appQrCodes.forEach { code ->
                        val badge = code.badge ?: return@forEach
                        AppBadge(badge, stringResource(code.badgeAltRes ?: return@forEach)) { qrCode = code }
                    }
                    AppBadge(R.drawable.badge_github, stringResource(R.string.android_app_github)) { uriHandler.openUri(GITHUB_RELEASES_URL) }
                }
                qrCode?.let { code -> AppQrDialog(code, onOpen = { uriHandler.openUri(code.url) }, onDismiss = { qrCode = null }) }
            }
        }

        // What the system reports about this phone, for support and bug reports
        val context = LocalContext.current
        val deviceRows = remember { deviceInfo(context) }
        Spacer(Modifier.height(24.dp))
        SectionHeader(stringResource(R.string.about_section_device))
        SettingsGroup {
            deviceRows.forEachIndexed { index, row ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                DeviceInfoRow(stringResource(row.labelRes), row.value)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 13.sp, color = onSurface.copy(alpha = 0.6f), modifier = Modifier.weight(0.4f))
        Spacer(Modifier.width(12.dp))
        // Left to right whatever the language, so versions and sizes read as written
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(text = value, fontSize = 13.sp, color = onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
        }
    }
}
