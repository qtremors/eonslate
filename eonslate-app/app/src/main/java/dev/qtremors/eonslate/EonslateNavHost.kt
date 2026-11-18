package dev.qtremors.eonslate

import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.qtremors.eonslate.core.ui.components.FloatingTabBar
import dev.qtremors.eonslate.core.ui.components.LocalShellHost
import dev.qtremors.eonslate.core.ui.components.ShellHost
import dev.qtremors.eonslate.core.ui.components.TabItem
import dev.qtremors.eonslate.core.ui.theme.LocalReduceMotion
import dev.qtremors.eonslate.feature.finance.FinanceSection
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
fun EonslateNavHost(
    startWithWelcome: Boolean,
    openLog: String? = null,
    onLogOpened: () -> Unit = {},
    sharedMessage: String? = null,
    onMessageOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    LaunchedEffect(openLog) {
        openLog ?: return@LaunchedEffect
        navController.navigate(Destination.LogEntry(openLog))
        onLogOpened()
    }
    val start: Destination = remember { if (startWithWelcome) Destination.Welcome else Destination.Home }
    LaunchedEffect(sharedMessage) {
        if (sharedMessage != null) navController.navigateToTab(Destination.Transactions)
    }
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val shell = remember { ShellHost() }
    val reduceMotion = LocalReduceMotion.current
    val tabs = listOf(
        TabItem(Icons.Rounded.SpaceDashboard, stringResource(R.string.tab_home)),
        TabItem(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.tab_transactions)),
        TabItem(Icons.Rounded.Autorenew, stringResource(R.string.tab_subscriptions)),
        TabItem(Icons.Rounded.TrackChanges, stringResource(R.string.tab_trackers)),
    )
    CompositionLocalProvider(LocalShellHost provides shell) {
    Box(Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = start,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { shellEnter(reduceMotion) },
        exitTransition = { shellExit(reduceMotion) },
        popEnterTransition = { shellPopEnter(reduceMotion) },
        popExitTransition = { shellPopExit(reduceMotion) },
        predictivePopEnterTransition = { shellPopEnter(reduceMotion) },
        predictivePopExitTransition = { shellPopExit(reduceMotion) },
    ) {
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
            HomeTab(
                onTransactions = { navController.navigateToTab(Destination.Transactions) },
                onSubscriptions = { navController.navigateToTab(Destination.Subscriptions) },
                onTrackers = { navController.navigateToTab(Destination.Trackers) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
                onSettings = { navController.navigate(Destination.Settings) },
            )
        }
        composable<Destination.Trackers> {
            HomeScreen(
                onNewTracker = { navController.navigate(Destination.Templates) },
                onOpenTracker = { id -> navController.navigate(Destination.TrackerDetail(id)) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
                onArchived = { navController.navigate(Destination.Archived) },
                onEditTracker = { id -> navController.navigate(Destination.TrackerEditor(id)) },
                onSettings = { navController.navigate(Destination.Settings) },
            )
        }
        composable<Destination.Transactions> {
            FinanceScreen(FinanceSection.TRANSACTIONS, sharedMessage = sharedMessage, onMessageOpened = onMessageOpened)
        }
        composable<Destination.Subscriptions> { FinanceScreen(FinanceSection.SUBSCRIPTIONS) }
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
                onDeleted = { navController.popBackStack(Destination.Trackers, inclusive = false) },
            )
        }
    }
        if (destination != null && !destination.hasRoute<Destination.Welcome>()) {
            val top = destination.isTab()
            FloatingTabBar(
                tabs = tabs,
                selectedIndex = destination.tabIndex(),
                onSelect = { navController.navigateToTab(TABS[it]) },
                backTitle = if (top) null else shell.title,
                onBack = if (top) null else ({ navController.popBackStack() }),
                action = shell.action,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    }
}

private val TABS = listOf(Destination.Home, Destination.Transactions, Destination.Subscriptions, Destination.Trackers)

private fun NavDestination.isTab(): Boolean =
    hasRoute<Destination.Home>() || hasRoute<Destination.Transactions>() || hasRoute<Destination.Subscriptions>() || hasRoute<Destination.Trackers>()

/** Detail pages light up the tab they belong to. */
private fun NavDestination.tabIndex(): Int = when {
    hasRoute<Destination.Transactions>() -> 1
    hasRoute<Destination.Subscriptions>() -> 2
    hasRoute<Destination.Trackers>() || hasRoute<Destination.TrackerDetail>() || hasRoute<Destination.TrackerEditor>() ||
        hasRoute<Destination.Templates>() || hasRoute<Destination.Archived>() || hasRoute<Destination.LogEntry>() ||
        hasRoute<Destination.Reminders>() -> 3
    else -> 0
}

/** Tabs keep their own back stacks and scroll positions. */
private fun NavHostController.navigateToTab(route: Destination) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
