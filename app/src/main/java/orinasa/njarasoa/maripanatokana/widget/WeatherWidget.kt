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
import androidx.glance.text.TextStyle
import orinasa.njarasoa.maripanatokana.MainActivity
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.wmoEmoji
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.widget.theme.WidgetColorProviders
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetWeatherFetcher.fetch(context)
        val metricPrimary = context
            .getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .getBoolean("metric_primary", true)

        provideContent {
            GlanceTheme {
                if (data != null) {
                    WeatherWidgetContent(data, metricPrimary)
                } else {
                    WidgetError()
                }
            }
        }
    }
}

@Composable
internal fun WidgetError() {
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
            text = "Open app to enable location",
            style = TextStyle(
                color = WidgetColorProviders.onSurfaceVariant,
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun WeatherWidgetContent(data: WeatherData, metricPrimary: Boolean) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    val today = dateFormat.format(Date(data.timestamp))
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
            // -- Top row: "Today" label + date --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        color = WidgetColorProviders.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = today,
                    style = TextStyle(
                        color = WidgetColorProviders.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // -- Bottom row: dual-unit temp + description + location --
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
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "${wmoEmoji(data.weatherCode)} ${data.description}",
                        style = TextStyle(
                            color = WidgetColorProviders.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    )
                    Text(
                        text = data.locationName,
                        style = TextStyle(
                            color = WidgetColorProviders.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
    }
}
