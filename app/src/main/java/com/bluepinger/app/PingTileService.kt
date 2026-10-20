package com.bluepinger.app

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class PingTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(PingState.status.value.isRunning)
    }

    override fun onClick() {
        super.onClick()
        val isRunning = PingState.status.value.isRunning
        val intent = Intent(this, PingService::class.java).apply {
            action = if (isRunning) PingService.ACTION_STOP else PingService.ACTION_START
        }

        if (isRunning) {
            startService(intent)
            updateTileState(false)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            updateTileState(true)
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Pinging..." else "BluePinger"
        tile.updateTile()
    }
}