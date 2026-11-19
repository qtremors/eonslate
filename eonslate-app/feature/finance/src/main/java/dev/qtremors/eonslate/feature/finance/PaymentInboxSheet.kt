package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MonogramBadge

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
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) viewModel.scanInbox(days)
    }
    val keys = ledger.transactions.mapNotNullTo(HashSet()) { it.smsKey }
    val held by viewModel.detection.pending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Payments found on open come first, before any manual scan.
    val messages = (state.messages ?: held).filter { it.key !in keys }
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
                if (PaymentInbox.hasAccess(context)) viewModel.scanInbox(days)
                else permission.launch(Manifest.permission.READ_SMS)
            },
        ) { Text(stringResource(R.string.finance_sms_scan)) }

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
                    scope.launch { viewModel.approveAll(messages) }
                }) { Text(stringResource(R.string.sms_add_all)) }
            }
        }
        if (!state.scanning && messages.isNotEmpty()) {
            LazyColumn(
                Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(messages, key = { index, item -> item.key ?: index }) { index, item ->
                    val date = item.epochDay?.let { displayDate(it) }
                    val account = item.accountEnding?.let {
                        stringResource(if (item.isCard) R.string.finance_sms_card_ending else R.string.finance_sms_ending, it)
                    }
                    val reference = item.reference?.let { stringResource(R.string.finance_sms_reference, it) }
                    val amount = ValueFormat.money(item.minor, item.currency, LocalValueDisplay.current.numberLocale)
                    val eligible = ledger.accounts.any { it.currency == item.currency }
                    val title = item.merchant ?: item.issuer ?: item.sender ?: item.kind.label()
                    val supporting = listOfNotNull(
                        (item.issuer ?: item.sender)?.takeUnless { it == title }, item.category, account, reference, date,
                        if (eligible) null else stringResource(R.string.finance_sms_account, item.currency),
                    ).joinToString(" · ")
                    ConnectedItem(index, messages.size) {
                        ConnectedRow(
                            title,
                            supporting = supporting,
                            leading = { MonogramBadge(title, title) },
                            enabled = eligible,
                            trailing = {
                                Row {
                                    Amount(item.kind, amount)
                                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { scope.launch { viewModel.detection.discard(listOf(item)) } }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.sms_discard))
                                    }
                                }
                            },
                            onClick = {
                                viewModel.detection.reviewed(item)
                                onReview(item)
                            },
                        )
                    }
                }
            }
        }
    }
}
