package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.MenuAction

/** The currencies the accounts use, in account order. */
internal fun Ledger.currencies(): List<String> = accounts.map { it.currency }.distinct()

/**
 * The one currency totals and charts are shown in, on every tab alike: the chosen one when an
 * account uses it, otherwise the first account's. Amounts are never added across currencies, and
 * showing one at a time keeps each screen a single set of figures rather than one per currency.
 */
internal fun Ledger.shownCurrency(chosen: String): String {
    val used = currencies()
    return if (used.isEmpty() || chosen in used) chosen else used.first()
}

/**
 * The shown currency as a small tag. With accounts in more than one currency it opens a menu to
 * pick another, and the pick holds on every tab; with one currency it is only a label.
 */
@Composable
internal fun CurrencyPicker(current: String, currencies: List<String>, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val several = currencies.size > 1
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.finance_shown_currency, current)
    Box(modifier) {
        val content: @Composable () -> Unit = {
            Row(
                Modifier.padding(start = 10.dp, end = if (several) 4.dp else 10.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(current, style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer)
                if (several) Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(18.dp))
            }
        }
        if (several) {
            Surface(onClick = { open = true }, color = colors.secondaryContainer, shape = CircleShape, modifier = Modifier.semantics { contentDescription = label }, content = content)
            EonslateMenu(
                open,
                onDismissRequest = { open = false },
                groups = listOf(currencies.map { code -> MenuAction(code, selected = code == current) { onPick(code) } }),
            )
        } else {
            Surface(color = colors.secondaryContainer, shape = CircleShape, modifier = Modifier.semantics { contentDescription = label }, content = content)
        }
    }
}
