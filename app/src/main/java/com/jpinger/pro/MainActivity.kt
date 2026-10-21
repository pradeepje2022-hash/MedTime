package com.jpinger.pro

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private data class Server(val label: String, val host: String, val port: Int)

    // ---- colours (same as the HTML design) ----
    private val cBg = Color.parseColor("#050B18")
    private val cBar = Color.parseColor("#0A1529")
    private val cCard = Color.parseColor("#0D1930")
    private val cCyan = Color.parseColor("#00E5FF")
    private val cBlue = Color.parseColor("#0088FF")
    private val cOk = Color.parseColor("#40C4FF")
    private val cErr = Color.parseColor("#FF5252")

    private lateinit var pingBtn: LinearLayout
    private lateinit var pingText: TextView
    private lateinit var statusText: TextView
    private lateinit var txtMin: TextView
    private lateinit var txtMax: TextView
    private lateinit var txtOk: TextView
    private lateinit var spinnerServer: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var logsList: LinearLayout

    private var isPinging = false
    private val handler = Handler(Looper.getMainLooper())
    private var pingRunnable: Runnable? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var minPing = 0L
    private var maxPing = 0L
    private var okCount = 0

    private val servers = listOf(
        Server("Cloudflare (1.1.1.1)", "1.1.1.1", 443),
        Server("Google DNS (8.8.8.8)", "8.8.8.8", 443),
        Server("Google (google.com)", "www.google.com", 443)
    )
    private val intervalLabels = listOf("500 ms (වේගවත්)", "තත්පර 1 (1000 ms)", "තත්පර 2 (2000 ms)")
    private val intervalValues = longArrayOf(500L, 1000L, 2000L)

    // ---------------------------------------------------------------- helpers
    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun box(fill: Int, stroke: Int, radiusDp: Int, strokeDp: Int = 1) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeDp > 0) setStroke(dp(strokeDp), stroke)
    }

    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

    // ---------------------------------------------------------------- UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBg)
        }

        root.addView(buildAppBar())

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        content.addView(buildPingButton())
        content.addView(buildStatsRow())
        content.addView(buildSettingsCard())
        content.addView(buildLogsBox())
        root.addView(content, lp(MATCH_PARENT, 0, 1f))

        setContentView(root)
    }

    private fun buildAppBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(cBar)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            elevation = dp(4).toFloat()
        }
        val title = TextView(this).apply {
            text = "⚡ J PINGER PRO"
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        bar.addView(title, lp(0, WRAP_CONTENT, 1f))

        val pressed = box(cBlue, cBlue, 6)
        val normal = box(Color.TRANSPARENT, cBlue, 6)
        val reset = TextView(this).apply {
            text = "Reset"
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), pressed)
                addState(intArrayOf(), normal)
            }
            isClickable = true
            setOnClickListener { resetStats() }
        }
        bar.addView(reset, lp(WRAP_CONTENT, WRAP_CONTENT))
        return bar
    }

    private fun circleDrawable(c1: Int, c2: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        gradientType = GradientDrawable.RADIAL_GRADIENT
        gradientRadius = dp(80).toFloat()
        setGradientCenter(0.5f, 0.5f)
        colors = intArrayOf(c1, c2)
        setStroke(dp(3), stroke)
    }

    private fun buildPingButton(): View {
        pingBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = circleDrawable(Color.parseColor("#152642"), Color.parseColor("#091224"), Color.parseColor("#4A6583"))
            isClickable = true
            elevation = dp(6).toFloat()
            setOnClickListener { togglePing() }
        }
        val bolt = TextView(this).apply {
            text = "⚡"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            gravity = Gravity.CENTER
        }
        pingText = TextView(this).apply {
            text = "START"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        statusText = TextView(this).apply {
            text = "නතර වී ඇත"
            setTextColor(Color.parseColor("#BFFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, 0)
        }
        pingBtn.addView(bolt)
        pingBtn.addView(pingText)
        pingBtn.addView(statusText)

        return pingBtn.also {
            it.layoutParams = lp(dp(145), dp(145)).apply { setMargins(0, dp(10), 0, dp(20)) }
        }
    }

    private fun statCard(parent: LinearLayout, label: String, initial: String, last: Boolean): TextView {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = box(cCard, Color.parseColor("#660088FF"), 10)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        card.addView(TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#99FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
        })
        val value = TextView(this).apply {
            text = initial
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        card.addView(value)
        parent.addView(card, lp(0, WRAP_CONTENT, 1f).apply { if (!last) rightMargin = dp(10) })
        return value
    }

    private fun buildStatsRow(): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        txtMin = statCard(row, "අවම (Min)", "0 ms", false)
        txtMax = statCard(row, "උපරිම (Max)", "0 ms", false)
        txtOk = statCard(row, "සාර්ථක (OK)", "0", true)
        row.layoutParams = lp(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) }
        return row
    }

    private fun styledSpinner(items: List<String>): Spinner {
        val s = Spinner(this)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            private fun style(v: View, dropdown: Boolean): View {
                (v as TextView).apply {
                    setTextColor(cCyan)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    if (dropdown) {
                        setBackgroundColor(cBg)
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                    }
                }
                return v
            }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
                style(super.getView(position, convertView, parent), false)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
                style(super.getDropDownView(position, convertView, parent), true)
        }
        s.adapter = adapter
        s.background = box(cBg, cBlue, 6)
        s.setPadding(dp(6), dp(2), dp(6), dp(2))
        s.setPopupBackgroundDrawable(box(cBg, cBlue, 6))
        return s
    }

    private fun settingRow(label: String, spinner: Spinner): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#CCFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }, lp(0, WRAP_CONTENT, 1f))
        row.addView(spinner, lp(WRAP_CONTENT, WRAP_CONTENT))
        return row
    }

    private fun buildSettingsCard(): View {
        spinnerServer = styledSpinner(servers.map { it.label })
        spinnerInterval = styledSpinner(intervalLabels).also { it.setSelection(1) }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(cCard, Color.parseColor("#4D0088FF"), 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        card.addView(settingRow("Server තෝරන්න:", spinnerServer))
        card.addView(View(this).apply { setBackgroundColor(Color.parseColor("#14FFFFFF")) },
            lp(MATCH_PARENT, dp(1)).apply { topMargin = dp(4); bottomMargin = dp(4) })
        card.addView(settingRow("වේගය (Interval):", spinnerInterval))
        card.layoutParams = lp(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) }
        return card
    }

    private fun buildLogsBox(): View {
        val logsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(Color.parseColor("#80000000"), Color.parseColor("#400088FF"), 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        logsBox.addView(TextView(this).apply {
            text = "PING LOGS (LIVE):"
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        })
        logsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(logsList) }
        logsBox.addView(scroll, lp(MATCH_PARENT, 0, 1f))
        logsBox.layoutParams = lp(MATCH_PARENT, 0, 1f).apply { }
        logsBox.minimumHeight = dp(140)
        return logsBox
    }

    // ---------------------------------------------------------------- logic
    private fun togglePing() {
        isPinging = !isPinging
        if (isPinging) {
            pingBtn.background = circleDrawable(cCyan, Color.parseColor("#0044AA"), cCyan)
            pingBtn.elevation = dp(12).toFloat()
            statusText.text = "ක්‍රියාත්මකයි"
            spinnerServer.isEnabled = false
            spinnerInterval.isEnabled = false
            addLog("J Pinger ආරම්භ විය...", true)
            startPingingLoop()
        } else {
            pingBtn.background = circleDrawable(Color.parseColor("#152642"), Color.parseColor("#091224"), Color.parseColor("#4A6583"))
            pingBtn.elevation = dp(6).toFloat()
            pingText.text = "START"
            statusText.text = "නතර වී ඇත"
            spinnerServer.isEnabled = true
            spinnerInterval.isEnabled = true
            stopPingingLoop()
            addLog("J Pinger නතර විය.", true)
        }
    }

    private fun startPingingLoop() {
        val server = servers[spinnerServer.selectedItemPosition]
        val interval = intervalValues[spinnerInterval.selectedItemPosition]

        pingRunnable = object : Runnable {
            override fun run() {
                if (!isPinging) return
                val self = this
                executor.execute {
                    val start = System.currentTimeMillis()
                    val success = try {
                        Socket().use { it.connect(InetSocketAddress(server.host, server.port), 2000) }
                        true
                    } catch (e: IOException) {
                        false
                    }
                    val elapsed = System.currentTimeMillis() - start

                    handler.post {
                        if (!isPinging) return@post
                        if (success) {
                            okCount++
                            if (minPing == 0L || elapsed < minPing) minPing = elapsed
                            if (elapsed > maxPing) maxPing = elapsed
                            pingText.text = "$elapsed ms"
                            updateStats()
                            addLog("Reply received: time=${elapsed}ms [OK]", true)
                        } else {
                            pingText.text = "TIMEOUT"
                            addLog("Request Timed Out [ERROR]", false)
                        }
                        handler.postDelayed(self, interval)
                    }
                }
            }
        }
        handler.post(pingRunnable!!)
    }

    private fun stopPingingLoop() {
        pingRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateStats() {
        txtMin.text = "$minPing ms"
        txtMax.text = "$maxPing ms"
        txtOk.text = "$okCount"
    }

    private fun resetStats() {
        minPing = 0L; maxPing = 0L; okCount = 0
        updateStats()
        logsList.removeAllViews()
        if (!isPinging) pingText.text = "START"
    }

    private fun addLog(msg: String, ok: Boolean) {
        val time = DateFormat.format("HH:mm:ss", Date()).toString()
        val tv = TextView(this).apply {
            text = "[$time] $msg"
            setTextColor(if (ok) cOk else cErr)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), 0, dp(2))
        }
        logsList.addView(tv, 0)
        if (logsList.childCount > 50) logsList.removeViewAt(logsList.childCount - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        isPinging = false
        stopPingingLoop()
        executor.shutdownNow()
    }
}
