package orinasa.njarasoa.maripanatokana.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FDroidPermissionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionHandler {
    @Composable
    override fun isPermissionGranted(): Boolean {
        val ctx = LocalContext.current
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    override fun rememberPermissionRequester(onPermissionGranted: () -> Unit): () -> Unit {
        val ctx = LocalContext.current
        val onGranted = rememberUpdatedState(onPermissionGranted)
        var granted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }

        LaunchedEffect(granted) {
            if (granted) onGranted.value()
        }

        return remember(launcher) {
            {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}
