package com.example.scrollcounter

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    private lateinit var store: CounterStore
    private lateinit var serviceStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var classification: TextView
    private lateinit var totals: TextView
    private lateinit var instagram: MaterialSwitch
    private lateinit var youtube: MaterialSwitch
    private lateinit var overlay: MaterialSwitch
    private lateinit var diagnostic: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyWindowInsets()
        store = CounterStore(this)
        serviceStatus = findViewById(R.id.serviceStatus)
        overlayStatus = findViewById(R.id.overlayStatus)
        batteryStatus = findViewById(R.id.batteryStatus)
        classification = findViewById(R.id.classification)
        totals = findViewById(R.id.totals)
        instagram = findViewById(R.id.instagramEnabled)
        youtube = findViewById(R.id.youtubeEnabled)
        overlay = findViewById(R.id.overlayEnabled)
        diagnostic = findViewById(R.id.diagnosticEnabled)
        val settings = getSharedPreferences(Prefs.SETTINGS, MODE_PRIVATE)
        bind(instagram, Prefs.trackKey(SupportedPackages.INSTAGRAM), true, settings)
        bind(youtube, Prefs.trackKey(SupportedPackages.YOUTUBE), true, settings)
        bind(overlay, Prefs.KEY_OVERLAY_ENABLED, true, settings)
        bind(diagnostic, Prefs.KEY_DIAGNOSTICS, false, settings)
        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.overlayButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.batteryButton).setOnClickListener { requestBatteryExemption() }
        findViewById<Button>(R.id.resetButton).setOnClickListener { store.reset(); refresh() }
        findViewById<Button>(R.id.diagnosticButton).setOnClickListener { exportDiagnostics() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun applyWindowInsets() {
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun bind(switch: MaterialSwitch, key: String, default: Boolean, settings: SharedPreferences) {
        switch.isChecked = settings.getBoolean(key, default)
        switch.setOnCheckedChangeListener { _, checked -> settings.edit().putBoolean(key, checked).apply() }
    }

    private fun requestBatteryExemption() {
        val power = getSystemService(PowerManager::class.java)
        if (!power.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun refresh() {
        val accessibilityEnabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(packageName) == true
        serviceStatus.text = getString(
            R.string.status_accessibility,
            getString(if (accessibilityEnabled) R.string.state_on else R.string.state_off)
        )
        val overlayAllowed = Settings.canDrawOverlays(this)
        overlayStatus.text = getString(
            R.string.status_overlay,
            getString(if (overlayAllowed) R.string.state_on else R.string.state_off)
        )
        val power = getSystemService(PowerManager::class.java)
        batteryStatus.text = getString(
            R.string.status_battery,
            getString(
                if (power.isIgnoringBatteryOptimizations(packageName)) R.string.battery_state_exempt
                else R.string.battery_state_active
            )
        )
        classification.text = getString(
            R.string.status_classification,
            TrackingStatus.current?.let { "${it.state} (${it.reason})" }
                ?: getString(R.string.status_classification_unknown)
        )
        totals.text = getString(
            R.string.status_totals,
            store.total(),
            store.appTotal(SupportedPackages.INSTAGRAM),
            store.appTotal(SupportedPackages.YOUTUBE)
        )
    }

    private fun exportDiagnostics() {
        val exported = TrackingStatus.diagnosticsLog?.export()?.takeIf { it.isNotEmpty() }
            ?: getSharedPreferences(Prefs.DIAGNOSTICS, MODE_PRIVATE)
                .getString(Prefs.KEY_DIAGNOSTIC_ENTRIES, null)
            ?: getString(R.string.diagnostics_empty)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_subject))
            putExtra(Intent.EXTRA_TEXT, exported)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.settings_export_diagnostics)))
    }
}
