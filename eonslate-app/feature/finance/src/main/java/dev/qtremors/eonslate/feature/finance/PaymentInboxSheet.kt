package dev.qtremors.eonslate.feature.finance

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.MonogramBadge
import dev.qtremors.eonslate.core.ui.components.PressableCard
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import kotlinx.coroutines.launch

/**
 * Payments found in SMS. Each card can be added as it is, after picking its category, or opened
 * to review every detail; it stays here until it is saved or discarded.
 */
@Composable
internal fun PaymentInboxSheet(
    ledger: Ledger,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onReview: (PaymentMessage) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.inbox.collectAsStateWithLifecycle()
    var days by rememberSaveable { mutableStateOf(30) }
    var denied by rememberSaveable { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<BatchResult?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) viewModel.scanInbox(days)
    }
    val keys = ledger.transactions.mapNotNullTo(HashSet()) { it.smsKey }
    val held by viewModel.detection.pending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // A category picked on a card, by its fingerprint, until it is added.
    val picked = remember { mutableStateMapOf<String, String>() }
    // Payments found on open come first, before any manual scan.
    val messages = (state.messages ?: held).filter { it.key !in keys }
    val withCategory = { item: PaymentMessage -> item.key?.let(picked::get)?.let { item.copy(category = it) } ?: item }
    val add = { items: List<PaymentMessage> -> scope.launch { outcome = viewModel.approveAll(items.map(withCategory)) } }
    EonslateBottomSheet(onDismiss, title = stringResource(R.string.finance_scan_inbox)) {
        ConnectedChoices(
            listOf(7, 30, 90),
            days,
            { pluralStringResource(R.plurals.finance_sms_scan_days, it, it) },
            {
                viewModel.clearInbox()
                days = it
            },
        )
        Button(
            shapes = ButtonDefaults.shapes(),
            enabled = !state.scanning,
            onClick = {
                denied = false
                outcome = null
                if (PaymentInbox.hasAccess(context)) viewModel.scanInbox(days)
                else permission.launch(Manifest.permission.READ_SMS)
            },
        ) { Text(stringResource(R.string.finance_sms_scan)) }

        outcome?.let { Text(batchText(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when {
            state.scanning -> LoadingState()
            denied -> Text(stringResource(R.string.finance_sms_denied), modifier = Modifier.padding(vertical = 8.dp))
            state.failed -> Text(stringResource(R.string.finance_sms_failed), modifier = Modifier.padding(vertical = 8.dp))
            state.messages != null && messages.isEmpty() ->
                Text(stringResource(R.string.finance_sms_empty), modifier = Modifier.padding(vertical = 8.dp))
            messages.isNotEmpty() -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { scope.launch { viewModel.detection.discard(messages) }; viewModel.clearInbox() }) {
                    Text(stringResource(R.string.sms_discard_all))
                }
                Button(shapes = ButtonDefaults.shapes(), enabled = messages.any { m -> ledger.accounts.any { it.currency == m.currency } }, onClick = {
                    add(messages)
                }) { Text(stringResource(R.string.sms_add_all)) }
            }
        }
        if (!state.scanning && messages.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages, key = { it.key ?: it.hashCode().toString() }) { item ->
                    FoundPayment(
                        item = withCategory(item),
                        ledger = ledger,
                        onCategory = { name -> item.key?.let { picked[it] = name } },
                        onAdd = { add(listOf(item)) },
                        onDiscard = { scope.launch { viewModel.detection.discard(listOf(item)) } },
                        onReview = { onReview(withCategory(item)) },
                    )
                }
            }
        }
    }
}

/** What a batch did, in one line: added, and any left out with why. */
@Composable
internal fun batchText(result: BatchResult): String = listOfNotNull(
    pluralStringResource(R.plurals.sms_added, result.added, result.added),
    result.noAccount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.sms_no_account, it, it) },
    result.failed.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.sms_failed, it, it) },
).joinToString(" · ")

/** One found payment: who and how much, where it came from, its category, and Add or Discard. */
@Composable
private fun FoundPayment(
    item: PaymentMessage,
    ledger: Ledger,
    onCategory: (String) -> Unit,
    onAdd: () -> Unit,
    onDiscard: () -> Unit,
    onReview: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val eligible = ledger.accounts.any { it.currency == item.currency }
    val title = item.merchant ?: item.issuer ?: item.sender ?: item.kind.label()
    val account = item.accountEnding?.let {
        stringResource(if (item.isCard) R.string.finance_sms_card_ending else R.string.finance_sms_ending, it)
    }
    val reference = item.reference?.let { stringResource(R.string.finance_sms_reference, it) }
    val supporting = listOfNotNull(
        (item.issuer ?: item.sender)?.takeUnless { it == title }, account, reference, item.epochDay?.let { displayDate(it) },
    ).joinToString(" · ")
    var choosing by remember { mutableStateOf(false) }
    PressableCard(Modifier.fillMaxWidth(), onClick = onReview, color = colors.surfaceBright) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonogramBadge(title, title)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1)
                    if (supporting.isNotBlank()) Text(supporting, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 2)
                }
                Amount(item.kind, ValueFormat.money(item.minor, item.currency, LocalValueDisplay.current.numberLocale))
            }
            if (!eligible) Text(stringResource(R.string.finance_sms_account, item.currency), style = MaterialTheme.typography.bodySmall, color = colors.error)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    AssistChip(
                        onClick = { choosing = true },
                        enabled = eligible,
                        label = { Text(item.category ?: stringResource(R.string.overview_uncategorized), maxLines = 1) },
                        leadingIcon = categoryIcon(item.category.orEmpty())?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                        trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = MaterialTheme.shapes.medium,
                    )
                    EonslateMenu(
                        expanded = choosing,
                        onDismissRequest = { choosing = false },
                        groups = listOf(
                            Categories.suggestions(ledger, item.kind).map { name ->
                                MenuAction(name, selected = name.equals(item.category, ignoreCase = true)) { onCategory(name) }
                            },
                        ),
                    )
                }
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDiscard) { Text(stringResource(R.string.sms_discard)) }
                Button(shapes = ButtonDefaults.shapes(), enabled = eligible, onClick = onAdd) { Text(stringResource(R.string.sms_add)) }
            }
        }
    }
}
