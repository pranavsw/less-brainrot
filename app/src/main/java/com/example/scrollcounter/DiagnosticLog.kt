package com.example.scrollcounter

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticLog(private val context: Context, private val capacity: Int = 200) {
    private val entries = ArrayDeque<String>(capacity)
    var enabled: Boolean = false

    init {
        context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
            .getString("entries", null)
            ?.lines()
            ?.takeLast(capacity)
            ?.forEach(entries::addLast)
    }

    fun record(event: AccessibilityEvent, observation: ScrollObservation, counted: Boolean, reason: String) {
        if (!enabled) return
        if (entries.size == capacity) entries.removeFirst()
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        entries.addLast("$time package=${observation.packageName} type=${event.eventType} " +
            "viewId=${observation.viewId ?: "-"} class=${observation.className ?: "-"} " +
            "scrollable=${observation.scrollable} dx=${observation.deltaX} dy=${observation.deltaY} " +
            "from=${observation.fromIndex} to=${observation.toIndex} count=${observation.itemCount} " +
            "state=${observation.classification.state} evidence=${observation.classification.evidence} " +
            "counted=$counted reason=$reason " +
            "approximate=${observation.isYouTubeFallbackEvent}")
        context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
            .edit()
            .putString("entries", export())
            .apply()
    }

    fun export(): String = entries.joinToString("\n")
}
