package dev.qtremors.eonslate

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.qtremors.eonslate.core.navigation.api.Destination

/*
 * Page motion: springs, so a released back gesture settles from where it was. Tabs crossfade;
 * pages slide in from the side and back out the same way.
 */
private fun <T> pageSpring(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

private val offsetSpring: SpringSpec<IntOffset> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

private fun NavBackStackEntry.isTab(): Boolean = destination.let {
    it.hasRoute<Destination.Home>() || it.hasRoute<Destination.Transactions>() ||
        it.hasRoute<Destination.Subscriptions>() || it.hasRoute<Destination.Trackers>()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs() = initialState.isTab() && targetState.isTab()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellEnter(reduceMotion: Boolean): EnterTransition = when {
    reduceMotion -> fadeIn(tween(0))
    betweenTabs() -> fadeIn(pageSpring())
    else -> slideInHorizontally(offsetSpring) { it } + fadeIn(pageSpring()) + scaleIn(pageSpring(), initialScale = 0.94f)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellExit(reduceMotion: Boolean): ExitTransition = when {
    reduceMotion -> fadeOut(tween(0))
    betweenTabs() -> fadeOut(pageSpring())
    else -> slideOutHorizontally(offsetSpring) { -it / 3 } + fadeOut(pageSpring()) + scaleOut(pageSpring(), targetScale = 1.04f)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellPopEnter(reduceMotion: Boolean): EnterTransition = when {
    reduceMotion -> fadeIn(tween(0))
    betweenTabs() -> fadeIn(pageSpring())
    else -> slideInHorizontally(offsetSpring) { -it / 3 } + fadeIn(pageSpring()) + scaleIn(pageSpring(), initialScale = 1.04f)
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.shellPopExit(reduceMotion: Boolean): ExitTransition = when {
    reduceMotion -> fadeOut(tween(0))
    betweenTabs() -> fadeOut(pageSpring())
    else -> slideOutHorizontally(offsetSpring) { it } + fadeOut(pageSpring()) + scaleOut(pageSpring(), targetScale = 0.94f)
}
