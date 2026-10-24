package dev.lessbrainrot

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.time.LocalDate

/**
 * Single platform channel surface for the Flutter UI. The native side owns all
 * storage and Android integrations; Flutter only reads and invokes.
 */
class Bridge(private val activity: Activity) {

    private val context = activity.applicationContext
    private val store = CounterStore(context)
    private val settings: SharedPreferences
        get() = context.getSharedPreferences(Prefs.SETTINGS, Activity.MODE_PRIVATE)

    fun handle(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getStatus" -> result.success(statusMap())
            "getToday" -> result.success(todayMap())
            "getHistory" -> {
                val days = (call.arguments as? Number)?.toInt() ?: 7
                result.success(store.history(days).map { day -> day.toMap() })
            }
            "setLimit" -> settings.edit().putInt(Prefs.KEY_LIMIT, intArg(call)).apply().let { result.success(null) }
            "setTrackEnabled" -> {
                val pkg = call.argument<String>("package") ?: ""
                settings.edit()
                    .putBoolean(Prefs.trackKey(pkg), call.argument<Boolean>("value") ?: false)
                    .apply()
                result.success(null)
            }
            "setOverlayEnabled" -> settings.edit().putBoolean(Prefs.KEY_OVERLAY_ENABLED, boolArg(call)).apply().let { result.success(null) }
            "setNotificationsEnabled" -> settings.edit().putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, boolArg(call)).apply().let { result.success(null) }
            "setDiagnosticsEnabled" -> settings.edit().putBoolean(Prefs.KEY_DIAGNOSTICS, boolArg(call)).apply().let { result.success(null) }
            "setOnboarded" -> settings.edit().putBoolean(Prefs.KEY_ONBOARDED, true).apply().let { result.success(null) }
            "resetCounters" -> { store.reset(); result.success(null) }
            "deleteAllData" -> {
                context.getSharedPreferences(Prefs.COUNTER_STORE, Activity.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences(Prefs.DIAGNOSTICS, Activity.MODE_PRIVATE).edit().clear().apply()
                settings.edit().clear().apply()
                result.success(null)
            }
            "openAccessibilitySettings" -> activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)).let { result.success(null) }
            "openOverlaySettings" -> activity.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            ).let { result.success(null) }
            "requestBatteryExemption" -> {
                val pm = context.getSystemService(PowerManager::class.java)
                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
                result.success(null)
            }
            "requestNotificationPermission" -> { requestNotificationPermission(); result.success(null) }
            "exportDiagnostics" -> result.success(TrackingStatus.diagnosticsLog?.export().orEmpty())
            else -> result.notImplemented()
        }
    }

    fun statusMap(): Map<String, Any?> = mapOf(
        "serviceEnabled" to isServiceEnabled(),
        "overlayPermission" to Settings.canDrawOverlays(context),
        "batteryExempt" to context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
        "notificationsEnabled" to
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled(),
        "limit" to settings.getInt(Prefs.KEY_LIMIT, 0),
        "overlayEnabled" to settings.getBoolean(Prefs.KEY_OVERLAY_ENABLED, true),
        "notificationsPreference" to settings.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, true),
        "diagnosticsEnabled" to settings.getBoolean(Prefs.KEY_DIAGNOSTICS, false),
        "onboarded" to settings.getBoolean(Prefs.KEY_ONBOARDED, false),
        "trackApps" to SupportedPackages.all.associateWith {
            settings.getBoolean(Prefs.trackKey(it), true)
        },
        "currentClassification" to TrackingStatus.current?.let {
            mapOf("state" to it.state.name, "reason" to it.reason)
        },
        "appVersion" to versionName(),
        "limitReachedToday" to hasReachedLimitToday()
    )

    fun todayMap(): Map<String, Any> = mapOf(
        "total" to store.total(),
        "perApp" to SupportedPackages.all.associate { it to store.appTotal(it) }
    )

    private fun hasReachedLimitToday(): Boolean {
        val limit = settings.getInt(Prefs.KEY_LIMIT, 0)
        val notified = settings.getString(Prefs.KEY_LIMIT_NOTIFIED_DATE, null)
        return limit > 0 && store.total() >= limit && notified == LocalDate.now().toString()
    }

    private fun isServiceEnabled(): Boolean =
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains("${context.packageName}/") == true

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }
    }

    private fun versionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }

    private fun DayCount.toMap(): Map<String, Any> = mapOf(
        "date" to date,
        "total" to total,
        "perApp" to perApp
    )

    companion object {
        const val CHANNEL = "scrollaware/bridge"
        const val EVENTS_CHANNEL = "scrollaware/counter"
        const val REQUEST_NOTIFICATIONS = 4001

        private fun intArg(call: MethodCall): Int = (call.arguments as? Number)?.toInt() ?: 0
        private fun boolArg(call: MethodCall): Boolean = (call.arguments as? Boolean) ?: false
    }
}
