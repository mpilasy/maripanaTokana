package orinasa.njarasoa.maripanatokana.ui.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orinasa.njarasoa.maripanatokana.R
import orinasa.njarasoa.maripanatokana.domain.model.SavedLocation
import orinasa.njarasoa.maripanatokana.ui.theme.Hairline
import orinasa.njarasoa.maripanatokana.ui.theme.LocalDisplayFont
import orinasa.njarasoa.maripanatokana.ui.theme.TileSurface

/** Names a favorite. Saving a blank name, or Reset, goes back to the place name. */
@Composable
fun RenameLocationDialog(
    location: SavedLocation,
    onDismissRequest: () -> Unit,
    onRename: (String?) -> Unit,
) {
    var text by remember {
        mutableStateOf(TextFieldValue(location.displayName, TextRange(0, location.displayName.length)))
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
                text = stringResource(R.string.rename_title),
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
                        location.name,
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRename(text.text) }),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (location.label != null) {
                    TextButton(onClick = { onRename(null) }) {
                        Text(stringResource(R.string.rename_reset), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.rename_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                TextButton(onClick = { onRename(text.text) }) {
                    Text(stringResource(R.string.rename_save), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
