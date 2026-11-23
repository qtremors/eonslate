package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyBank
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyMove
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyStatus
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.components.ProgressRing
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import java.time.LocalDate

/** "₹45,000 of ₹1,00,000 · HDFC · by 12 Mar". */
@Composable
internal fun piggyLine(status: PiggyStatus, account: Account?): String {
    val locale = LocalValueDisplay.current.numberLocale
    val currency = account?.currency ?: return ""
    return listOfNotNull(
        stringResource(
            R.string.piggy_of,
            ValueFormat.money(status.saved, currency, locale),
            ValueFormat.money(status.target, currency, locale),
        ),
        account.label,
        status.piggy.targetEpochDay?.let { stringResource(R.string.piggy_by, displayDate(it)) },
    ).joinToString(" · ")
}

/** Every piggy bank as a row with how full it is; a tap opens one, New makes another. */
@Composable
internal fun PiggySection(statuses: List<PiggyStatus>, accounts: Map<String, Account>, onOpen: (String) -> Unit, onNew: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.piggy_title)) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onNew) { Text(stringResource(R.string.piggy_new)) }
        }
        ConnectedGroup {
            statuses.forEachIndexed { index, status ->
                ConnectedItem(index, statuses.size) {
                    ConnectedRow(
                        title = status.piggy.name,
                        supporting = piggyLine(status, accounts[status.piggy.accountId]),
                        leading = {
                            ProgressRing(
                                fraction = status.fraction,
                                color = MaterialTheme.colorScheme.primary,
                                contentDescription = "${(status.fraction * 100).toInt()}%",
                                size = 40.dp,
                                thickness = 5.dp,
                            ) { Icon(Icons.Rounded.Savings, contentDescription = null, modifier = Modifier.padding(10.dp)) }
                        },
                        onClick = { onOpen(status.piggy.id) },
                    )
                }
            }
        }
    }
}

/** Makes or changes a piggy bank: what it is for, which account keeps it, the goal, and maybe a day. */
@Composable
internal fun PiggyEditor(
    piggy: PiggyBank?,
    accounts: List<Account>,
    busy: Boolean,
    failed: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (PiggyBank) -> Unit,
    onDelete: (() -> Unit)?,
    newId: () -> String,
) {
    var name by rememberSaveable { mutableStateOf(piggy?.name.orEmpty()) }
    var accountId by rememberSaveable { mutableStateOf(piggy?.accountId ?: accounts.first().id) }
    val account = accounts.firstOrNull { it.id == accountId } ?: accounts.first()
    var target by rememberSaveable { mutableStateOf(piggy?.targetMinor?.let { moneyInput(it, account.currency) }.orEmpty()) }
    var dated by rememberSaveable { mutableStateOf(piggy?.targetEpochDay != null) }
    var date by rememberSaveable { mutableStateOf((piggy?.targetEpochDay?.let(LocalDate::ofEpochDay) ?: today.plusMonths(6)).toString()) }
    val goal = parseMoney(target, account.currency)?.takeIf { it > 0 }
    val day = if (dated) EpochDays.parse(date)?.takeIf { it in Transaction.DATES } else null
    val entered = listOf<Any?>(name, accountId, target, dated, date)
    val opened = rememberSaveable { entered.toString() }
    EditorSheet(stringResource(if (piggy == null) R.string.piggy_new else R.string.piggy_edit), busy, failed, onDismiss, dirty = entered.toString() != opened) {
        FormField(name, { name = it }, stringResource(R.string.piggy_name), placeholder = stringResource(R.string.piggy_name_hint), maxLength = PiggyBank.NAME_LIMIT, enabled = !busy)
        AccountChoice(stringResource(R.string.piggy_account), account, accounts, !busy) {
            if (it.currency != account.currency) target = ""
            accountId = it.id
        }
        FormField(target, { target = it }, stringResource(R.string.piggy_goal), keyboardType = KeyboardType.Decimal, suffix = account.currency, maxLength = 24, enabled = !busy)
        ConnectedGroup {
            ToggleRow(title = stringResource(R.string.piggy_dated), checked = dated, onCheckedChange = { dated = it }, icon = Icons.Rounded.Savings)
        }
        if (dated) DateField(date, { date = it }, stringResource(R.string.piggy_by_label), busy)
        SaveRow(
            canSave = !busy && name.isNotBlank() && goal != null && (!dated || day != null),
            onSave = {
                goal?.let { onSave(PiggyBank(piggy?.id ?: newId(), account.id, name.trim(), it, day, piggy?.createdAt ?: 0)) }
            },
            delete = onDelete?.let { Deletion(stringResource(R.string.piggy_delete, name), R.string.piggy_delete_message, it) },
            busy = busy,
        )
    }
}

/** One piggy bank: how full it is, what is left, every move in and out, and buttons to add or take out. */
@Composable
internal fun PiggySheet(
    status: PiggyStatus,
    account: Account?,
    onDismiss: () -> Unit,
    onMove: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDeleteMove: (PiggyMove) -> Unit,
) {
    val locale = LocalValueDisplay.current.numberLocale
    val currency = account?.currency ?: return
    EditorSheet(status.piggy.name, busy = false, failed = false, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            ProgressRing(
                fraction = status.fraction,
                color = MaterialTheme.colorScheme.primary,
                contentDescription = "${(status.fraction * 100).toInt()}%",
                size = 96.dp,
                thickness = 11.dp,
            ) { Text("${(status.fraction * 100).toInt()}%", style = MaterialTheme.typography.titleMedium) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ValueFormat.money(status.saved, currency, locale), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.piggy_of_goal, ValueFormat.money(status.target, currency, locale)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (status.reached) stringResource(R.string.piggy_reached)
                    else stringResource(R.string.piggy_left, ValueFormat.money(status.left, currency, locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    listOfNotNull(account.label, status.piggy.targetEpochDay?.let { stringResource(R.string.piggy_by, displayDate(it)) }).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onMove(true) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.piggy_add), modifier = Modifier.padding(start = 8.dp))
            }
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onMove(false) }, modifier = Modifier.weight(1f), enabled = status.saved.signum() > 0) {
                Text(stringResource(R.string.piggy_take_out))
            }
        }
        if (status.moves.isNotEmpty()) {
            Text(stringResource(R.string.piggy_history), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            ConnectedGroup {
                status.moves.asReversed().forEachIndexed { index, move ->
                    ConnectedItem(index, status.moves.size) {
                        ConnectedRow(
                            title = (if (move.minor > 0) "+" else "−") + ValueFormat.money(kotlin.math.abs(move.minor), currency, locale),
                            supporting = displayDate(move.epochDay),
                            onLongClick = { onDeleteMove(move) },
                        )
                    }
                }
            }
        }
        TextButton(shapes = ButtonDefaults.shapes(), onClick = onEdit) { Text(stringResource(R.string.piggy_edit)) }
    }
}

/** Puts money in a piggy bank from its account, or takes it back out. */
@Composable
internal fun PiggyMoveSheet(
    status: PiggyStatus,
    currency: String,
    adding: Boolean,
    free: java.math.BigInteger,
    busy: Boolean,
    failed: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit,
) {
    var add by rememberSaveable { mutableStateOf(adding) }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today.toString()) }
    val locale = LocalValueDisplay.current.numberLocale
    val minor = parseMoney(amount, currency)?.takeIf { it > 0 }
    val day = EpochDays.parse(date)?.takeIf { it in Transaction.DATES }
    val tooMuch = !add && minor != null && java.math.BigInteger.valueOf(minor) > status.saved
    EditorSheet(status.piggy.name, busy, failed, onDismiss, dirty = amount.isNotEmpty()) {
        ConnectedChoices(listOf(true, false), add, { stringResource(if (it) R.string.piggy_add else R.string.piggy_take_out) }, { add = it })
        FormField(
            amount, { amount = it }, stringResource(R.string.finance_amount),
            supporting = if (add) stringResource(R.string.piggy_free, ValueFormat.money(free, currency, locale))
            else stringResource(R.string.piggy_in_it, ValueFormat.money(status.saved, currency, locale)),
            error = if (tooMuch) stringResource(R.string.piggy_too_much) else null,
            keyboardType = KeyboardType.Decimal, suffix = currency, maxLength = 24, enabled = !busy,
        )
        DateField(date, { date = it }, stringResource(R.string.finance_date), busy)
        SaveRow(
            canSave = !busy && minor != null && day != null && !tooMuch,
            onSave = { if (minor != null && day != null) onSave(if (add) minor else -minor, day) },
            delete = null,
            busy = busy,
        )
    }
}
