// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.tv

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.data.remote.GeocodingResult
import orinasa.njarasoa.maripanatokana.domain.model.SavedLocation

/** The saved places and the active place, in the same preferences and format as the phone screens. */
internal class TvPlaceStore(private val prefs: SharedPreferences) {
    private val serializer = ListSerializer(SavedLocation.serializer())

    fun saved(): List<SavedLocation> {
        val raw = prefs.getString("saved_locations", null) ?: return emptyList()
        return runCatching { Json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    fun setSaved(list: List<SavedLocation>) {
        prefs.edit { putString("saved_locations", Json.encodeToString(serializer, list)) }
    }

    /** The id of the active saved place; null for the device location. */
    fun activeId(): String? = prefs.getString("active_location_id", null)

    fun setActiveId(id: String?) {
        prefs.edit { if (id == null) remove("active_location_id") else putString("active_location_id", id) }
    }
}

internal fun GeocodingResult.toSavedLocation() = SavedLocation(
    id = "$latitude,$longitude",
    name = name,
    subtext = listOfNotNull(admin1, country).joinToString(", ").ifBlank { null },
    latitude = latitude,
    longitude = longitude,
)

/**
 * The search, the device location and the saved places. The text field shows only after OK on the search card:
 * a field that takes the D-pad focus opens the keyboard and keeps the focus.
 */
@Composable
internal fun PlacesScreen(
    saved: List<SavedLocation>,
    activeId: String?,
    showDevice: Boolean,
    locationFailed: Boolean,
    search: suspend (String) -> Result<List<GeocodingResult>>,
    onDevice: () -> Unit,
    onSaved: (SavedLocation) -> Unit,
    onAdd: (GeocodingResult) -> Unit,
    onRemove: (SavedLocation) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GeocodingResult>>(emptyList()) }
    var noResults by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val fieldFocus = remember { FocusRequester() }
    val resultFocus = remember { FocusRequester() }

    LaunchedEffect(editing) {
        if (editing) {
            fieldFocus.requestFocus()
            keyboard?.show()
        } else {
            searchFocus.requestFocus()
        }
    }
    LaunchedEffect(results) { if (results.isNotEmpty()) resultFocus.requestFocus() }
    if (editing) BackHandler { editing = false }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
        Text(stringResource(R.string.locations_title), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (editing) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 24.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    editing = false
                    if (query.isNotBlank()) scope.launch {
                        val found = search(query.trim()).getOrDefault(emptyList())
                        results = found
                        noResults = found.isEmpty()
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fieldFocus)
                    .border(3.dp, Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            )
        } else {
            FocusCard(Modifier.fillMaxWidth().focusRequester(searchFocus), onClick = { editing = true }) {
                Text(
                    query.ifBlank { stringResource(R.string.locations_search_hint) },
                    localize = false,
                    color = if (query.isBlank()) Dim else Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (locationFailed) Text(stringResource(R.string.error_get_location), color = Dim, fontSize = 20.sp)
        if (noResults) Text(stringResource(R.string.chart_view_no_data), color = Dim, fontSize = 20.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(results) { i, result ->
                PlaceRow(
                    name = result.name,
                    subtext = listOfNotNull(result.admin1, result.country).joinToString(", "),
                    active = false,
                    modifier = if (i == 0) Modifier.focusRequester(resultFocus) else Modifier,
                    onClick = { onAdd(result) },
                )
            }
            if (showDevice) item {
                PlaceRow(stringResource(R.string.current_location), null, activeId == null, Modifier, onDevice)
            }
            items(saved, key = { it.id }) { place ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceRow(place.displayName, place.subtext, place.id == activeId, Modifier.weight(1f), onClick = { onSaved(place) })
                    FocusCard(
                        Modifier.width(64.dp),
                        onClick = {
                            // The focused row goes away, so the focus must move
                            searchFocus.requestFocus()
                            onRemove(place)
                        },
                    ) {
                        Text("✕", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(name: String, subtext: String?, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FocusCard(modifier.fillMaxWidth(), onClick) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(if (active) "● $name" else name, localize = false, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (!subtext.isNullOrBlank()) Text(subtext, localize = false, color = Dim, fontSize = 16.sp, maxLines = 1)
        }
    }
}
