package dev.qtremors.eonslate

import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.TrackChanges
import dev.qtremors.eonslate.feature.finance.NotificationsScreen
import dev.qtremors.eonslate.core.ui.components.ProfileAvatar
import dev.qtremors.eonslate.core.ui.components.NotificationBell
import dev.qtremors.eonslate.core.ui.components.LocalAccountActions
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.GridView
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
import dev.qtremors.eonslate.feature.finance.ManageCategoriesScreen
import dev.qtremors.eonslate.feature.home.ArchivedScreen
import dev.qtremors.eonslate.feature.home.HomeScreen
import dev.qtremors.eonslate.feature.settings.AboutScreen
import dev.qtremors.eonslate.feature.settings.LicensesScreen
import dev.qtremors.eonslate.feature.settings.PrivacyScreen
import dev.qtremors.eonslate.feature.settings.SettingsScreen
import dev.qtremors.eonslate.feature.settings.DisplaySettingsScreen
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
    displayName: String = "",
    /** Called once the first screen has what it shows, so the splash can go. */
    onReady: () -> Unit = {},
    /** What the bell's badge shows. */
    alertCount: Int = 0,
) {
    val navController = rememberNavController()
    val latestMessage by rememberUpdatedState(sharedMessage)
    val messageOpened by rememberUpdatedState(onMessageOpened)
    val homeName by rememberUpdatedState(displayName)
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
    var openInbox by remember { mutableStateOf(false) }
    var openNew by remember { mutableStateOf<TransactionKind?>(null) }
    var openBudget by remember { mutableStateOf(false) }
    var openCategory by remember { mutableStateOf<String?>(null) }
    var openProfile by remember { mutableStateOf(false) }
    var openAccount by remember { mutableStateOf<String?>(null) }
    var openNewAccount by remember { mutableStateOf(false) }
    val accountActions: @Composable RowScope.() -> Unit = {
        NotificationBell(alertCount) { navController.navigate(Destination.Notifications) { launchSingleTop = true } }
        ProfileAvatar(homeName, onClick = { openProfile = true })
    }
    val reduceMotion = LocalReduceMotion.current
    val tabs = listOf(
        TabItem(Icons.Rounded.SpaceDashboard, stringResource(R.string.tab_home)),
        TabItem(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.tab_transactions)),
        TabItem(Icons.Rounded.Autorenew, stringResource(R.string.tab_subscriptions)),
        TabItem(Icons.Rounded.TrackChanges, stringResource(R.string.tab_trackers)),
    )
    CompositionLocalProvider(LocalShellHost provides shell, LocalAccountActions provides accountActions) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    // From 840 dp the tabs move to a rail at the side, as on tablets and in landscape.
    val wide = maxWidth >= WIDE_LAYOUT
    val showShell = destination != null && !destination.hasRoute<Destination.Welcome>()
    Row(Modifier.fillMaxSize()) {
    if (wide && showShell) {
        val action = shell.action
        NavigationRail(
            header = action?.let {
                {
                    FloatingActionButton(onClick = it.onClick) { Icon(it.icon, contentDescription = it.label) }
                }
            },
        ) {
            Spacer(Modifier.weight(1f))
            tabs.forEachIndexed { index, tab ->
                NavigationRailItem(
                    selected = destination?.tabIndex() == index,
                    onClick = { navController.navigateToTab(TABS[index]) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
    Box(Modifier.weight(1f).fillMaxSize()) {
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
            LaunchedEffect(Unit) { onReady() }
            WelcomeScreen(
                onDone = {
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Welcome> { inclusive = true }
                    }
                },
                // Restoring from the first run: Home, with Backup open on top of it.
                onRestore = {
                    navController.navigate(Destination.Home) {
                        popUpTo<Destination.Welcome> { inclusive = true }
                    }
                    navController.navigate(Destination.Backup)
                },
            )
        }
        composable<Destination.Home> {
            HomeTab(
                onLoaded = onReady,
                onAccount = { id ->
                    openAccount = id
                    navController.navigateToTab(Destination.Transactions)
                },
                onAddAccount = {
                    openNewAccount = true
                    navController.navigateToTab(Destination.Transactions)
                },
                onTransactions = { navController.navigateToTab(Destination.Transactions) },
                onSubscriptions = { navController.navigateToTab(Destination.Subscriptions) },
                onTrackers = { navController.navigateToTab(Destination.Trackers) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
                onSettings = { navController.navigate(Destination.Settings) },
                onAdd = { kind ->
                    openNew = kind
                    navController.navigateToTab(Destination.Transactions)
                },
                onAddBudget = {
                    openBudget = true
                    navController.navigateToTab(Destination.Transactions)
                },
                onCategory = { name ->
                    openCategory = name
                    navController.navigateToTab(Destination.Transactions)
                },
                displayName = homeName,
            )
        }
        composable<Destination.Trackers> {
            HomeScreen(
                onNewTracker = { navController.navigate(Destination.Templates) },
                onOpenTracker = { id -> navController.navigate(Destination.TrackerDetail(id)) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
                onLogPart = { id, option -> navController.navigate(Destination.LogEntry(id, optionId = option)) },
                onEditTracker = { id -> navController.navigate(Destination.TrackerEditor(id)) },
                onSettings = { navController.navigate(Destination.Settings) },
            )
        }
        composable<Destination.Transactions> {
            FinanceScreen(
                FinanceSection.TRANSACTIONS, sharedMessage = latestMessage, onMessageOpened = messageOpened,
                openInbox = openInbox, onInboxOpened = { openInbox = false },
                openNew = openNew, onNewOpened = { openNew = null },
                openBudget = openBudget, onBudgetOpened = { openBudget = false },
                openCategory = openCategory, onCategoryOpened = { openCategory = null },
                openAccount = openAccount, onAccountOpened = { openAccount = null },
                openNewAccount = openNewAccount, onNewAccountOpened = { openNewAccount = false },
                onManageCategories = { navController.navigate(Destination.Categories) },
                onSettings = { navController.navigate(Destination.Settings) },
            )
        }
        composable<Destination.Subscriptions> {
            FinanceScreen(FinanceSection.SUBSCRIPTIONS, onSettings = { navController.navigate(Destination.Settings) })
        }
        composable<Destination.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onReviewSms = {
                    openInbox = true
                    navController.navigateToTab(Destination.Transactions)
                },
                onSubscriptions = { navController.navigateToTab(Destination.Subscriptions) },
                onTransactions = { navController.navigateToTab(Destination.Transactions) },
                onTracker = { id -> navController.navigate(Destination.TrackerDetail(id)) },
            )
        }
        composable<Destination.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(Destination.About) },
                onBackup = { navController.navigate(Destination.Backup) },
                onCategories = { navController.navigate(Destination.Categories) },
                onDisplay = { navController.navigate(Destination.DisplaySettings) },
                onScanSms = {
                    openInbox = true
                    navController.navigateToTab(Destination.Transactions)
                },
            )
        }
        composable<Destination.DisplaySettings> {
            DisplaySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<Destination.Categories> {
            ManageCategoriesScreen(onBack = { navController.popBackStack() })
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
                onLogPart = { id, option -> navController.navigate(Destination.LogEntry(id, optionId = option)) },
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
        // On a wide screen the rail holds the tabs, so the toolbar shows only on detail pages, for Back.
        if (showShell && destination != null && !(wide && destination.isTab())) {
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
    if (openProfile) {
        ProfileDialog(
            name = homeName,
            links = ProfileLinks(
                settings = { navController.navigate(Destination.Settings) },
                personalization = { navController.navigate(Destination.DisplaySettings) },
                categories = { navController.navigate(Destination.Categories) },
                archived = { navController.navigate(Destination.Archived) },
                backup = { navController.navigate(Destination.Backup) },
                privacy = { navController.navigate(Destination.Privacy) },
                about = { navController.navigate(Destination.About) },
            ),
            onDismiss = { openProfile = false },
        )
    }
    }
}

private val WIDE_LAYOUT = 840.dp

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
