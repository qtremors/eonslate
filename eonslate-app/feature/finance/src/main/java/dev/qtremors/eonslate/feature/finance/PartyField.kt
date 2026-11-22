package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
import dev.qtremors.eonslate.core.tracker.domain.finance.PartyKind
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.ui.components.FormField

internal fun PartyKind.icon(): ImageVector = if (this == PartyKind.BUSINESS) Icons.Rounded.Storefront else Icons.Rounded.Person

/** People and businesses, the most recently paid first, then the rest by name. */
internal fun Ledger.partiesByUse(): List<Party> {
    val last = HashMap<String, Long>()
    for (t in transactions) t.partyId?.let { id -> if (t.epochDay > (last[id] ?: Long.MIN_VALUE)) last[id] = t.epochDay }
    return parties.sortedWith(compareByDescending<Party> { last[it.id] ?: Long.MIN_VALUE }.thenBy { it.key })
}

/**
 * Who was paid, or paid you: type a name, or tap one of the people and businesses already used.
 * A name not seen before becomes a new one when the transaction is saved.
 */
@Composable
internal fun PartyField(name: String, onName: (String) -> Unit, kind: TransactionKind, parties: List<Party>, enabled: Boolean) {
    val key = Party.keyOf(name)
    val known = parties.firstOrNull { it.key == key }
    val matches = remember(key, parties) {
        if (known != null) emptyList() else parties.filter { key.isEmpty() || key in it.key }.take(MAX_SUGGESTIONS)
    }
    FormField(
        value = name,
        onValueChange = onName,
        label = stringResource(if (kind == TransactionKind.INCOME) R.string.party_from else R.string.party_to),
        supporting = if (key.isNotEmpty() && known == null) stringResource(R.string.party_new) else null,
        capitalization = KeyboardCapitalization.Words,
        maxLength = Party.NAME_LIMIT,
        enabled = enabled,
    )
    if (matches.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            matches.forEach { party ->
                FilterChip(
                    selected = false,
                    onClick = { onName(party.name) },
                    label = { Text(party.name) },
                    leadingIcon = { Icon(party.kind.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = MaterialTheme.shapes.medium,
                    enabled = enabled,
                )
            }
        }
    }
}

private const val MAX_SUGGESTIONS = 6
