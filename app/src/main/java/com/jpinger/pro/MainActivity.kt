package com.jpinger.pro

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.Date
import android.text.format.DateFormat

class MainActivity : AppCompatActivity() {

    private lateinit var btnPing: Button
    private lateinit var txtMin: TextView
    private lateinit var txtMax: TextView
    private lateinit var txtOkCount: TextView
    private lateinit var spinnerServer: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var listLogs: ListView
    
    private val logAdapter by lazy { ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, logList) }
    private val logList = ArrayList<String>()

    private var isPinging = false
    private val handler = Handler(Looper.getMainLooper())
    private var pingRunnable: Runnable? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var minPing = 0L
    private var maxPing = 0L
    private var successCount = 0

    private val servers = mapOf(
        "Cloudflare (1.1.1.1)" to "1.1.1.1",
        "Google DNS (8.8.8.8)" to "8.8.8.8",
        "OpenDNS (208.67.222.222)" to "208.67.222.222",
        "Quad9 (9.9.9.9)" to "9.9.9.9"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050B18"))
            setPadding(32, 32, 32, 32)
        }

        val titleText = TextView(this).apply {
            text = "⚡ J PINGER PRO"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 10, 0, 30)
        }
        rootLayout.addView(titleText)

        btnPing = Button(this).apply {
            text = "START"
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#152642"))
            layoutParams = LinearLayout.LayoutParams(400, 400).apply {
                gravity = android.view.Gravity.CENTER
                bottomMargin = 40
            }
        }
        rootLayout.addView(btnPing)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 30
            }
        }
        txtMin = createStatCard(statsRow, "අවම (Min)", "0 ms")
        txtMax = createStatCard(statsRow, "උපරිම (Max)", "0 ms")
        txtOkCount = createStatCard(statsRow, "සාර්ථක", "0")
        rootLayout.addView(statsRow)

        spinnerServer = Spinner(this).apply {
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, servers.keys.toList())
            this.adapter = adapter
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
        }
        rootLayout.addView(spinnerServer)

        spinnerInterval = Spinner(this).apply {
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("500 ms (වේගවත්)", "1000 ms (තත්පර 1)", "2000 ms (තත්පර 2)"))
            this.adapter = adapter
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 30 }
        }
        rootLayout.addView(spinnerInterval)

        listLogs = ListView(this).apply {
            adapter = logAdapter
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        rootLayout.addView(listLogs)

        setContentView(rootLayout)

        btnPing.setOnClickListener {
            togglePing()
        }
    }

    private fun createStatCard(parent: LinearLayout, title: String, initialVal: String): TextView {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1930"))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 8, 0)
            }
        }
        val lbl = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
        }
        val valueView = TextView(this).apply {
            text = initialVal
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        card.addView(lbl)
        card.addView(valueView)
        parent.addView(card)
        return valueView
    }

    private fun togglePing() {
        isPinging = !isPinging
        if (isPinging) {
            btnPing.text = "STOP"
            btnPing.setBackgroundColor(Color.parseColor("#0044AA"))
            spinnerServer.isEnabled = false
            spinnerInterval.isEnabled = false
            addLog("J Pinger ආරම්භ විය...")
            startPingingLoop()
        } else {
            btnPing.text = "START"
            btnPing.setBackgroundColor(Color.parseColor("#152642"))
            spinnerServer.isEnabled = true
            spinnerInterval.isEnabled = true
            stopPingingLoop()
            addLog("J Pinger නතර විය.")
        }
    }

    private fun startPingingLoop() {
        val selectedKey = spinnerServer.selectedItem.toString()
        val host = servers[selectedKey] ?: "1.1.1.1"
        val interval = when (spinnerInterval.selectedItemPosition) {
            0 -> 500L
            1 -> 1000L
            else -> 2000L
        }

        pingRunnable = object : Runnable {
            override fun run() {
                if (!isPinging) return
                executor.execute {
                    val start = System.currentTimeMillis()
                    var success = false
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(host, 53), 2000)
                        socket.close()
                        success = true
                    } catch (e: IOException) {
                        success = false
                    }
                    val elapsed = System.currentTimeMillis() - start

                    handler.post {
                        if (!isPinging) return@post
                        if (success) {
                            successCount++
                            if (minPing == 0L || elapsed < minPing) minPing = elapsed
                            if (elapsed > maxPing) maxPing = elapsed
                            btnPing.text = "${elapsed}ms"
                            txtMin.text = "${minPing} ms"
                            txtMax.text = "${maxPing} ms"
                            txtOkCount.text = "$successCount"
                            addLog("Reply: ${elapsed}ms [OK]")
                        } else {
                            btnPing.text = "TIMEOUT"
                            addLog("Request Timed Out [ERROR]")
                        }
                    }
                }
                handler.postDelayed(this, interval)
            }
        }
        handler.post(pingRunnable!!)
    }

    private fun stopPingingLoop() {
        pingRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun addLog(msg: String) {
        val timeStr = DateFormat.format("HH:mm:ss", Date()).toString()
        logList.add(0, "[$timeStr] $msg")
        if (logList.size > 50) logList.removeAt(logList.size - 1)
        logAdapter.notifyDataSetChanged()
    }
}
