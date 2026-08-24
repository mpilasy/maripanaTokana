package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.ActivityIndices
import orinasa.njarasoa.maripanatokana.domain.model.ActivityTier
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont

private data class ActivityColors(val background: Color, val text: Color)

private fun colorsFor(tier: ActivityTier): ActivityColors = when (tier) {
    ActivityTier.EXCELLENT -> ActivityColors(Color(0xFF299501), Color.White)
    ActivityTier.GOOD -> ActivityColors(Color(0xFF0088CC), Color.White)
    ActivityTier.FAIR -> ActivityColors(Color(0xFFF7E401), Color(0xFF1A1A1A))
    ActivityTier.POOR -> ActivityColors(Color(0xFFF85900), Color.White)
}

@Composable
private fun tierLabel(tier: ActivityTier): String = when (tier) {
    ActivityTier.EXCELLENT -> stringResource(R.string.activity_tier_excellent)
    ActivityTier.GOOD -> stringResource(R.string.activity_tier_good)
    ActivityTier.FAIR -> stringResource(R.string.activity_tier_fair)
    ActivityTier.POOR -> stringResource(R.string.activity_tier_poor)
}

@Composable
fun ActivitiesCard(
    indices: ActivityIndices,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x992A1FA5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActivityRow(
            icon = "🏃",
            title = stringResource(R.string.activity_running),
            tier = indices.running
        )
        ActivityRow(
            icon = "🧺",
            title = stringResource(R.string.activity_laundry),
            tier = indices.laundry
        )
        ActivityRow(
            icon = "☀️",
            title = stringResource(R.string.activity_uv_safety),
            tier = indices.uvSafety
        )
    }
}

@Composable
private fun ActivityRow(
    icon: String,
    title: String,
    tier: ActivityTier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 18.sp)
            Text(
                text = title,
                fontFamily = LocalBodyFont.current,
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        val colors = colorsFor(tier)
        Text(
            text = tierLabel(tier),
            fontFamily = LocalDisplayFont.current,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
