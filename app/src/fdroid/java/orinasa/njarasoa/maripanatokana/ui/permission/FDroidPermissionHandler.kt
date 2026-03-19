package orinasa.njarasoa.maripanatokana.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FDroidPermissionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionHandler {
    @Composable
    override fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    override fun RequestPermission(onPermissionGranted: () -> Unit) {
        // F-Droid version relies on user granting permissions via system settings or 
        // the app's fetchWeather() will trigger a permission check in the ViewModel/Repo.
        // For now, we just call the callback if granted, or do nothing (triggering the PermissionRequired UI).
        val granted = isPermissionGranted()
        LaunchedEffect(granted) {
            if (granted) {
                onPermissionGranted()
            }
        }
    }
}
