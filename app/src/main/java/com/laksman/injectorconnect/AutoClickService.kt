package com.laksman.injectorconnect

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
     * Stop button eka thiyenawanam (connect wela) -> Stop click (disconnect).
     * Start button eka thiyenawanam -> Start click (connect).
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
                // Connected nam Stop button eka penawa -> disconnect
                if (clickText(root, "stop")) {
                    waiting = false
                    toast("Disconnecting...")
                    return
                }
                // Disconnected nam Start button eka penawa -> connect
                if (clickText(root, "start")) {
                    waiting = false
                    toast("Connecting...")
                    return
                }
            }
            handler.postDelayed(this, POLL_MS)
        }
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
