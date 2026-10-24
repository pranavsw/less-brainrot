package dev.lessbrainrot

import android.content.SharedPreferences
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val bridge by lazy { Bridge(this) }
    private var counterSink: EventChannel.EventSink? = null
    private val counterListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> pushCounter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences(Prefs.COUNTER_STORE, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(counterListener)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, Bridge.CHANNEL)
            .setMethodCallHandler { call, result -> bridge.handle(call, result) }
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, Bridge.EVENTS_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    counterSink = events
                    pushCounter()
                }

                override fun onCancel(arguments: Any?) {
                    counterSink = null
                }
            })
    }

    override fun onDestroy() {
        getSharedPreferences(Prefs.COUNTER_STORE, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(counterListener)
        super.onDestroy()
    }

    // Counters are written by the accessibility service in this same process; the
    // prefs listener forwards every change to the dashboard without polling.
    private fun pushCounter() {
        val sink = counterSink ?: return
        sink.success(bridge.todayMap())
    }
}
