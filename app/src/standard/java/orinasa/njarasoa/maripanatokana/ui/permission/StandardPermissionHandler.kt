package orinasa.njarasoa.maripanatokana.ui.permission

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import javax.inject.Inject

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
        return state.allPermissionsGranted
    }

    @Composable
    override fun rememberPermissionRequester(onPermissionGranted: () -> Unit): () -> Unit {
        val onGranted = rememberUpdatedState(onPermissionGranted)
        val state = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        LaunchedEffect(state.allPermissionsGranted) {
            if (state.allPermissionsGranted) onGranted.value()
        }

        return remember(state) { { state.launchMultiplePermissionRequest() } }
    }
}
