package dev.lessbrainrot

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class FloatingCounterOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var view: TextView? = null

    fun show(count: Int): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            return false
        }
        var current = view
        if (current == null) {
            current = createOverlayView()
            try {
                windowManager.addView(current, overlayParams())
            } catch (e: WindowManager.BadTokenException) {
                // Permission can be revoked between the canDrawOverlays check and addView.
                return false
            }
            view = current
        }
        current.text = "↕ Scrolls: $count"
        current.visibility = TextView.VISIBLE
        return true
    }

    fun hide() {
        val current = view ?: return
        view = null
        try {
            windowManager.removeView(current)
        } catch (e: IllegalArgumentException) {
            // The system detaches the window itself when overlay permission is revoked;
            // removing an already-detached view would crash the service.
        }
    }

    private fun createOverlayView(): TextView {
        val density = context.resources.displayMetrics.density
        return TextView(context).apply {
            setTextColor(Color.rgb(25, 25, 25))
            textSize = 13f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 8f
            }
            alpha = 0.96f
            isClickable = false
            isFocusable = false
        }
    }

    private fun overlayParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val size = (88 * density).toInt()
        return WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (96 * density).toInt()
        }
    }
}
