package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkOnPrimary
import orinasa.njarasoa.maripanatokana.ui.theme.AppDarkPrimary
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A 270° arc, open at the bottom, filled to [fraction] (0-1). */
@Composable
fun Gauge(fraction: Float, color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val f = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 5f / 48f
        val inset = this.size.minDimension * 4f / 48f
        val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(Color.White.copy(alpha = 0.12f), 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (f > 0f) drawArc(color, 135f, 270f * f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** A compass ring whose needle points to where the wind comes from (degrees, 0 = north). */
@Composable
fun WindCompass(degrees: Int, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 48f
        val c = Offset(24 * u, 24 * u)
        drawCircle(Color.White.copy(alpha = 0.12f), 20 * u, c, style = Stroke(2 * u))
        drawLine(Color.White.copy(alpha = 0.5f), Offset(24 * u, 2 * u), Offset(24 * u, 8 * u), 2 * u, StrokeCap.Round)
        rotate(degrees.toFloat(), c) {
            fun shape(vararg p: Pair<Float, Float>) = Path().apply {
                moveTo(p[0].first * u, p[0].second * u)
                p.drop(1).forEach { lineTo(it.first * u, it.second * u) }
                close()
            }
            drawPath(shape(24f to 8f, 29f to 26f, 24f to 22f, 19f to 26f), Color(0xFF64B5F6))
            drawPath(shape(24f to 22f, 29f to 26f, 24f to 40f, 19f to 26f), Color.White.copy(alpha = 0.25f))
        }
    }
}

/** The day as an arc from sunrise to sunset with the sun at [now] (all epoch seconds). */
@Composable
fun SunArc(sunrise: Long, sunset: Long, now: Long, modifier: Modifier = Modifier) {
    val t = if (sunset > sunrise) (now - sunrise).toFloat() / (sunset - sunrise) else -1f
    Canvas(modifier) {
        // Half-ellipse across the width, peaking near the top
        val base = size.height * 44f / 52f
        val rx = size.width * 52f / 120f
        val ry = size.height * 36f / 52f
        val cx = size.width / 2
        val oval = Rect(cx - rx, base - ry, cx + rx, base + ry)
        drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
        drawArc(Color.White.copy(alpha = 0.25f), 180f, 180f, false, oval.topLeft, oval.size,
            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
        if (t in 0f..1f) {
            drawArc(Color(0xFFFFB74D), 180f, 180f * t, false, oval.topLeft, oval.size, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            val a = PI * (1 - t)
            drawCircle(Color(0xFFFFD54F), 5.dp.toPx(), Offset(cx + rx * cos(a).toFloat(), base - ry * sin(a).toFloat()))
        }
    }
}

/** A vertical track with a blue-to-orange segment from [low] to [high] (fractions of the week's range, 0 = bottom). */
@Composable
fun TemperatureRangeBar(low: Float, high: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val r = androidx.compose.ui.geometry.CornerRadius(w / 2, w / 2)
        drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = r)
        val top = size.height * (1 - high.coerceIn(0f, 1f))
        val bottom = maxOf(size.height * (1 - low.coerceIn(0f, 1f)), top + w)
        drawRoundRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFF7043), Color(0xFF64B5F6)), top, bottom),
            topLeft = Offset(0f, top), size = Size(w, bottom - top), cornerRadius = r,
        )
    }
}

/** A row of chips that picks what a forecast card shows. */
@Composable
fun <T> ForecastTabs(tabs: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val bodyFont = LocalBodyFont.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (id, label) ->
            val isSelected = id == selected
            Text(
                text = label,
                fontSize = 13.sp,
                fontFamily = bodyFont,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AppDarkOnPrimary else Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) AppDarkPrimary else TileSurface)
                    .clickable { onSelect(id) }
                    .semantics { role = Role.Tab; this.selected = isSelected }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}
