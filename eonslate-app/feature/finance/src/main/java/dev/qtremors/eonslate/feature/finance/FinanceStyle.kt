package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.BadgeColors
import dev.qtremors.eonslate.core.ui.components.BadgeIconPicker
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.BadgePigmentPicker
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.SectionHeader

private val financeIcons = linkedMapOf(
    "Wallet" to Icons.Rounded.AccountBalanceWallet,
    "CreditCard" to Icons.Rounded.CreditCard,
    "Restaurant" to Icons.Rounded.Restaurant,
    "DirectionsCar" to Icons.Rounded.DirectionsCar,
    "Movie" to Icons.Rounded.Movie,
    "ShoppingCart" to Icons.Rounded.ShoppingCart,
    "Lightbulb" to Icons.Rounded.Lightbulb,
    "Favorite" to Icons.Rounded.Favorite,
    "Work" to Icons.Rounded.Work,
    "Home" to Icons.Rounded.Home,
    "FitnessCenter" to Icons.Rounded.FitnessCenter,
    "Cloud" to Icons.Rounded.Cloud,
    "Tag" to Icons.Rounded.Label,
    "AutoAwesome" to Icons.Rounded.AutoAwesome,
)

internal fun financeIcon(icon: String?) = financeIcons[icon]

@Composable
internal fun financeBadgeColors(color: Int?, key: String): BadgeColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (color == null) BadgePalette.forKey(key, dark)
    else BadgePalette.forColor(Color(color or 0xFF000000.toInt()), dark)
}

@Composable
internal fun FinanceBadge(
    icon: String?, color: Int?, name: String, category: String = name, size: Dp = 40.dp,
    fallback: String = "Tag",
) {
    IconBadge(
        financeIcon(icon) ?: categoryIcon(category) ?: financeIcons.getValue(fallback),
        name, colors = financeBadgeColors(color, name), size = size,
        iconSize = if (size < 40.dp) 18.dp else 22.dp,
    )
}

@Composable
internal fun FinanceStyleField(
    icon: String?,
    color: Int?,
    name: String,
    category: String,
    fallback: String,
    busy: Boolean,
    onIcon: (String) -> Unit,
    onColor: (Int) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ConnectedGroup {
        ConnectedRow(
            stringResource(R.string.finance_style),
            leading = { FinanceBadge(icon, color, name, category, fallback = fallback) },
            trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
            enabled = !busy,
            onClick = { open = true },
        )
    }
    if (open) EonslateBottomSheet(onDismissRequest = { open = false }, title = stringResource(R.string.finance_style)) {
        val selectedColor = (0 until BadgePalette.size).firstOrNull {
            (BadgePalette.pigment(it).toArgb() and 0xFFFFFF) == color
        }
        val selectedIcon = icon ?: financeIcons.entries.firstOrNull { it.value == categoryIcon(category) }?.key ?: fallback
        Column(
            Modifier.weight(1f, fill = false).heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(stringResource(R.string.finance_icon))
            BadgeIconPicker(selectedIcon, financeIcons, stringArrayResource(R.array.finance_style_icons), financeBadgeColors(color, name), onIcon)
            SectionHeader(stringResource(R.string.finance_color))
            BadgePigmentPicker(selectedColor, stringArrayResource(R.array.finance_style_colors)) {
                onColor(BadgePalette.pigment(it).toArgb() and 0xFFFFFF)
            }
        }
        Button(shapes = ButtonDefaults.shapes(), onClick = { open = false }) {
            Text(stringResource(R.string.finance_done))
        }
    }
}
