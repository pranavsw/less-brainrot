package dev.lessbrainrot

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class TrackingAccessibilityService : AccessibilityService() {
    private lateinit var store: CounterStore
    private lateinit var detector: FeedDetector
    private lateinit var counter: GestureCounter
    private lateinit var diagnostics: DiagnosticLog
    private lateinit var overlay: FloatingCounterOverlay
    private lateinit var limitNotifier: LimitNotifier
    private var activePackage: String? = null
    private var overlayPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val retireOverlay = Runnable {
        if (::overlay.isInitialized) {
            overlay.hide()
            overlayPackage = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = CounterStore(this)
        detector = FeedDetector()
        counter = GestureCounter()
        counter.reset()
        diagnostics = DiagnosticLog(this)
        diagnostics.enabled = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("diagnostics", false)
        overlay = FloatingCounterOverlay(this)
        limitNotifier = LimitNotifier(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::detector.isInitialized) return
        diagnostics.enabled = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("diagnostics", false)
        val packageName = event.packageName?.toString() ?: return
        val enabled = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("track_$packageName", true)
        if (packageName !in SupportedPackages.all || !enabled) {
            // System overlay windows (keyboard, status bar, One UI overlays) fire
            // accessibility events constantly while a feed is open. Reacting to them
            // re-arms the gesture warm-up mid-scroll and kills counting, so they are
            // ignored; the overlay retires itself via retireOverlay once feed events
            // stop arriving.
            return
        }
        if (packageName != activePackage) {
            activePackage = packageName
            overlay.hide()
            overlayPackage = null
            counter.reset()
        }
        val source = event.source
        val classification = detector.classify(event, source)
        TrackingStatus.current = classification
        val observation = detector.observation(event, source, classification)

        val decision = counter.decide(observation)
        val counted = decision.counted
        if (counted) {
            store.increment(packageName)
            limitNotifier.maybeNotify(store.total())
        }
        val overlayEnabled = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("overlay_enabled", true)
        val shouldShowOverlay = overlayPackage == packageName ||
            detector.isFeedContextConfirmed(packageName) ||
            packageName == SupportedPackages.YOUTUBE
        if (overlayEnabled && shouldShowOverlay) {
            val limit = getSharedPreferences("settings", MODE_PRIVATE)
                .getInt(Prefs.KEY_LIMIT, 0)
            if (overlay.show(store.total(), PlantState.forCount(store.total(), limit))) {
                overlayPackage = packageName
            }
        } else if (!overlayEnabled) {
            overlay.hide()
            overlayPackage = null
        }
        handler.removeCallbacks(retireOverlay)
        handler.postDelayed(retireOverlay, OVERLAY_RETIRE_MS)
        diagnostics.record(event, observation, counted, decision.reason)
        TrackingStatus.diagnostics = diagnostics.export()
    }

    override fun onInterrupt() {
        handler.removeCallbacks(retireOverlay)
        if (::overlay.isInitialized) overlay.hide()
        if (::counter.isInitialized) counter.reset()
    }

    override fun onDestroy() {
        handler.removeCallbacks(retireOverlay)
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }

    companion object {
        // While a feed is open its events keep arriving (video progress, content
        // changes), so the overlay stays; 2.5s of silence means the feed was left.
        private const val OVERLAY_RETIRE_MS = 2_500L
    }
}
