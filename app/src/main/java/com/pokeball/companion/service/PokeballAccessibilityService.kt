package com.pokeball.companion.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.pokeball.companion.util.AppLogger

class PokeballAccessibilityService : AccessibilityService() {

    private val tag = "PokeballAcc"

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppLogger.i(tag, "✅ Accessibility Service CONNECTED & BOUND by Android OS")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        AppLogger.w(tag, "⚠️ Accessibility Service DESTROYED / UNBOUND")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        AppLogger.w(tag, "⚠️ Accessibility Service INTERRUPTED by system")
    }

    /**
     * Dispatches an emulated tap gesture to (x, y).
     */
    fun simulateTap(x: Float, y: Float, durationMs: Long = 120L) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                AppLogger.d(tag, "Tap gesture completed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                AppLogger.w(tag, "Tap gesture CANCELLED by system at ($x, $y)")
            }
        }, null)

        if (!dispatched) {
            AppLogger.e(tag, "Failed to dispatch tap gesture to system queue at ($x, $y)")
        }
    }

    /**
     * Dispatches a directional drag / stick movement gesture.
     */
    fun simulateDrag(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 80L) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                AppLogger.d(tag, "Drag gesture completed to ($endX, $endY)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                AppLogger.w(tag, "Drag gesture CANCELLED by system to ($endX, $endY)")
            }
        }, null)

        if (!dispatched) {
            AppLogger.e(tag, "Failed to dispatch drag gesture to system queue")
        }
    }

    companion object {
        var instance: PokeballAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            if (am == null) {
                AppLogger.e("PokeballAcc", "AccessibilityManager is null")
                return false
            }
            val enabledServices = am.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            val expectedName = PokeballAccessibilityService::class.java.name
            val isMatch = enabledServices.any { it.resolveInfo.serviceInfo.name == expectedName }

            if (!isMatch) {
                val serviceNames = enabledServices.map { it.resolveInfo.serviceInfo.name.substringAfterLast('.') }
                AppLogger.d("PokeballAcc", "Accessibility check: DISABLED. Currently active in OS: $serviceNames")
            }
            return isMatch
        }
    }
}
