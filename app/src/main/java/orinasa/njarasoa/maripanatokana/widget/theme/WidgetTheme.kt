package orinasa.njarasoa.maripanatokana.widget.theme

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Widget palette derived from the reference screenshots:
 * deep navy background, vibrant purple-blue cards, white text.
 */
object WidgetColors {
    val BackgroundDark = Color(0xFF0E0B3D)
    val BackgroundMid = Color(0xFF1A1565)
    val CardSurface = Color(0xFF2A1FA5)
    val CardBright = Color(0xFF4535D0)
    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xAAFFFFFF)
    val Accent = Color(0xFF9F8FEF)
}

/** Glance-compatible [ColorProvider]s — widget is always dark-themed. */
object WidgetColorProviders {
    val onSurface: ColorProvider = ColorProvider(WidgetColors.OnSurface)
    val onSurfaceVariant: ColorProvider = ColorProvider(WidgetColors.OnSurfaceVariant)
    val accent: ColorProvider = ColorProvider(WidgetColors.Accent)
    val cardSurface: ColorProvider = ColorProvider(WidgetColors.CardSurface)
}
