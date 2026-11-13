package dev.qtremors.eonslate

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.qtremors.eonslate.core.navigation.api.Destination
import dev.qtremors.eonslate.feature.home.HomeScreen

@Composable
fun EonslateNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destination.Home) {
        composable<Destination.Home> { HomeScreen() }
    }
}
