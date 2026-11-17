package dev.qtremors.eonslate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.qtremors.eonslate.core.navigation.api.Destination
import dev.qtremors.eonslate.feature.backup.BackupScreen
import dev.qtremors.eonslate.feature.reminders.RemindersScreen
import dev.qtremors.eonslate.feature.entry.LogEntryScreen
import dev.qtremors.eonslate.feature.finance.FinanceScreen
import dev.qtremors.eonslate.feature.home.ArchivedScreen
import dev.qtremors.eonslate.feature.home.HomeScreen
import dev.qtremors.eonslate.feature.settings.AboutScreen
import dev.qtremors.eonslate.feature.settings.LicensesScreen
import dev.qtremors.eonslate.feature.settings.PrivacyScreen
import dev.qtremors.eonslate.feature.settings.SettingsScreen
import dev.qtremors.eonslate.feature.trackerdetail.TrackerDetailScreen
import dev.qtremors.eonslate.feature.trackereditor.TemplateGalleryScreen
import dev.qtremors.eonslate.feature.trackereditor.TrackerEditorScreen
import dev.qtremors.eonslate.feature.welcome.WelcomeScreen

/**
 * The app's screens. [startWithWelcome] is read once: finishing the welcome moves on to Home,
 * and the welcome never shows again.
 */
@Composable
fun EonslateNavHost(startWithWelcome: Boolean, openLog: String? = null, onLogOpened: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(openLog) {
        openLog ?: return@LaunchedEffect
        navController.navigate(Destination.LogEntry(openLog))
        onLogOpened()
    }
    val start: Destination = remember { if (startWithWelcome) Destination.Welcome else Destination.Home }
    NavHost(navController = navController, startDestination = start) {
        composable<Destination.Welcome> {
            WelcomeScreen(
                onDone = {
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Welcome> { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.Home> {
            HomeScreen(
                onNewTracker = { navController.navigate(Destination.Templates) },
                onOpenTracker = { id -> navController.navigate(Destination.TrackerDetail(id)) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
                onArchived = { navController.navigate(Destination.Archived) },
                onEditTracker = { id -> navController.navigate(Destination.TrackerEditor(id)) },
                onSettings = { navController.navigate(Destination.Settings) },
                onFinance = { navController.navigate(Destination.Finance) },
            )
        }
        composable<Destination.Finance> { FinanceScreen(onBack = { navController.popBackStack() }) }
        composable<Destination.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(Destination.About) },
                onBackup = { navController.navigate(Destination.Backup) },
            )
        }
        composable<Destination.Backup> {
            BackupScreen(versionName = BuildConfig.VERSION_NAME, onBack = { navController.popBackStack() })
        }
        composable<Destination.About> {
            AboutScreen(
                versionName = BuildConfig.VERSION_NAME,
                onBack = { navController.popBackStack() },
                onLicenses = { navController.navigate(Destination.Licenses) },
                onPrivacy = { navController.navigate(Destination.Privacy) },
            )
        }
        composable<Destination.Licenses> { LicensesScreen(onBack = { navController.popBackStack() }) }
        composable<Destination.Privacy> { PrivacyScreen(onBack = { navController.popBackStack() }) }
        composable<Destination.Archived> {
            ArchivedScreen(
                onBack = { navController.popBackStack() },
                onOpenTracker = { id -> navController.navigate(Destination.TrackerDetail(id)) },
            )
        }
        composable<Destination.TrackerDetail> {
            TrackerDetailScreen(
                onBack = { navController.popBackStack() },
                onEditTracker = { id -> navController.navigate(Destination.TrackerEditor(id)) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
                onEditEntry = { trackerId, entryId -> navController.navigate(Destination.LogEntry(trackerId, entryId)) },
                onReminders = { id -> navController.navigate(Destination.Reminders(id)) },
            )
        }
        composable<Destination.Reminders> { RemindersScreen(onBack = { navController.popBackStack() }) }
        composable<Destination.LogEntry> {
            LogEntryScreen(onDone = { navController.popBackStack() })
        }
        composable<Destination.Templates> {
            TemplateGalleryScreen(
                onPick = { templateId ->
                    // The gallery steps aside, so leaving the builder returns to where it began.
                    navController.navigate(Destination.TrackerEditor(templateId = templateId)) {
                        popUpTo<Destination.Templates> { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Destination.TrackerEditor> {
            TrackerEditorScreen(
                onDone = { navController.popBackStack() },
                // A deleted tracker has no page to return to, so go all the way back to Home.
                onDeleted = { navController.popBackStack(Destination.Home, inclusive = false) },
            )
        }
    }
}
