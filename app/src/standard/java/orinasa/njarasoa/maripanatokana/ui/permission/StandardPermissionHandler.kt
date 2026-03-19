package orinasa.njarasoa.maripanatokana.ui.permission

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    override fun RequestPermission(onPermissionGranted: () -> Unit) {
        val state = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        LaunchedEffect(state.allPermissionsGranted) {
            if (state.allPermissionsGranted) {
                onPermissionGranted()
            }
        }

        LaunchedEffect(Unit) {
            if (!state.allPermissionsGranted) {
                state.launchMultiplePermissionRequest()
            }
        }
    }
}
