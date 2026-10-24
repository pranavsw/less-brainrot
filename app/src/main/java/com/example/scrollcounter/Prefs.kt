package com.example.scrollcounter

object Prefs {
    const val SETTINGS = "settings"
    const val COUNTER_STORE = "counter_store"
    const val DIAGNOSTICS = "diagnostics"

    const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    const val KEY_DIAGNOSTICS = "diagnostics"
    const val KEY_DIAGNOSTIC_ENTRIES = "entries"
    const val TRACK_PREFIX = "track_"

    fun trackKey(packageName: String) = "$TRACK_PREFIX$packageName"
}
