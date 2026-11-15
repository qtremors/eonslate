package dev.qtremors.eonslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.settings.WelcomeGate
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var trackers: TrackerRepository

    /** Null until the start is decided; the splash stays up until then, so nothing flashes. */
    private var showWelcome by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { showWelcome == null }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val seen = settings.welcomeSeen.first()
            val hasTrackers = !seen && trackers.nextPosition() > 0
            // Someone with trackers already has what the welcome would offer; never show it to them.
            if (hasTrackers) settings.setWelcomeSeen(true)
            showWelcome = WelcomeGate.shouldShow(seen, hasTrackers)
        }
        setContent {
            val appearance by settings.appearance.collectAsState(initial = Appearance())
            EonslateTheme(options = appearance.toThemeOptions()) {
                showWelcome?.let { EonslateNavHost(startWithWelcome = it) }
            }
        }
    }
}
