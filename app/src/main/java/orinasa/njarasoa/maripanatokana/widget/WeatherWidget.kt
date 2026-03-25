package orinasa.njarasoa.maripanatokana.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import orinasa.njarasoa.maripanatokana.MainActivity
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoDescriptionRes
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.widget.theme.WidgetColorProviders
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetWeatherFetcher.fetch(context)
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val metricPrimary = prefs.getBoolean("metric_primary", true)
        val hasCachedLocation = prefs.getFloat("lat", Float.MIN_VALUE) != Float.MIN_VALUE

        provideContent {
            GlanceTheme {
                if (data != null) {
                    WeatherWidgetContent(data, metricPrimary)
                } else {
                    WidgetError(hasCachedLocation)
                }
            }
        }
    }
}

@Composable
internal fun WidgetError(hasCachedLocation: Boolean = false) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(
                if (hasCachedLocation) R.string.widget_tap_to_refresh
                else R.string.widget_open_app
            ),
            style = TextStyle(
                color = WidgetColorProviders.onSurfaceVariant,
                fontSize = 12.sp,
            ),
        )
    }
}

@SuppressLint("LocalContextConfigurationRead")
@Composable
private fun WeatherWidgetContent(data: WeatherData, metricPrimary: Boolean) {
        val baseContext = LocalContext.current
        val context = remember(baseContext) {
            val prefs = baseContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val localeIndex = prefs.getInt("locale_index", 0).coerceIn(orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales.indices)
            val locale = java.util.Locale.forLanguageTag(orinasa.njarasoa.maripanatokana.ui.weather.supportedLocales[localeIndex].tag)
            val config = android.content.res.Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            baseContext.createConfigurationContext(config)
        }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val refreshTime = timeFormat.format(Date(data.timestamp))
    val (tempPrimary, tempSecondary) = data.temperature.displayDual(metricPrimary)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // -- Top row: "Today in {city}" + refresh time --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.widget_now_in, data.locationName),
                    style = TextStyle(
                        color = WidgetColorProviders.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (data.alerts.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "\u26A0\uFE0F ${data.alerts.size}",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF4444), androidx.compose.ui.graphics.Color(0xFFFF4444)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshActionCallback>())
                ) {
                    Text(
                        text = refreshTime,
                        style = TextStyle(
                            color = WidgetColorProviders.onSurfaceVariant,
                            fontSize = 10.sp,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = context.getString(R.string.cd_refresh),
                        modifier = GlanceModifier.size(12.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(WidgetColorProviders.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // -- Bottom row: dual-unit temp + description --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tempPrimary,
                    style = TextStyle(
                        color = WidgetColorProviders.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = tempSecondary,
                    style = TextStyle(
                        color = WidgetColorProviders.onSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(
                    text = "${wmoEmoji(data.weatherCode)} ${context.getString(wmoDescriptionRes(data.weatherCode))}",
                    style = TextStyle(
                        color = WidgetColorProviders.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}
