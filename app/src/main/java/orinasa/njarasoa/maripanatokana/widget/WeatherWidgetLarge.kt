package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import orinasa.njarasoa.maripanatokana.MainActivity
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.widget.theme.WidgetColorProviders
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidgetLarge : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetWeatherFetcher.fetch(context)
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val metricPrimary = prefs.getBoolean("metric_primary", true)
        val hasCachedLocation = prefs.getFloat("lat", Float.MIN_VALUE) != Float.MIN_VALUE

        provideContent {
            GlanceTheme {
                if (data != null) {
                    WeatherWidgetLargeContent(data, metricPrimary)
                } else {
                    WidgetError(hasCachedLocation)
                }
            }
        }
    }
}

@Composable
private fun WeatherWidgetLargeContent(data: WeatherData, metricPrimary: Boolean) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
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
        ) {
            // -- Top row: "Today in {city}" + refresh time --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today in ${data.locationName}",
                    style = TextStyle(
                        color = WidgetColorProviders.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = refreshTime,
                    style = TextStyle(
                        color = WidgetColorProviders.onSurfaceVariant,
                        fontSize = 10.sp,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // -- Temperature + description --
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
                    text = "${wmoEmoji(data.weatherCode)} ${data.description}",
                    style = TextStyle(
                        color = WidgetColorProviders.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // -- Detail row: Feels Like / Humidity / Wind --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (flP, flS) = data.feelsLike.displayDual(metricPrimary)
                DetailCell(
                    label = "Feels Like",
                    value = flP,
                    secondaryValue = flS,
                    modifier = GlanceModifier.defaultWeight(),
                )
                DetailCell(
                    label = "Humidity",
                    value = "${data.humidity}%",
                    modifier = GlanceModifier.defaultWeight(),
                )
                val (windP, windS) = data.windSpeed.displayDual(metricPrimary)
                DetailCell(
                    label = "Wind",
                    value = windP,
                    secondaryValue = windS,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // -- Min / Max row --
            val (minP, minS) = data.tempMin.displayDual(metricPrimary)
            val (maxP, maxS) = data.tempMax.displayDual(metricPrimary)
            Text(
                text = "Min $minP ($minS) \u00B7 Max $maxP ($maxS)",
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    color = WidgetColorProviders.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun DetailCell(
    label: String,
    value: String,
    modifier: GlanceModifier = GlanceModifier,
    secondaryValue: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = WidgetColorProviders.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = value,
            style = TextStyle(
                color = WidgetColorProviders.onSurface,
                fontSize = 12.sp,
            ),
        )
        if (secondaryValue != null) {
            Text(
                text = secondaryValue,
                style = TextStyle(
                    color = WidgetColorProviders.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
            )
        }
    }
}
