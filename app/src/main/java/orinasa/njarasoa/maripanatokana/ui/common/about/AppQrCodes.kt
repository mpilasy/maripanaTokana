// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.common.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import orinasa.njarasoa.maripanatokana.R

/** A link of the About page: its label, the address shown under it, and the QR code that a tap shows. The web has the same links. */
internal data class AppQrCode(@DrawableRes val image: Int, @StringRes val labelRes: Int?, val label: String, val address: String, val url: String)

internal val appQrCodes = listOf(
    AppQrCode(R.drawable.qr_source_code, R.string.about_source_code, "", "github.com/mpilasy/maripanaTokana", "https://github.com/mpilasy/maripanaTokana"),
    AppQrCode(R.drawable.qr_web_app, R.string.about_web_version, "", "maripana.njarasoa.org", "https://maripana.njarasoa.org"),
    AppQrCode(R.drawable.qr_fdroid, null, "F-Droid", "f-droid.org", "https://f-droid.org/packages/orinasa.njarasoa.maripanatokana/"),
    AppQrCode(R.drawable.qr_obtainium, null, "Obtainium", "apps.obtainium.imranr.dev", "https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/mpilasy/maripanaTokana"),
)

@Composable
internal fun AppQrCode.labelText() = labelRes?.let { stringResource(it) } ?: label

/** The QR code, for another phone or a computer to scan. No smoothing: the small image keeps sharp squares when it grows. */
@Composable
internal fun AppQrImage(code: AppQrCode, size: Dp) {
    Image(ImageBitmap.imageResource(code.image), contentDescription = code.address, modifier = Modifier.size(size), filterQuality = FilterQuality.None)
}
