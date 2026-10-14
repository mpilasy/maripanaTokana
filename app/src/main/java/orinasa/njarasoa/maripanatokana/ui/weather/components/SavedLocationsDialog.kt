package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalBodyFont
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult

/**
 * Plain location search: always opens blank, searches as you type, tapping a result switches
 * the displayed weather to it (a preview — it is not saved). Saving to favorites happens
 * separately, via the header's favorite icon on the currently-displayed location.
 */
@Composable
fun SavedLocationsDialog(
    searchResults: List<GeocodingResult>,
    onDismissRequest: () -> Unit,
    onSelectSearchResult: (GeocodingResult) -> Unit,
    searchQuery: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        if (text.length >= 2) searchQuery(text)
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LocalizedDialog(onDismissRequest = onDismissRequest) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1565), shape)
                .border(1.dp, Hairline, shape)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.locations_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalDisplayFont.current,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        stringResource(R.string.locations_search_hint),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                    )
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = TileSurface,
                    unfocusedContainerColor = TileSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchQuery(text) }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear text",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
            )

            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    itemsIndexed(searchResults) { index, result ->
                        if (index > 0) {
                            Box(Modifier.fillMaxWidth().padding(start = 44.dp).height(1.dp).background(Hairline))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSearchResult(result) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Map for a region or country, pin for a town or city
                            Icon(
                                painter = if (result.isArea) painterResource(R.drawable.ic_map) else rememberVectorPainter(Icons.Default.LocationOn),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = result.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = LocalBodyFont.current,
                                )
                                Text(
                                    text = result.displayName(),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
