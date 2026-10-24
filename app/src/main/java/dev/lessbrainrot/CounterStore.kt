package dev.lessbrainrot

import android.content.Context
import android.content.SharedPreferences
import java.time.Clock
import java.time.LocalDate

class CounterStore(
    private val prefs: SharedPreferences,
    private val clock: Clock
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(Prefs.COUNTER_STORE, Context.MODE_PRIVATE),
        Clock.systemDefaultZone()
    )

    private val today: String get() = LocalDate.now(clock).toString()

    fun increment(packageName: String) {
        normalize()
        prefs.edit()
            .putInt(KEY_TOTAL, prefs.getInt(KEY_TOTAL, 0) + 1)
            .putInt(appKey(packageName), prefs.getInt(appKey(packageName), 0) + 1)
            .apply()
    }

    fun total(): Int { normalize(); return prefs.getInt(KEY_TOTAL, 0) }
    fun appTotal(packageName: String): Int { normalize(); return prefs.getInt(appKey(packageName), 0) }

    fun reset() {
        val editor = prefs.edit().putString(KEY_DATE, today).putInt(KEY_TOTAL, 0)
        SupportedPackages.all.forEach { editor.putInt(appKey(it), 0) }
        editor.apply()
    }

    fun history(days: Int): List<DayCount> {
        normalize()
        val base = LocalDate.now(clock)
        return (days - 1 downTo 0).map { offset ->
            val day = base.minusDays(offset.toLong())
            val dateKey = day.toString()
            if (offset == 0) {
                // Today's counts are still live; the archive only holds past days.
                DayCount(dateKey, total(), SupportedPackages.all.associateWith { appTotal(it) })
            } else {
                DayCount(
                    date = dateKey,
                    total = prefs.getInt("$HIST_PREFIX$dateKey.$KEY_TOTAL", 0),
                    perApp = SupportedPackages.all.associateWith {
                        prefs.getInt("$HIST_PREFIX$dateKey.$it", 0)
                    }
                )
            }
        }
    }

    private fun normalize() {
        val stored = prefs.getString(KEY_DATE, null)
        if (stored != today) {
            if (stored != null) archive(stored)
            reset()
        }
    }

    // Rollover keeps history: yesterday's totals are archived under their own date
    // before the live counters are zeroed for the new day.
    private fun archive(dateKey: String) {
        val editor = prefs.edit()
            .putInt("$HIST_PREFIX$dateKey.$KEY_TOTAL", prefs.getInt(KEY_TOTAL, 0))
        SupportedPackages.all.forEach {
            editor.putInt("$HIST_PREFIX$dateKey.$it", prefs.getInt(appKey(it), 0))
        }
        editor.apply()
    }

    companion object {
        private const val KEY_TOTAL = "total"
        private const val KEY_DATE = "date"
        private const val HIST_PREFIX = "hist."
        private fun appKey(packageName: String) = "$packageName.total"
    }
}
