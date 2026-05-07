package orinasa.njarasoa.maripanatokana.ui.permission

import androidx.compose.runtime.Composable

interface PermissionHandler {
    @Composable
    fun isPermissionGranted(): Boolean

    /**
     * Composes the state needed to request location permission.
     *
     * @param onPermissionGranted invoked whenever permission transitions to granted.
     * @return a function the UI can call (e.g. from a button onClick) to launch the
     *   system permission dialog. Always re-invokable.
     */
    @Composable
    fun rememberPermissionRequester(onPermissionGranted: () -> Unit): () -> Unit
}
