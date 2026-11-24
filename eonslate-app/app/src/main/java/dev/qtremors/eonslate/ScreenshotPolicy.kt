package dev.qtremors.eonslate

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.qtremors.eonslate.core.settings.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun ComponentActivity.applyScreenshotPolicy(settings: SettingsRepository) {
    val flag = WindowManager.LayoutParams.FLAG_SECURE
    // Keep the window protected while the stored setting loads.
    window.addFlags(flag)
    lifecycleScope.launch {
        settings.appearance.map { it.blockScreenshots }.distinctUntilChanged().collect { blocked ->
            if (blocked) window.addFlags(flag) else window.clearFlags(flag)
        }
    }
}
