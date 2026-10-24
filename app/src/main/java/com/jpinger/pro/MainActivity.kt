package com.jpinger.pro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity(), PingEngine.Listener {

    private val cBg = Color.parseColor("#050B18")
    private val cBar = Color.parseColor("#0A1529")
    private val cCard = Color.parseColor("#0D1930")
    private val cCyan = Color.parseColor("#00E5FF")
    private val cBlue = Color.parseColor("#0088FF")
    private val cOk = Color.parseColor("#40C4FF")
    private val cWarn = Color.parseColor("#FFC107")
    private val cErr = Color.parseColor("#FF5252")

    private val servers = listOf(
        Target("Cloudflare (1.1.1.1)", "1.1.1.1", 443),
        Target("Google DNS (8.8.8.8)", "8.8.8.8", 443),
        Target("Google (google.com)", "www.google.com", 443),
        Target("YouTube (youtube.com)", "www.youtube.com", 443),
        Target("Facebook (facebook.com)", "www.facebook.com", 443),
        Target("Custom… (ඔබේ host එක)", "", 443)
    )
    private val intervalLabels = listOf(
        "500 ms (වේගවත්)", "තත්පර 1 (1000 ms)", "තත්පර 2 (2000 ms)", "තත්පර 5 (5000 ms)"
    )
    private val intervalValues = longArrayOf(500, 1000, 2000, 5000)

    private lateinit var pingBtn: LinearLayout
    private lateinit var pingText: TextView
    private lateinit var statusText: TextView
    private lateinit var tLast: TextView
    private lateinit var tMin: TextView
    private lateinit var tAvg: TextView
    private lateinit var tMax: TextView
    private lateinit var tLoss: TextView
    private lateinit var tJitter: TextView
    private lateinit var tQuality: TextView
    private lateinit var netText: TextView
    private lateinit var tipText: TextView
    private lateinit var spinnerServer: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var customHost: EditText
    private lateinit var logsList: LinearLayout

    private val prefs by lazy { getSharedPreferences("jpinger", Context.MODE_PRIVATE) }

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cBar

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
        content.addView(buildStats())
        content.addView(buildSettingsCard())
        content.addView(buildLogsBox())

        root.addView(ScrollView(this).apply { addView(content) }, lp(-1, 0, 1f))
        setContentView(root)

        restorePrefs()
        updateCustomVisibility()
        askNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        logsList.removeAllViews()
        for (e in PingEngine.logHistory()) addLogView(e)       // oldest first
        PingEngine.addListener(this)
        onSnapshot(PingEngine.snapshot())
    }

    override fun onStop() {
        PingEngine.removeListener(this)
        savePrefs()
        super.onStop()
    }

    // ---------------------------------------------------------------- engine callbacks

    override fun onSnapshot(s: Snapshot) {
        // button + status
        pingBtn.background = if (s.running) circle(cCyan, Color.parseColor("#0044AA"), cCyan)
        else circle(Color.parseColor("#152642"), Color.parseColor("#091224"), Color.parseColor("#4A6583"))
        pingBtn.elevation = dp(if (s.running) 12 else 6).toFloat()
        statusText.text = if (s.running) "ක්‍රියාත්මකයි" else "නතර වී ඇත"
        spinnerServer.isEnabled = !s.running
        spinnerInterval.isEnabled = !s.running
        customHost.isEnabled = !s.running

        val lastText = when {
            s.sent == 0 -> if (s.running) "…" else "—"
            s.last >= 0 -> "${s.last} ms"
            else -> "TIMEOUT"
        }
        pingText.text = if (s.running) lastText else "START"
        pingText.textSize = if (s.running && s.last < 0 && s.sent > 0) 16f else 22f

        tLast.text = lastText
        tLast.setTextColor(if (s.sent > 0 && s.last < 0) cErr else cCyan)
        tMin.text = if (s.ok == 0) "—" else "${s.min} ms"
        tAvg.text = if (s.ok == 0) "—" else "${s.avg} ms"
        tMax.text = if (s.ok == 0) "—" else "${s.max} ms"

        tLoss.text = if (s.sent == 0) "—" else "${s.recentLossPct}%"
        tLoss.setTextColor(
            when {
                s.recentLossPct >= 10 -> cErr
                s.recentLossPct >= 3 -> cWarn
                else -> cCyan
            }
        )
        tJitter.text = if (s.ok < 2) "—" else "${s.jitter} ms"

        val (qText, qColor) = when (s.quality) {
            Quality.EXCELLENT -> "Excellent" to cOk
            Quality.GOOD -> "Good" to cOk
            Quality.FAIR -> "Fair" to cWarn
            Quality.POOR -> "Poor" to cErr
            Quality.UNKNOWN -> "—" to cCyan
        }
        tQuality.text = qText
        tQuality.setTextColor(qColor)

        val net = if (s.running) s.network else NetUtil.current(this)
        netText.text = "ජාලය: $net"

        tipText.text = when {
            !s.running -> "START ඔබා පසුබිමේ (background) පවා ping කරන්න. වීඩියෝ බලද්දී app එක වසා දැමුවත් වැඩ කරයි."
            s.quality == Quality.POOR ->
                "⚠ සම්බන්ධතාව දුර්වලයි — router එකට ළං වෙන්න, Wi-Fi ↔ Mobile Data මාරු කර බලන්න, background downloads නවත්වන්න."
            s.quality == Quality.FAIR -> "සම්බන්ධතාව මධ්‍යස්ථයි — buffering ටිකක් වෙන්න පුළුවන්."
            s.quality == Quality.UNKNOWN -> "දත්ත රැස් කරමින්…"
            else -> "✔ සම්බන්ධතාව ස්ථාවරයි."
        }
        tipText.setTextColor(
            when (s.quality) {
                Quality.POOR -> cErr
                Quality.FAIR -> cWarn
                else -> Color.parseColor("#99FFFFFF")
            }
        )
    }

    override fun onLog(e: LogEntry) {
        addLogView(e)
    }

    // ---------------------------------------------------------------- actions

    private fun toggle() {
        if (PingEngine.snapshot().running) {
            PingService.stop(this)
            return
        }
        val target = selectedTarget() ?: return
        savePrefs()
        PingService.start(this, target, intervalValues[spinnerInterval.selectedItemPosition])
    }

    private fun selectedTarget(): Target? {
        val t = servers[spinnerServer.selectedItemPosition]
        if (t.host.isNotEmpty()) return t

        val raw = customHost.text.toString().trim()
            .removePrefix("https://").removePrefix("http://").substringBefore('/')
        val host = raw.substringBefore(':').trim()
        if (host.isEmpty()) {
            Toast.makeText(this, "Host එකක් ඇතුළත් කරන්න (උදා: example.com)", Toast.LENGTH_SHORT).show()
            return null
        }
        val port = if (raw.contains(':')) raw.substringAfter(':').toIntOrNull() else 443
        if (port == null || port !in 1..65535) {
            Toast.makeText(this, "Port අංකය වැරදියි", Toast.LENGTH_SHORT).show()
            return null
        }
        return Target(host, host, port)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun savePrefs() {
        prefs.edit()
            .putInt("server", spinnerServer.selectedItemPosition)
            .putInt("interval", spinnerInterval.selectedItemPosition)
            .putString("custom", customHost.text.toString())
            .apply()
    }

    private fun restorePrefs() {
        spinnerServer.setSelection(prefs.getInt("server", 0).coerceIn(0, servers.size - 1))
        spinnerInterval.setSelection(prefs.getInt("interval", 1).coerceIn(0, intervalValues.size - 1))
        customHost.setText(prefs.getString("custom", ""))
    }

    private fun updateCustomVisibility() {
        val custom = servers[spinnerServer.selectedItemPosition].host.isEmpty()
        customHost.visibility = if (custom) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------- UI building

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun box(fill: Int, stroke: Int, radiusDp: Int, strokeDp: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), stroke)
        }

    private fun circle(c1: Int, c2: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(80).toFloat()
            setGradientCenter(0.5f, 0.5f)
            colors = intArrayOf(c1, c2)
            setStroke(dp(3), stroke)
        }

    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

    private fun buildAppBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(cBar)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            elevation = dp(4).toFloat()
        }
        bar.addView(TextView(this).apply {
            text = "⚡ J PINGER PRO"
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.1f
        }, lp(0, -2, 1f))

        val reset = TextView(this).apply {
            text = "Reset"
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), box(cBlue, cBlue, 6, 0))
                addState(intArrayOf(), box(0, cBlue, 6))
            }
            isClickable = true
            setOnClickListener {
                PingEngine.resetStats()
                logsList.removeAllViews()
            }
        }
        bar.addView(reset, lp(-2, -2))
        return bar
    }

    private fun buildPingButton(): View {
        pingBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = circle(Color.parseColor("#152642"), Color.parseColor("#091224"), Color.parseColor("#4A6583"))
            isClickable = true
            elevation = dp(6).toFloat()
            setOnClickListener { toggle() }
        }
        pingBtn.addView(TextView(this).apply {
            text = "⚡"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            gravity = Gravity.CENTER
        })
        pingText = TextView(this).apply {
            text = "START"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        pingBtn.addView(pingText)
        statusText = TextView(this).apply {
            text = "නතර වී ඇත"
            setTextColor(Color.parseColor("#BFFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, 0)
        }
        pingBtn.addView(statusText)
        pingBtn.layoutParams = lp(dp(145), dp(145)).apply { setMargins(0, dp(10), 0, dp(20)) }
        return pingBtn
    }

    private fun statCard(parent: LinearLayout, label: String, last: Boolean): TextView {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = box(cCard, Color.parseColor("#660088FF"), 10)
            setPadding(dp(6), dp(10), dp(6), dp(10))
        }
        card.addView(TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#99FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER
        })
        val value = TextView(this).apply {
            text = "—"
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        card.addView(value)
        parent.addView(card, lp(0, -2, 1f).apply { if (!last) rightMargin = dp(8) })
        return value
    }

    private fun buildStats(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tLast = statCard(row1, "දැන් (Last)", false)
        tMin = statCard(row1, "අවම (Min)", false)
        tAvg = statCard(row1, "සාමාන්‍ය (Avg)", false)
        tMax = statCard(row1, "උපරිම (Max)", true)
        col.addView(row1, lp(-1, -2).apply { bottomMargin = dp(8) })

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tLoss = statCard(row2, "Loss (අන්තිම 20)", false)
        tJitter = statCard(row2, "Jitter", false)
        tQuality = statCard(row2, "ගුණාත්මකභාවය", true)
        col.addView(row2, lp(-1, -2).apply { bottomMargin = dp(8) })

        netText = TextView(this).apply {
            setTextColor(Color.parseColor("#CCFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
        }
        col.addView(netText, lp(-1, -2).apply { bottomMargin = dp(4) })

        tipText = TextView(this).apply {
            setTextColor(Color.parseColor("#99FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
        }
        col.addView(tipText, lp(-1, -2).apply { bottomMargin = dp(16) })

        col.layoutParams = lp(-1, -2)
        return col
    }

    private fun styledSpinner(items: List<String>): Spinner {
        val self = this
        val s = Spinner(this)
        val adapter = object : ArrayAdapter<String>(self, android.R.layout.simple_spinner_item, items) {
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

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getView(position, convertView, parent), false)

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getDropDownView(position, convertView, parent), true)
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        s.adapter = adapter
        s.background = box(cBg, cBlue, 6)
        s.setPadding(dp(6), dp(2), dp(6), dp(2))
        if (Build.VERSION.SDK_INT >= 23) s.setPopupBackgroundDrawable(box(cBg, cBlue, 6))
        return s
    }

    private fun settingRow(label: String, view: View): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#CCFFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }, lp(0, -2, 1f))
        row.addView(view, lp(-2, -2))
        return row
    }

    private fun buildSettingsCard(): View {
        spinnerServer = styledSpinner(servers.map { it.label })
        spinnerInterval = styledSpinner(intervalLabels)
        spinnerServer.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateCustomVisibility()
            }

            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        customHost = EditText(this).apply {
            hint = "example.com  හෝ  example.com:443"
            setHintTextColor(Color.parseColor("#66FFFFFF"))
            setTextColor(cCyan)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            background = box(cBg, cBlue, 6)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            visibility = View.GONE
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(cCard, Color.parseColor("#4D0088FF"), 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        card.addView(settingRow("Server තෝරන්න:", spinnerServer))
        card.addView(customHost, lp(-1, -2).apply { topMargin = dp(2); bottomMargin = dp(4) })
        card.addView(View(this).apply { setBackgroundColor(Color.parseColor("#14FFFFFF")) },
            lp(-1, dp(1)).apply { topMargin = dp(4); bottomMargin = dp(4) })
        card.addView(settingRow("වේගය (Interval):", spinnerInterval))
        card.layoutParams = lp(-1, -2).apply { bottomMargin = dp(16) }
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
        logsBox.addView(logsList, lp(-1, -2))
        logsBox.layoutParams = lp(-1, -2)
        return logsBox
    }

    private fun addLogView(e: LogEntry) {
        val tv = TextView(this).apply {
            text = "[${e.time}] ${e.msg}"
            setTextColor(if (e.ok) cOk else cErr)
            typeface = Typeface.MONOSPACE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dp(2), 0, dp(2))
        }
        logsList.addView(tv, 0)
        if (logsList.childCount > 50) logsList.removeViewAt(logsList.childCount - 1)
    }
}
