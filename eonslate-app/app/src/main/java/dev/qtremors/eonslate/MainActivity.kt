package dev.qtremors.eonslate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.eonslate.core.reminders.EXTRA_LOG_TRACKER
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.DisplayPreferences
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.settings.WelcomeGate
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessages
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.ui.theme.ValueDisplay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDateTime

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var trackers: TrackerRepository

    @Inject
    lateinit var finance: FinanceRepository

    /** Null until the start is decided; the splash stays up until then, so nothing flashes. */
    private var showWelcome by mutableStateOf<Boolean?>(null)

    /** A tracker whose log screen a reminder asked to open. */
    private var openLog by mutableStateOf<String?>(null)
    private var sharedMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { showWelcome == null }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyScreenshotPolicy(settings)
        if (savedInstanceState == null) {
            openLog = intent.getStringExtra(EXTRA_LOG_TRACKER)
            sharedMessage = takePaymentMessage(intent)
        }
        lifecycleScope.launch {
            val seen = settings.welcomeSeen.first()
            val hasTrackers = !seen && trackers.nextPosition() > 0
            // Someone with trackers already has what the welcome would offer; never show it to them.
            if (hasTrackers) settings.setWelcomeSeen(true)
            showWelcome = WelcomeGate.shouldShow(seen, hasTrackers)
        }
        setContent {
            val appearance by settings.appearance.collectAsState(initial = Appearance())
            val display by settings.display.collectAsState(initial = DisplayPreferences())
            EonslateTheme(options = appearance.toThemeOptions()) {
                val lightBars = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                CompositionLocalProvider(LocalValueDisplay provides ValueDisplay(display.numbers.locale(), display.dates.pattern)) {
                    showWelcome?.let {
                        EonslateNavHost(
                            startWithWelcome = it,
                            openLog = openLog,
                            onLogOpened = { openLog = null },
                            sharedMessage = sharedMessage,
                            onMessageOpened = { sharedMessage = null },
                            displayName = display.name,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val now = LocalDateTime.now()
        lifecycleScope.launch { finance.postDue(now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_LOG_TRACKER)?.let { openLog = it }
        takePaymentMessage(intent)?.let { sharedMessage = it }
    }

    private fun takePaymentMessage(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        intent.removeExtra(Intent.EXTRA_TEXT)
        return text?.takeIf { it.length <= PaymentMessages.MAX_LENGTH && it.isNotBlank() }?.toString()
    }
}
