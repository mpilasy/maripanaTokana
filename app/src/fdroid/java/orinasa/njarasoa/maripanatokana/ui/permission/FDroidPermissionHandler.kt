package orinasa.njarasoa.maripanatokana.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val PERM_PREFS = "perm_prefs"
private const val KEY_REQUESTED = "location_requested"

class FDroidPermissionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionHandler {

    @Composable
    override fun isPermissionGranted(): Boolean {
        val ctx = LocalContext.current
        return checkGranted(ctx)
    }

    @Composable
    override fun isPermissionPermanentlyDenied(): Boolean {
        val ctx = LocalContext.current
        if (checkGranted(ctx)) return false

        val requestedBefore = ctx.getSharedPreferences(PERM_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_REQUESTED, false)
        if (!requestedBefore) return false

        val activity = remember(ctx) { ctx.findActivity() } ?: return false
        val showFine = ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val showCoarse = ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return !showFine && !showCoarse
    }

    @Composable
    override fun rememberPermissionRequester(onPermissionGranted: () -> Unit): () -> Unit {
        val ctx = LocalContext.current
        val onGranted = rememberUpdatedState(onPermissionGranted)
        var granted by remember {
            mutableStateOf(checkGranted(ctx))
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
                // Mark that we have requested at least once (used by isPermissionPermanentlyDenied)
                ctx.getSharedPreferences(PERM_PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_REQUESTED, true).apply()
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun checkGranted(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
