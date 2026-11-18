package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import java.util.Locale

/** Category names to offer, by kind of money; provided by the finance screen from the ledger. */
internal val LocalCategorySuggestions = compositionLocalOf<(TransactionKind) -> List<String>> { { emptyList() } }
internal val LocalCategoryStyles = compositionLocalOf<List<CategoryChoice>> { emptyList() }

/** The icon of a default category, or null for one a person named. */
internal fun categoryIcon(name: String): ImageVector? = when (name.trim().lowercase(Locale.ROOT)) {
    "food & dining", "food" -> Icons.Rounded.Restaurant
    "transport" -> Icons.Rounded.DirectionsCar
    "shopping" -> Icons.Rounded.ShoppingCart
    "bills", "utilities" -> Icons.Rounded.Lightbulb
    "health" -> Icons.Rounded.Favorite
    "entertainment" -> Icons.Rounded.Movie
    "subscriptions" -> Icons.Rounded.CreditCard
    "salary", "income" -> Icons.Rounded.Work
    "other" -> Icons.Rounded.AutoAwesome
    else -> null
}

/**
 * Pick a category with one tap: what has been used most, then the defaults. "New" opens a field
 * for any other name. Tapping the picked chip again clears it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryPicker(value: String, onChange: (String) -> Unit, kind: TransactionKind, busy: Boolean) {
    val suggestions = LocalCategorySuggestions.current(kind)
    val known = suggestions.any { it.equals(value.trim(), ignoreCase = true) }
    var typing by rememberSaveable { mutableStateOf(value.isNotBlank() && !known) }
    val haptics = rememberAppHaptics()
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.finance_category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            suggestions.forEach { name ->
                val category = LocalCategoryStyles.current.firstOrNull { it.name.equals(name, ignoreCase = true) }
                val icon = financeIcon(category?.icon) ?: categoryIcon(name)
                val selected = !typing && name.equals(value.trim(), ignoreCase = true)
                FilterChip(
                    selected = selected,
                    enabled = !busy,
                    onClick = {
                        haptics.toggle(!selected)
                        typing = false
                        onChange(if (selected) "" else name)
                    },
                    label = { Text(name) },
                    leadingIcon = when {
                        category?.color != null && !selected -> {
                            { FinanceBadge(category.icon, category.color, name, size = 26.dp) }
                        }
                        icon != null -> {
                            { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        }
                        else -> null
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = colors,
                )
            }
            FilterChip(
                selected = typing,
                enabled = !busy,
                onClick = {
                    typing = !typing
                    if (!typing && !known) onChange("")
                },
                label = { Text(stringResource(R.string.finance_category_new)) },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.medium,
                colors = colors,
            )
        }
        if (typing) {
            FormField(value, onChange, stringResource(R.string.finance_category), maxLength = Transaction.CATEGORY_LIMIT, enabled = !busy)
        }
    }
}
