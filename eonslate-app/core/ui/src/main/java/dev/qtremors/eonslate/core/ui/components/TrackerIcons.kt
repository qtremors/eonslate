package dev.qtremors.eonslate.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The icons a tracker can show, stored by name so the database never holds a resource id that
 * changes between builds. Unknown names fall back to [DEFAULT], so an old backup or a name from
 * a newer version still shows something.
 */
object TrackerIcons {
    const val DEFAULT_NAME = "AutoAwesome"
    val DEFAULT: ImageVector get() = Icons.Rounded.AutoAwesome

    /** Every icon the tracker builder offers, by stored name, in the order it shows them. */
    val all: Map<String, ImageVector> by lazy {
        linkedMapOf(
            DEFAULT_NAME to Icons.Rounded.AutoAwesome,
            "Payments" to Icons.Rounded.Payments,
            "Mood" to Icons.Rounded.Mood,
            "FitnessCenter" to Icons.Rounded.FitnessCenter,
            "Book" to Icons.Rounded.Book,
            "LocalDrink" to Icons.Rounded.LocalDrink,
            "Medication" to Icons.Rounded.Medication,
            "Bedtime" to Icons.Rounded.Bedtime,
            "SelfImprovement" to Icons.Rounded.SelfImprovement,
            "Timer" to Icons.Rounded.Timer,
            "Favorite" to Icons.Rounded.Favorite,
            "Star" to Icons.Rounded.Star,
        )
    }

    fun resolve(name: String): ImageVector = all[name] ?: DEFAULT
}
