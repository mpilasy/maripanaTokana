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

/** Meteocons artwork (MIT, Bas Milius) for a WMO weather code, with day and night variants. */
@DrawableRes
internal fun weatherIconRes(code: Int, night: Boolean): Int = when (code) {
    0 -> if (night) R.drawable.wx_clear_night else R.drawable.wx_clear_day
    1, 2 -> if (night) R.drawable.wx_partly_cloudy_night else R.drawable.wx_partly_cloudy_day
    3 -> R.drawable.wx_overcast
    45, 48 -> if (night) R.drawable.wx_fog_night else R.drawable.wx_fog_day
    51, 53, 55 -> R.drawable.wx_drizzle
    56, 57, 66, 67 -> R.drawable.wx_sleet
    61, 63, 65 -> R.drawable.wx_rain
    71, 73, 75, 77 -> R.drawable.wx_snow
    80, 81, 82 -> if (night) R.drawable.wx_partly_cloudy_night_rain else R.drawable.wx_partly_cloudy_day_rain
    85, 86 -> if (night) R.drawable.wx_partly_cloudy_night_snow else R.drawable.wx_partly_cloudy_day_snow
    95, 96, 99 -> if (night) R.drawable.wx_thunderstorms_night_rain else R.drawable.wx_thunderstorms_day_rain
    else -> R.drawable.wx_not_available
}

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
