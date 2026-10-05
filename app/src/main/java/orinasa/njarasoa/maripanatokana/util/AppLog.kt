package orinasa.njarasoa.maripanatokana.util

import android.util.Log
import orinasa.njarasoa.maripanatokana.BuildConfig

/** Debug-only logging. Never log API keys or URLs containing them. Safe in JVM unit tests. */
object AppLog {
    fun d(tag: String, msg: String, t: Throwable? = null) = log { if (t != null) Log.d(tag, msg, t) else Log.d(tag, msg) }

    fun w(tag: String, msg: String, t: Throwable? = null) = log { if (t != null) Log.w(tag, msg, t) else Log.w(tag, msg) }

    private inline fun log(block: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        try {
            block()
        } catch (_: RuntimeException) {
            // android.util.Log is not mocked in JVM unit tests
        }
    }
}
