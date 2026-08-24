package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

internal data class ControlPoints(val cp1: Offset, val cp2: Offset)

internal fun computeMonotoneCubicControlPoints(points: List<Offset>): List<ControlPoints> {
    val n = points.size
    val result = if (n < 2) {
        emptyList()
    } else {
        val dx = FloatArray(n - 1)
        val dy = FloatArray(n - 1)
        val ms = FloatArray(n - 1)

        for (i in 0 until n - 1) {
            dx[i] = (points[i + 1].x - points[i].x).coerceAtLeast(0.0001f)
            dy[i] = points[i + 1].y - points[i].y
            ms[i] = dy[i] / dx[i]
        }

        val ds = FloatArray(n)
        ds[0] = ms[0]
        ds[n - 1] = ms[n - 2]

        for (i in 1 until n - 1) {
            if (ms[i - 1] * ms[i] <= 0f) {
                ds[i] = 0f
            } else {
                ds[i] = (ms[i - 1] + ms[i]) / 2f
            }
        }

        for (i in 0 until n - 1) {
            if (ms[i] == 0f) {
                ds[i] = 0f
                ds[i + 1] = 0f
            } else {
                val alpha = ds[i] / ms[i]
                val beta = ds[i + 1] / ms[i]
                val dist = alpha * alpha + beta * beta
                if (dist > 9f) {
                    val tau = 3f / sqrt(dist)
                    ds[i] = tau * alpha * ms[i]
                    ds[i + 1] = tau * beta * ms[i]
                }
            }
        }

        val list = mutableListOf<ControlPoints>()
        for (i in 0 until n - 1) {
            val h = dx[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val cp1 = Offset(p1.x + h / 3f, p1.y + ds[i] * h / 3f)
            val cp2 = Offset(p2.x - h / 3f, p2.y - ds[i + 1] * h / 3f)
            list.add(ControlPoints(cp1, cp2))
        }
        list
    }
    return result
}
