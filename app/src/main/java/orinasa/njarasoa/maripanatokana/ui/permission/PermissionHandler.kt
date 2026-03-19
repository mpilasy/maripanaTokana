package orinasa.njarasoa.maripanatokana.ui.permission

import androidx.compose.runtime.Composable

interface PermissionHandler {
    @Composable
    fun isPermissionGranted(): Boolean

    @Composable
    fun RequestPermission(onPermissionGranted: () -> Unit)
}
