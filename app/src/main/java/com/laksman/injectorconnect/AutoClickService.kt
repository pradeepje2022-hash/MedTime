package com.laksman.injectorconnect

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AutoClickService : AccessibilityService() {

    companion object {
        const val INJECTOR_PACKAGE = "com.evozi.injector"
        private const val POLL_MS = 600L
        private const val TIMEOUT_MS = 25_000L

        // Connect click karala passe Home ekata yanna kalin balan inna welawa
        private const val GO_HOME_DELAY_MS = 2500L

        // Disconnect click karala passe app eka close karanna kalin balan inna welawa
        private const val CLOSE_DELAY_MS = 1500L

        // "Clear all" button eke nam (phone eka anuwa wenas wenawa)
        private val CLEAR_LABELS = listOf(
            "clear all", "close all", "clear", "close all apps", "remove all"
        )

        @Volatile
        var instance: AutoClickService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var deadline = 0L
    private var waiting = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    /**
     * Toggle: HTTP Injector open karanawa.
     * Stop thiyenawanam (connect wela) -> Stop click (disconnect) + recents eken app eka ain karanawa.
     * Start thiyenawanam -> Start click (connect) + Home ekata yanawa.
     */
    fun startConnect(): Boolean {
        val launch = packageManager.getLaunchIntentForPackage(INJECTOR_PACKAGE) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)

        handler.removeCallbacksAndMessages(null)
        waiting = true
        deadline = System.currentTimeMillis() + TIMEOUT_MS
        handler.postDelayed(pollRunnable, 1500L)
        return true
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!waiting) return
            if (System.currentTimeMillis() > deadline) {
                waiting = false
                toast("Start/Stop button eka hoyaganna beri una")
                return
            }
            val root = rootInActiveWindow
            if (root != null && root.packageName == INJECTOR_PACKAGE) {
                if (clickText(root, "stop")) {
                    waiting = false
                    toast("Disconnecting...")
                    removeFromRecentsLater()
                    return
                }
                if (clickText(root, "start")) {
                    waiting = false
                    toast("Connecting...")
                    goHomeLater()
                    return
                }
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    private fun goHomeLater() {
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
        }, GO_HOME_DELAY_MS)
    }

    /**
     * Disconnect wela passe:
     * Home -> Recents -> "Clear all" button eka (nathnam card eka uda swipe) -> Home.
     */
    private fun removeFromRecentsLater() {
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
        }, CLOSE_DELAY_MS)

        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_RECENTS)
        }, CLOSE_DELAY_MS + 800L)

        handler.postDelayed({
            if (!clickClearAll()) {
                swipeUpCard()
            }
        }, CLOSE_DELAY_MS + 2200L)

        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
        }, CLOSE_DELAY_MS + 3600L)
    }

    /** Recents screen eke "Clear all" wage button ekak thiyenawanam click karanawa. */
    private fun clickClearAll(): Boolean {
        val root = rootInActiveWindow ?: return false
        for (label in CLEAR_LABELS) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                var n: AccessibilityNodeInfo? = node
                while (n != null) {
                    if (n.isClickable && n.isEnabled) {
                        if (n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    }
                    n = n.parent
                }
            }
        }
        return false
    }

    /** Recents eke kalin use karapu app eke card eka uda swipe karala ain karanawa. */
    private fun swipeUpCard() {
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val startY = dm.heightPixels * 0.55f
        val endY = dm.heightPixels * 0.10f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun clickText(root: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText(text)
            .filter { it.text?.toString()?.trim().equals(text, ignoreCase = true) }
        for (node in nodes) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) {
                if (n.isClickable && n.isEnabled) {
                    return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                n = n.parent
            }
        }
        return false
    }

    private fun toast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}
