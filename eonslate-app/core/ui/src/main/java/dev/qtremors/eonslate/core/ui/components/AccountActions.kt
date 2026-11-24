package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.R
import dev.qtremors.eonslate.core.ui.theme.EonslateDisplay

/**
 * The bell and profile picture at the end of each top-level tab's bar. The app fills it in;
 * a tab without it falls back to its own settings button.
 */
val LocalAccountActions = staticCompositionLocalOf<(@Composable RowScope.() -> Unit)?> { null }

/**
 * The app's mark on its own, drawn in the theme's text color: dark on light, light on dark and
 * pure black, with the page showing through around it. The drawing keeps the launcher's padding,
 * so it is drawn larger than an icon.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Icon(
        painter = painterResource(R.drawable.eonslate_mark),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.size(size),
    )
}

/** Opens the notifications; filled with a count while anything waits. */
@Composable
fun NotificationBell(count: Int, onClick: () -> Unit) {
    val label = if (count > 0) pluralStringResource(R.plurals.notifications_waiting, count, count)
    else stringResource(R.string.action_notifications)
    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onClick) {
        BadgedBox(badge = { if (count > 0) Badge { Text(if (count > 99) "99+" else count.toString()) } }) {
            Icon(if (count > 0) Icons.Rounded.Notifications else Icons.Outlined.Notifications, contentDescription = label)
        }
    }
}

/** The person's initial in a circle, or a figure until they give a name; opens the profile. */
@Composable
fun ProfileAvatar(name: String, onClick: () -> Unit, size: Dp = 32.dp) {
    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onClick) {
        Avatar(name, size = size, contentDescription = stringResource(R.string.action_profile))
    }
}

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 32.dp, contentDescription: String? = null) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(size).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
        val initial = name.trim().take(1).uppercase()
        if (initial.isEmpty()) {
            Icon(Icons.Rounded.Person, contentDescription = contentDescription, tint = colors.onPrimary, modifier = Modifier.size(size * 0.6f))
        } else {
            Text(
                initial,
                style = (if (size > 40.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall)
                    .copy(fontFamily = EonslateDisplay),
                color = colors.onPrimary,
            )
        }
    }
}
