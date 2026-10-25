package dev.lessbrainrot

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Floating counter shown over supported feeds: the plant-health icon for the
 * current usage state plus today's count. The plant crossfades when the state
 * worsens; the count pops on every increment.
 */
class FloatingCounterOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var card: LinearLayout? = null
    private var plant: ImageView? = null
    private var countText: TextView? = null
    private var lastState: PlantState? = null
    private var lastCount = -1

    fun show(count: Int, state: PlantState): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            return false
        }
        var current = card
        if (current == null) {
            current = createCard()
            try {
                windowManager.addView(current, overlayParams())
            } catch (e: WindowManager.BadTokenException) {
                // Permission can be revoked between the canDrawOverlays check and addView.
                return false
            }
            card = current
        }
        render(count, state)
        current.visibility = LinearLayout.VISIBLE
        return true
    }

    fun hide() {
        val current = card ?: return
        card = null
        lastState = null
        lastCount = -1
        try {
            windowManager.removeView(current)
        } catch (e: IllegalArgumentException) {
            // The system detaches the window itself when overlay permission is revoked;
            // removing an already-detached view would crash the service.
        }
    }

    private fun render(count: Int, state: PlantState) {
        val plantView = plant ?: return
        val textView = countText ?: return
        textView.text = "$count"

        if (state != lastState) {
            val worsened = lastState != null && state.ordinal > lastState!!.ordinal
            plantView.setImageResource(state.drawableRes)
            // Crossfade on any state change; a heavier drop when the plant gets worse.
            plantView.alpha = 0f
            plantView.scaleX = 0.8f
            plantView.scaleY = 0.8f
            plantView.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(if (worsened) 350L else 220L)
                .start()
            lastState = state
        }
        if (count != lastCount) {
            val pop = AnimatorSet()
            pop.playTogether(
                ObjectAnimator.ofFloat(textView, "scaleX", 1f, 1.25f, 1f),
                ObjectAnimator.ofFloat(textView, "scaleY", 1f, 1.25f, 1f)
            )
            pop.duration = 180L
            pop.start()
            lastCount = count
        }
    }

    private fun createCard(): LinearLayout {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics
        ).toInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.argb(250, 20, 22, 28))
                cornerRadius = 26f * density
                setStroke(
                    (1 * density).toInt(),
                    Color.argb(255, 42, 47, 58)
                )
            }
            plant = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(62), dp(72))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }.also { addView(it) }
            countText = TextView(context).apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dp(8), 0, 0, 0)
            }.also { addView(it) }
        }
    }

    private fun overlayParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Top-center: clear of the camera cutout but above feed content.
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (32 * density).toInt()
        }
    }
}
