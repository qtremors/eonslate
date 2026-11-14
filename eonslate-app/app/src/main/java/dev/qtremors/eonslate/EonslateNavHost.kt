package dev.qtremors.eonslate

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.qtremors.eonslate.core.navigation.api.Destination
import dev.qtremors.eonslate.feature.entry.LogEntryScreen
import dev.qtremors.eonslate.feature.home.HomeScreen
import dev.qtremors.eonslate.feature.trackereditor.TrackerEditorScreen

@Composable
fun EonslateNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destination.Home) {
        composable<Destination.Home> {
            HomeScreen(
                onNewTracker = { navController.navigate(Destination.TrackerEditor()) },
                onOpenTracker = { id -> navController.navigate(Destination.TrackerEditor(id)) },
                onLog = { id -> navController.navigate(Destination.LogEntry(id)) },
            )
        }
        composable<Destination.LogEntry> {
            LogEntryScreen(onDone = { navController.popBackStack() })
        }
        composable<Destination.TrackerEditor> {
            TrackerEditorScreen(onDone = { navController.popBackStack() })
        }
    }
}
