package com.bluepinger.app

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class PingTileService : TileService() {

    override fun onStartListening() {
        render(PingState.running.value)
    }

    override fun onClick() {
        val willRun = !PingState.running.value
        if (willRun) PingService.start(this) else PingService.stop(this)
        render(willRun)
    }

    private fun render(running: Boolean) {
        val tile = qsTile ?: return
        tile.label = "Blue Pinger"
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) {
            val last = PingState.stats.value.last
            tile.subtitle = when {
                !running -> "Tap to start"
                last != null -> "%.0f ms".format(last)
                else -> "Pinging…"
            }
        }
        tile.updateTile()
    }

    companion object {
        fun refresh(ctx: Context) {
            try {
                requestListeningState(ctx, ComponentName(ctx, PingTileService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
