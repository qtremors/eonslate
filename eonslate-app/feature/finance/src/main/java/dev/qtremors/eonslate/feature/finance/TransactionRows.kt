package dev.qtremors.eonslate.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.shareKind

/** What a transaction's row says, the same on every screen. */
internal data class RowText(val title: String, val details: String?)

/**
 * One rule for every list: who it was with names it; else the note, the category, the kind. Under
 * it, in order: pending, lent or borrowed, the note when the name took the title, the category,
 * the account only when it is not the [usual] one, the day when [day] is set, the notes, and how
 * many attachments it keeps.
 */
@Composable
internal fun rowText(
    t: Transaction,
    parties: Map<String, Party>,
    accounts: Map<String, Account>,
    usual: String?,
    day: String? = null,
    /** How many receipts and links it keeps. */
    attached: Int = 0,
): RowText {
    val party = t.partyId?.let(parties::get)?.name
    val title = party ?: t.note.ifBlank { t.category.ifBlank { t.kind.label() } }
    val source = accounts[t.accountId]
    val target = t.destinationId?.let(accounts::get)
    val account = when {
        target != null && source != null -> stringResource(R.string.finance_transfer_accounts, source.label, target.label)
        source != null && source.id != usual -> source.label
        else -> null
    }
    val details = listOfNotNull(
        stringResource(R.string.finance_pending).takeIf { t.pending },
        t.shareKind?.label(),
        t.note.takeIf { party != null && it.isNotBlank() },
        t.category.takeIf { it.isNotBlank() && it != title },
        account,
        day,
        t.details.lineSequence().firstOrNull { it.isNotBlank() }?.trim(),
        attachedText(attached),
    ).joinToString(" · ").ifBlank { null }
    return RowText(title, details)
}
