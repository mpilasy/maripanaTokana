// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes

/** Meteocons artwork (MIT, Bas Milius) for each WMO code group, as (day, night). */
private val WEATHER_ICONS = mapOf(
    listOf(0) to (R.drawable.wx_clear_day to R.drawable.wx_clear_night),
    listOf(1, 2) to (R.drawable.wx_partly_cloudy_day to R.drawable.wx_partly_cloudy_night),
    listOf(3) to (R.drawable.wx_overcast to R.drawable.wx_overcast),
    listOf(45, 48) to (R.drawable.wx_fog_day to R.drawable.wx_fog_night),
    listOf(51, 53, 55) to (R.drawable.wx_drizzle to R.drawable.wx_drizzle),
    listOf(56, 57, 66, 67) to (R.drawable.wx_sleet to R.drawable.wx_sleet),
    listOf(61, 63, 65) to (R.drawable.wx_rain to R.drawable.wx_rain),
    listOf(71, 73, 75, 77) to (R.drawable.wx_snow to R.drawable.wx_snow),
    listOf(80, 81, 82) to (R.drawable.wx_partly_cloudy_day_rain to R.drawable.wx_partly_cloudy_night_rain),
    listOf(85, 86) to (R.drawable.wx_partly_cloudy_day_snow to R.drawable.wx_partly_cloudy_night_snow),
    listOf(95, 96, 99) to (R.drawable.wx_thunderstorms_day_rain to R.drawable.wx_thunderstorms_night_rain),
).flatMap { (codes, icons) -> codes.map { it to icons } }.toMap()

/** The icon for a WMO weather code, with day and night variants. */
@DrawableRes
internal fun weatherIconRes(code: Int, night: Boolean): Int =
    WEATHER_ICONS[code]?.let { if (night) it.second else it.first } ?: R.drawable.wx_not_available

/**
 * A weather condition as an icon. The artwork leaves wide margins in its square, so the icon takes
 * up a little less room in the layout than it draws, pulling its neighbours closer (as on the web).
 */
@Composable
fun WeatherIcon(code: Int, night: Boolean = false, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(weatherIconRes(code, night)),
        contentDescription = stringResource(wmoDescriptionRes(code)),
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val inset = (placeable.height * 0.12f).toInt()
                layout(placeable.width - 2 * inset, placeable.height - 2 * inset) {
                    placeable.place(-inset, -inset)
                }
            }
            .size(size),
    )
}
