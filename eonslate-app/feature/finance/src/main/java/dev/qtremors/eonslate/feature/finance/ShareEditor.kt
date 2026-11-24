package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
import dev.qtremors.eonslate.core.tracker.domain.finance.PartyKind
import dev.qtremors.eonslate.core.tracker.domain.finance.ShareKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.finance.shareKind
import dev.qtremors.eonslate.core.tracker.domain.finance.own
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup

/** What a payment is with people, as chosen in the editor. [MINE] is all yours. */
enum class ShareMode { MINE, LENT, SPLIT, PAID_BACK, BORROWED, GOT_BACK }

/** The editor's choice about people: the mode, and for a split, who it is split with by name. */
data class ShareDraft(val mode: ShareMode = ShareMode.MINE, val splitWith: List<String> = emptyList(), val includeMe: Boolean = true) {
    /** Lending, borrowing and paying back are with the payee named above, so they need one. */
    val needsParty: Boolean get() = mode != ShareMode.MINE && mode != ShareMode.SPLIT
}

internal fun ShareMode.fits(kind: TransactionKind): Boolean = when (kind) {
    TransactionKind.EXPENSE -> this in setOf(ShareMode.MINE, ShareMode.LENT, ShareMode.SPLIT, ShareMode.PAID_BACK)
    TransactionKind.INCOME -> this in setOf(ShareMode.MINE, ShareMode.BORROWED, ShareMode.GOT_BACK)
    TransactionKind.TRANSFER -> this == ShareMode.MINE
}

internal fun ShareKind.mode(): ShareMode = when (this) {
    ShareKind.LENT -> ShareMode.LENT
    ShareKind.SPLIT -> ShareMode.SPLIT
    ShareKind.PAID_BACK -> ShareMode.PAID_BACK
    ShareKind.BORROWED -> ShareMode.BORROWED
    ShareKind.GOT_BACK -> ShareMode.GOT_BACK
}

/** How an existing transaction opens in the editor. */
internal fun Transaction?.shareDraft(parties: Map<String, Party>): ShareDraft {
    val kind = this?.shareKind ?: return ShareDraft()
    return if (kind == ShareKind.SPLIT) {
        ShareDraft(ShareMode.SPLIT, shares.mapNotNull { parties[it.partyId]?.name }, includeMe = own > 0)
    } else ShareDraft(kind.mode())
}

@Composable
internal fun ShareMode.label(): String = stringResource(
    when (this) {
        ShareMode.MINE -> R.string.share_mine
        ShareMode.LENT -> R.string.share_lent
        ShareMode.SPLIT -> R.string.share_split
        ShareMode.PAID_BACK -> R.string.share_paid_back
        ShareMode.BORROWED -> R.string.share_borrowed
        ShareMode.GOT_BACK -> R.string.share_got_back
    },
)

/**
 * Whether money is all yours, or with people: lent, split, borrowed or paid back. Lending and
 * borrowing are with the person named in Paid to or Received from; a split picks its people here.
 */
@Composable
internal fun ShareEditor(
    draft: ShareDraft,
    onDraft: (ShareDraft) -> Unit,
    kind: TransactionKind,
    party: String,
    people: List<Party>,
    enabled: Boolean,
) {
    val modes = ShareMode.entries.filter { it.fits(kind) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        ConnectedChoices(modes, draft.mode.takeIf { it in modes } ?: ShareMode.MINE, { it.label() }, { if (enabled) onDraft(draft.copy(mode = it)) })
        when {
            draft.needsParty && party.isBlank() -> Text(
                stringResource(R.string.share_needs_party),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            draft.mode == ShareMode.SPLIT -> SplitPeople(draft, onDraft, people, enabled)
        }
    }
}

@Composable
private fun SplitPeople(draft: ShareDraft, onDraft: (ShareDraft) -> Unit, people: List<Party>, enabled: Boolean) {
    var typed by rememberSaveable { mutableStateOf("") }
    val picked = draft.splitWith.map(Party::keyOf).toSet()
    val choices = (draft.splitWith + people.filter { it.kind == PartyKind.PERSON }.map { it.name })
        .distinctBy(Party::keyOf).take(MAX_PEOPLE)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { name ->
            val on = Party.keyOf(name) in picked
            FilterChip(
                selected = on,
                onClick = {
                    if (enabled) onDraft(draft.copy(splitWith = if (on) draft.splitWith.filterNot { Party.keyOf(it) == Party.keyOf(name) } else draft.splitWith + name))
                },
                label = { Text(name) },
                leadingIcon = { Icon(if (on) Icons.Rounded.Check else Icons.Rounded.Person, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
    val add = {
        val name = typed.trim()
        if (name.isNotEmpty() && Party.keyOf(name) !in picked) onDraft(draft.copy(splitWith = draft.splitWith + name))
        typed = ""
    }
    FormField(
        value = typed,
        onValueChange = { typed = it },
        label = stringResource(R.string.share_add_person),
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Done,
        maxLength = Party.NAME_LIMIT,
        enabled = enabled,
        trailingIcon = {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = add, enabled = typed.isNotBlank()) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.share_add_person))
            }
        },
    )
    ConnectedGroup {
        ToggleRow(
            title = stringResource(R.string.share_include_me),
            supporting = stringResource(R.string.share_include_me_hint),
            checked = draft.includeMe,
            onCheckedChange = { if (enabled) onDraft(draft.copy(includeMe = it)) },
            icon = Icons.Rounded.Person,
        )
    }
    if (draft.splitWith.isNotEmpty()) {
        val count = draft.splitWith.size + if (draft.includeMe) 1 else 0
        Text(
            stringResource(R.string.share_equal, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private const val MAX_PEOPLE = 12
