package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.AlertLevel
import orinasa.njarasoa.maripanatokana.domain.model.WeatherAlert
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.weather.LocalScale
import orinasa.njarasoa.maripanatokana.ui.weather.s
import orinasa.njarasoa.maripanatokana.ui.weather.sd

@Composable
fun WeatherAlertBanner(
    alerts: List<WeatherAlert>,
    localizeDigits: (String) -> String,
) {
    if (alerts.isEmpty()) return

    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val scale = LocalScale.current
    val bodyFont = LocalBodyFont.current
    val displayFont = LocalDisplayFont.current

    val topLevel = when {
        alerts.any { it.level == AlertLevel.EMERGENCY } -> AlertLevel.EMERGENCY
        alerts.any { it.level == AlertLevel.WARNING } -> AlertLevel.WARNING
        else -> AlertLevel.WATCH
    }

    val bannerColor = when (topLevel) {
        AlertLevel.WATCH -> Color(0xFFFFA500).copy(alpha = 0.15f)
        else -> Color(0xFFFF4444).copy(alpha = 0.15f)
    }

    val topAlert = alerts.find { it.level == topLevel } ?: alerts.first()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.sd(scale))
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.sd(scale)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (topLevel == AlertLevel.WATCH) "\u26A0\uFE0F" else "\u2757",
                        fontSize = 18f.s(scale)
                    )
                    Spacer(modifier = Modifier.width(8.sd(scale)))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val title = context.resources.getIdentifier(topAlert.titleKey, "string", context.packageName).let { id ->
                                if (id != 0) stringResource(id) else topAlert.titleKey
                            }
                            Text(
                                text = title,
                                fontSize = 16f.s(scale),
                                fontWeight = FontWeight.Bold,
                                fontFamily = displayFont,
                                color = Color.White
                            )
                            if (topAlert.source != "derived") {
                                Spacer(modifier = Modifier.width(6.sd(scale)))
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = topAlert.source.uppercase(),
                                        fontSize = 10f.s(scale),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.sd(scale), vertical = 2.sd(scale))
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.sd(scale), vertical = 4.sd(scale))
                ) {
                    Text(
                        text = stringResource(if (expanded) R.string.alert_hide_details else R.string.alert_show_details),
                        fontSize = 12f.s(scale),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = bodyFont,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(6.sd(scale)))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(10.sd(scale))
                            .rotate(if (expanded) 180f else 0f),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.sd(scale), end = 16.sd(scale), bottom = 16.sd(scale)),
                    verticalArrangement = Arrangement.spacedBy(12.sd(scale))
                ) {
                    alerts.forEachIndexed { index, alert ->
                        if (index > 0) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val title = context.resources.getIdentifier(alert.titleKey, "string", context.packageName).let { id ->
                                    if (id != 0) stringResource(id) else alert.titleKey
                                }
                                Text(
                                    text = title,
                                    fontSize = 14f.s(scale),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = bodyFont,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (alert.source != "derived") {
                                    Spacer(modifier = Modifier.width(6.sd(scale)))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = alert.source.uppercase(),
                                            fontSize = 9f.s(scale),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.sd(scale), vertical = 1.sd(scale))
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.sd(scale)))
                            val desc = context.resources.getIdentifier(alert.descKey, "string", context.packageName).let { id ->
                                if (id != 0) stringResource(id) else alert.descKey
                            }
                            Text(
                                text = desc,
                                fontSize = 13f.s(scale),
                                fontFamily = bodyFont,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18f.s(scale)
                            )
                        }
                    }
                }
            }
        }
    }
}
