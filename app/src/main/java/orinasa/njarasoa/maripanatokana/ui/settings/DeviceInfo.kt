// SPDX-FileCopyrightText: Orinasa Njarasoa
// SPDX-License-Identifier: GPL-3.0-or-later

package orinasa.njarasoa.maripanatokana.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import orinasa.njarasoa.maripanatokana.BuildConfig
import orinasa.njarasoa.maripanatokana.R
import java.util.Locale
import java.util.TimeZone

/** One line of the About page's device section: a label and what the system reports for it. */
internal data class DeviceRow(@StringRes val labelRes: Int, val value: String)

/** What Android tells an app about this device without extra permissions; no hardware identifiers. */
internal fun deviceInfo(context: Context): List<DeviceRow> {
    val res = context.resources
    val metrics = res.displayMetrics
    val config = res.configuration
    val rows = mutableListOf<DeviceRow>()
    rows += DeviceRow(R.string.device_model, "${Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.ROOT) }} ${Build.MODEL} (${Build.DEVICE})")
    val patch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) " · ${Build.VERSION.SECURITY_PATCH}" else ""
    rows += DeviceRow(R.string.device_os, "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})$patch")
    rows += DeviceRow(R.string.device_screen, "${metrics.widthPixels} × ${metrics.heightPixels} px · ${config.screenWidthDp} × ${config.screenHeightDp} dp")
    rows += DeviceRow(R.string.device_density, "${metrics.densityDpi} dpi (× ${metrics.density})")
    rows += DeviceRow(R.string.device_font_scale, "× ${android.content.res.Resources.getSystem().configuration.fontScale}")
    // The phone's own languages (the app's context carries the language chosen in the app), then the app's
    val system = android.content.res.Resources.getSystem().configuration.locales
    val systemLocales = (0 until system.size()).joinToString(", ") { system[it].toLanguageTag() }
    rows += DeviceRow(R.string.device_language, "$systemLocales · app: ${config.locales[0].toLanguageTag()}")
    val tz = TimeZone.getDefault()
    val offsetMin = tz.getOffset(System.currentTimeMillis()) / 60_000
    val sign = if (offsetMin >= 0) "+" else "−"
    val minutes = Math.abs(offsetMin) % 60
    rows += DeviceRow(R.string.device_time_zone, "${tz.id} (UTC$sign${Math.abs(offsetMin) / 60}${if (minutes != 0) ":%02d".format(minutes) else ""})")
    rows += DeviceRow(R.string.device_processor, "${Build.SUPPORTED_ABIS.joinToString(", ")} · ${Runtime.getRuntime().availableProcessors()}")
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let { am ->
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        rows += DeviceRow(R.string.device_memory, "%.1f GB".format(Locale.US, info.totalMem / 1_073_741_824.0))
    }
    val versionCode = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    rows += DeviceRow(
        R.string.device_app_build,
        "v${BuildConfig.VERSION_NAME}${versionCode?.let { " ($it)" } ?: ""} · ${BuildConfig.FLAVOR} · ${BuildConfig.GIT_HASH}${if (BuildConfig.DEBUG) " · debug" else ""}",
    )
    val installer = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION") context.packageManager.getInstallerPackageName(context.packageName)
        }
    } catch (e: Exception) {
        null
    }
    rows += DeviceRow(R.string.device_installer, installer ?: "adb")
    rows += DeviceRow(R.string.device_touch, if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)) "multi-touch" else "touch")
    return rows
}
