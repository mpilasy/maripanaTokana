package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.SavedLocation

/**
 * Always-available location switcher: "Current Location" (GPS) plus a persisted list of saved
 * locations, with an inline search (reusing the same forward-geocoding search as
 * LocationOverrideDialog) to add more. Distinct from Expert Mode's LocationOverrideDialog, which
 * remains a separate temporary testing override.
 *
 * Two swipeable pages in one dialog: page 0 picks from Current Location + saved locations, page 1
 * searches and adds a new one. Swipe between them, or use the +/back buttons.
 */
@Composable
fun SavedLocationsDialog(
    savedLocations: List<SavedLocation>,
    activeLocationId: String?,
    searchResults: List<GeocodingResult>,
    onDismissRequest: () -> Unit,
    onSelectCurrentLocation: () -> Unit,
    onSelectSavedLocation: (String) -> Unit,
    onRemoveSavedLocation: (String) -> Unit,
    onAddSearchResult: (GeocodingResult) -> Unit,
    searchQuery: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(text) {
        if (text.length >= 2) searchQuery(text)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (pagerState.currentPage == 1) {
                        IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back_to_locations),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            if (pagerState.currentPage == 0) R.string.locations_title else R.string.locations_add_title
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (pagerState.currentPage == 0) {
                        IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_location),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) { page ->
                    if (page == 0) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCurrentLocation() }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.locations_current),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (activeLocationId == null) 1f else 0.7f),
                                        fontWeight = if (activeLocationId == null) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                if (savedLocations.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                }
                            }
                            items(savedLocations) { loc ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectSavedLocation(loc.id) }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = loc.name,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (activeLocationId == loc.id) 1f else 0.8f),
                                            fontWeight = if (activeLocationId == loc.id) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        if (loc.subtext != null) {
                                            Text(
                                                text = loc.subtext,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onRemoveSavedLocation(loc.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.locations_remove),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.locations_search_hint),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    cursorColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { searchQuery(text) }),
                                trailingIcon = {
                                    if (text.isNotEmpty()) {
                                        IconButton(onClick = { text = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear text",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(searchResults) { result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onAddSearchResult(result)
                                                text = ""
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                    ) {
                                        Column {
                                            Text(text = result.name, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                text = result.displayName(),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f))
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (pagerState.currentPage == index) 0.8f else 0.3f
                                    ),
                                    shape = CircleShape,
                                )
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
