package dev.qtremors.eonslate.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

/** A page's main action, shown by the shell's floating toolbar. */
@Immutable
class PageAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * The app shell's floating toolbar, as pages see it. Inside the shell, [MainActionButton] hands
 * its action here and [EonslateScaffold] hands its title, so the toolbar shows the page's action
 * beside the tabs and its title beside Back. The page shown last wins: during a transition the
 * incoming page registers after the outgoing one, which then leaves when it is disposed.
 */
@Stable
class ShellHost {
    private class Slot<T>(value: T) {
        var value by mutableStateOf(value)
    }

    private val actions: SnapshotStateList<Slot<PageAction>> = mutableStateListOf()
    private val titles: SnapshotStateList<Slot<String>> = mutableStateListOf()

    val action: PageAction? get() = actions.lastOrNull()?.value
    val title: String? get() = titles.lastOrNull()?.value

    @Composable
    internal fun Offer(action: PageAction) = offer(actions, action)

    @Composable
    internal fun Offer(title: String) = offer(titles, title)

    @Composable
    private fun <T> offer(list: SnapshotStateList<Slot<T>>, value: T) {
        val slot = remember { Slot(value) }
        DisposableEffect(slot) {
            list.add(slot)
            onDispose { list.remove(slot) }
        }
        SideEffect { slot.value = value }
    }
}

/** Null outside the shell, where pages draw their own back arrow and action button. */
val LocalShellHost = staticCompositionLocalOf<ShellHost?> { null }
