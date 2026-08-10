package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
                Text(
                    text = stringResource(R.string.locations_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(searchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSearchResult(result) }
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
}
