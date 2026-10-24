package com.example.scrollcounter

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CounterStore(context: Context) {
    private val prefs = context.getSharedPreferences("counter_store", Context.MODE_PRIVATE)
    private val date: String get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun increment(packageName: String) {
        normalize()
        prefs.edit()
            .putInt("total", prefs.getInt("total", 0) + 1)
            .putInt("$packageName.total", prefs.getInt("$packageName.total", 0) + 1)
            .apply()
    }

    fun total(): Int { normalize(); return prefs.getInt("total", 0) }
    fun appTotal(packageName: String): Int { normalize(); return prefs.getInt("$packageName.total", 0) }
    fun reset() = prefs.edit().putString("date", date).putInt("total", 0)
        .putInt("${SupportedPackages.INSTAGRAM}.total", 0)
        .putInt("${SupportedPackages.YOUTUBE}.total", 0).apply()

    private fun normalize() {
        if (prefs.getString("date", null) != date) reset()
    }
}
