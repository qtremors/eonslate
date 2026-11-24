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
import kotlinx.coroutines.delay
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

    @Inject
    lateinit var detection: dev.qtremors.eonslate.feature.finance.SmsDetection

    @Inject
    lateinit var alerts: dev.qtremors.eonslate.feature.finance.AlertCenter

    /** Null until the start is decided; the splash stays up until then, so nothing flashes. */
    private var showWelcome by mutableStateOf<Boolean?>(null)

    /** A tracker whose log screen a reminder asked to open. */
    private var openLog by mutableStateOf<String?>(null)
    private var sharedMessage by mutableStateOf<String?>(null)

    /** True once the first screen has its data, or after [SPLASH_LIMIT_MILLIS] at most. */
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash holds until Home has its figures, so the app opens filled in rather than loading.
        installSplashScreen().setKeepOnScreenCondition { showWelcome == null || !ready }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyScreenshotPolicy(settings)
        if (savedInstanceState == null) {
            openLog = intent.getStringExtra(EXTRA_LOG_TRACKER)
            sharedMessage = takePaymentMessage(intent)
        }
        lifecycleScope.launch {
            delay(SPLASH_LIMIT_MILLIS)
            ready = true
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
            val alertCount by alerts.count.collectAsState(initial = 0)
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
                            onReady = { ready = true },
                            alertCount = alertCount,
                        )
                    }
                }
            }
        }
    }

    // Every return to the app catches up on payments that fell due meanwhile.
    override fun onResume() {
        super.onResume()
        val now = LocalDateTime.now()
        lifecycleScope.launch { finance.postDue(now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute) }
        // New payment SMS, or access just granted, show up without a restart.
        detection.refreshLater()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_LOG_TRACKER)?.let { openLog = it }
        takePaymentMessage(intent)?.let { sharedMessage = it }
    }

    private companion object {
        const val SPLASH_LIMIT_MILLIS = 1_500L
    }

    private fun takePaymentMessage(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        intent.removeExtra(Intent.EXTRA_TEXT)
        return text?.takeIf { it.length <= PaymentMessages.MAX_LENGTH && it.isNotBlank() }?.toString()
    }
}
