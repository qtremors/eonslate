package dev.qtremors.eonslate.quicklog

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickLogTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openPicker) else openPicker()
    }

    @Suppress("DEPRECATION")
    private fun openPicker() {
        val intent = QuickLog.picker(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(QuickLog.pending(this, 0, intent))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
