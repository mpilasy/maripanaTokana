package orinasa.njarasoa.maripanatokana.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
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
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.WeatherData
import orinasa.njarasoa.maripanatokana.widget.theme.WidgetColorProviders
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherWidgetLarge : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetWeatherFetcher.fetch(context)

        provideContent {
            GlanceTheme {
                if (data != null) {
                    WeatherWidgetLargeContent(data)
                } else {
                    WidgetError()
                }
            }
        }
    }
}

@Composable
private fun WeatherWidgetLargeContent(data: WeatherData) {
    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    val today = dateFormat.format(Date(data.timestamp))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            // -- Top row: "Today" + date --
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

            // -- Temperature + description + location --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.temperature.displayDual(),
                    style = TextStyle(
                        color = WidgetColorProviders.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.description,
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

            Spacer(modifier = GlanceModifier.height(8.dp))

            // -- Detail row: Feels Like / Humidity / Wind --
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailCell(
                    label = "Feels Like",
                    value = data.feelsLike.displayDual(),
                    modifier = GlanceModifier.defaultWeight(),
                )
                DetailCell(
                    label = "Humidity",
                    value = "${data.humidity}%",
                    modifier = GlanceModifier.defaultWeight(),
                )
                DetailCell(
                    label = "Wind",
                    value = data.windSpeed.displayMetric(),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // -- Min / Max row --
            Text(
                text = "Min ${data.tempMin.displayCelsius()} · Max ${data.tempMax.displayCelsius()}",
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
    }
}
