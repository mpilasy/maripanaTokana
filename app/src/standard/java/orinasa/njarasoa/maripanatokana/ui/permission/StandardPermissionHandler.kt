package orinasa.njarasoa.maripanatokana.ui.permission

import android.Manifest
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import javax.inject.Inject

private const val PERM_PREFS = "perm_prefs"
private const val KEY_REQUESTED = "location_requested"

@OptIn(ExperimentalPermissionsApi::class)
class StandardPermissionHandler @Inject constructor() : PermissionHandler {

    @Composable
    override fun isPermissionGranted(): Boolean {
        val state = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        // Accept either FINE or COARSE (Android 12+ lets users choose Approximate)
        return state.permissions.any { it.status.isGranted }
    }

    @Composable
    override fun isPermissionPermanentlyDenied(): Boolean {
        val ctx = LocalContext.current
        val state = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        if (state.permissions.any { it.status.isGranted }) return false
        val requestedBefore = remember {
            ctx.getSharedPreferences(PERM_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_REQUESTED, false)
        }
        // shouldShowRationale is false both before the first request and after permanent denial;
        // the requestedBefore flag distinguishes the two.
        return requestedBefore && !state.shouldShowRationale
    }

    @Composable
    override fun rememberPermissionRequester(onPermissionGranted: () -> Unit): () -> Unit {
        val ctx = LocalContext.current
        val onGranted = rememberUpdatedState(onPermissionGranted)
        val state = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        // Fire callback when any permission (FINE or COARSE) becomes granted
        val anyGranted = state.permissions.any { it.status.isGranted }
        LaunchedEffect(anyGranted) {
            if (anyGranted) onGranted.value()
        }

        return remember(state) {
            {
                ctx.getSharedPreferences(PERM_PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_REQUESTED, true).apply()
                state.launchMultiplePermissionRequest()
            }
        }
    }
}
