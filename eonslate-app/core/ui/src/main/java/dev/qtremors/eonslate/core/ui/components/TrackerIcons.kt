package dev.qtremors.eonslate.core.ui.components

import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.NoDrinks
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Soap
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
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
            "WaterDrop" to Icons.Rounded.WaterDrop,
            "Restaurant" to Icons.Rounded.Restaurant,
            "LocalCafe" to Icons.Rounded.LocalCafe,
            "DirectionsRun" to Icons.Rounded.DirectionsRun,
            "DirectionsBike" to Icons.Rounded.DirectionsBike,
            "Pool" to Icons.Rounded.Pool,
            "Spa" to Icons.Rounded.Spa,
            "MonitorHeart" to Icons.Rounded.MonitorHeart,
            "Healing" to Icons.Rounded.Healing,
            "Psychology" to Icons.Rounded.Psychology,
            "TwoWheeler" to Icons.Rounded.TwoWheeler,
            "DirectionsCar" to Icons.Rounded.DirectionsCar,
            "LocalGasStation" to Icons.Rounded.LocalGasStation,
            "Build" to Icons.Rounded.Build,
            "CleaningServices" to Icons.Rounded.CleaningServices,
            "ContentCut" to Icons.Rounded.ContentCut,
            "Shower" to Icons.Rounded.Shower,
            "Soap" to Icons.Rounded.Soap,
            "Pets" to Icons.Rounded.Pets,
            "Eco" to Icons.Rounded.Eco,
            "Home" to Icons.Rounded.Home,
            "ShoppingCart" to Icons.Rounded.ShoppingCart,
            "Savings" to Icons.Rounded.Savings,
            "Work" to Icons.Rounded.Work,
            "School" to Icons.Rounded.School,
            "Code" to Icons.Rounded.Code,
            "Brush" to Icons.Rounded.Brush,
            "MusicNote" to Icons.Rounded.MusicNote,
            "CameraAlt" to Icons.Rounded.CameraAlt,
            "SportsEsports" to Icons.Rounded.SportsEsports,
            "Flight" to Icons.Rounded.Flight,
            "Language" to Icons.Rounded.Language,
            "Lightbulb" to Icons.Rounded.Lightbulb,
            "Bolt" to Icons.Rounded.Bolt,
            "Event" to Icons.Rounded.Event,
            "SmokeFree" to Icons.Rounded.SmokeFree,
            "NoDrinks" to Icons.Rounded.NoDrinks,
            "LocalBar" to Icons.Rounded.LocalBar,
            "Science" to Icons.Rounded.Science,
            "Phone" to Icons.Rounded.Phone,
        )
    }

    fun resolve(name: String): ImageVector = all[name] ?: DEFAULT
}
