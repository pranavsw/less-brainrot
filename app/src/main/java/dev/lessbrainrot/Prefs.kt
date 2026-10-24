package dev.lessbrainrot

object Prefs {
    const val SETTINGS = "settings"
    const val COUNTER_STORE = "counter_store"
    const val DIAGNOSTICS = "diagnostics"

    const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    const val KEY_DIAGNOSTICS = "diagnostics"
    const val KEY_DIAGNOSTIC_ENTRIES = "entries"
    const val TRACK_PREFIX = "track_"

    const val KEY_LIMIT = "daily_limit"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_ONBOARDED = "onboarded"
    const val KEY_LIMIT_NOTIFIED_DATE = "limit_notified_date"

    fun trackKey(packageName: String) = "$TRACK_PREFIX$packageName"
}
