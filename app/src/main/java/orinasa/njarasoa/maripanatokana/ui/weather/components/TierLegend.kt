package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact color-key legend for charts whose data points are colored by a discrete tier (UV,
 * AQI, ...) rather than a single fixed line color — without this, a viewer has no way to know
 * what a given dot's color means. Horizontally scrollable rather than wrapping (matches the
 * app's existing Hourly Forecast card-row convention), so it stays readable and never overflows
 * regardless of label length or locale.
 */
@Composable
fun TierLegend(
    entries: List<Pair<Color, String>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f))
            }
        }
    }
}
