package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.settings.Tracking
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessages
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FilterTop
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MainActionButton
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FinanceScreen(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = hiltViewModel(),
    sharedMessage: String? = null,
    onMessageOpened: () -> Unit = {},
) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = Tracking.deviceCurrency())
    val firstDay by viewModel.firstDay.collectAsStateWithLifecycle(initialValue = Tracking().firstDayOfWeek())
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var recordId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewingInbox by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(FinancePage.ACTIVITY) }
    var query by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var prefill by remember { mutableStateOf<PaymentMessage?>(null) }
    val done = {
        editor = null
        message = ""
        prefill = null
        reviewingInbox = false
        viewModel.clearInbox()
    }
    val dismiss = { if (!busy) done() }
    val finishReview = {
        if (reviewingInbox) {
            reviewingInbox = false
            prefill = null
            editor = Editor.INBOX
        } else done()
    }
    val open: (Editor, String?) -> Unit = { type, id ->
        recordId = id
        viewModel.clearError()
        editor = type
    }

    LaunchedEffect(sharedMessage, busy) {
        if (sharedMessage != null && !busy) {
            message = sharedMessage
            open(Editor.SMS, null)
            onMessageOpened()
        }
    }

    EonslateScaffold(
        title = stringResource(R.string.finance_title),
        onBack = onBack,
        filter = if (page == FinancePage.ACTIVITY && ledger?.transactions?.isNotEmpty() == true) {
            FilterTop(query, { query = it }, stringResource(R.string.finance_search))
        } else null,
        actions = {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { open(Editor.SMS, null) }) {
                Icon(Icons.Rounded.Sms, stringResource(R.string.finance_import_sms))
            }
            if (ledger?.accounts?.isNotEmpty() == true) {
                val plans = page == FinancePage.PLANS
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { open(if (plans) Editor.BUDGET else Editor.ACCOUNT, null) }) {
                    Icon(
                        if (plans) Icons.Rounded.Savings else Icons.Rounded.AccountBalanceWallet,
                        stringResource(if (plans) R.string.finance_new_budget else R.string.finance_new_account),
                    )
                }
            }
        },
        floatingActionButton = {
            ledger?.let { data ->
                val empty = data.accounts.isEmpty()
                val action = when {
                    empty -> Editor.ACCOUNT
                    page == FinancePage.PLANS -> Editor.RECURRING
                    else -> Editor.TRANSACTION
                }
                MainActionButton(
                    text = stringResource(
                        when (action) {
                            Editor.ACCOUNT -> R.string.finance_new_account
                            Editor.RECURRING -> R.string.finance_new_recurring
                            Editor.TRANSACTION -> R.string.finance_new_transaction
                            Editor.SMS -> R.string.finance_import_sms
                            Editor.BUDGET -> R.string.finance_new_budget
                            Editor.DUE -> R.string.finance_record_due
                            Editor.INBOX -> R.string.finance_scan_inbox
                        }
                    ),
                    icon = Icons.Rounded.Add,
                    onClick = { open(action, null) },
                )
            }
        },
    ) { padding ->
        val data = ledger
        when {
            data == null -> LoadingState(Modifier.fillMaxSize().padding(padding))
            data.accounts.isEmpty() -> EmptyState(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = stringResource(R.string.finance_empty),
                message = "",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LedgerList(data, padding, page, { page = it }, viewModel.today(), firstDay, query, open)
        }
    }

    val data = ledger ?: return
    when (editor) {
        Editor.ACCOUNT -> {
            val account = data.accounts.firstOrNull { it.id == recordId }
            val canDelete = account != null && data.transactions.none {
                it.accountId == account.id || it.destinationId == account.id
            } && data.recurring.none { it.accountId == account.id } && data.budgets.none { it.accountId == account.id }
            if (recordId == null || account != null) key(recordId) {
                AccountEditor(
                    account = account,
                    defaultCurrency = currency,
                    busy = busy,
                    failed = failed,
                    onDismiss = dismiss,
                    onSave = { name, code, opening, budget ->
                        viewModel.save(Account(account?.id ?: viewModel.newId(), name, code, opening, budget), done)
                    },
                    onDelete = if (account != null && canDelete) ({ viewModel.delete(account, done) }) else null,
                )
            }
        }
        Editor.TRANSACTION -> {
            val transaction = data.transactions.firstOrNull { it.id == recordId }
            if (data.accounts.isNotEmpty() && (recordId == null || transaction != null)) key(recordId) {
                TransactionEditor(
                    transaction = transaction,
                    accounts = data.accounts,
                    today = viewModel.today(),
                    prefill = prefill,
                    busy = busy,
                    failed = failed,
                    onDismiss = { if (!busy) finishReview() },
                    onSave = { account, kind, amount, day, note, destination, category, smsKey ->
                        viewModel.save(
                            Transaction(
                                transaction?.id ?: viewModel.newId(), account, kind, amount, day, note,
                                destination, category, smsKey,
                            ),
                            finishReview,
                        )
                    },
                    onDelete = transaction?.let { { viewModel.delete(it, done) } },
                )
            }
        }
        Editor.RECURRING -> {
            val existing = data.recurring.firstOrNull { it.id == recordId }
            if (data.accounts.isNotEmpty() && (recordId == null || existing != null)) key(recordId) {
                val original = remember { existing }
                RecurringEditor(
                    original, data.accounts, viewModel.today(), busy, failed, dismiss,
                    onSave = { viewModel.schedule(original, it.copy(id = original?.id ?: viewModel.newId()), done) },
                    onDelete = existing?.let { { viewModel.delete(it, done) } },
                    onRecord = existing?.takeIf { !it.paused && it.nextEpochDay != null }?.let {
                        { open(Editor.DUE, it.id) }
                    },
                )
            }
        }
        Editor.BUDGET -> {
            val budget = data.budgets.firstOrNull { it.id == recordId }
            if (data.accounts.isNotEmpty() && (recordId == null || budget != null)) key(recordId) {
                CategoryBudgetEditor(
                    budget, data.accounts, viewModel.today(), busy, failed, dismiss,
                    onSave = { viewModel.save(it.copy(id = budget?.id ?: viewModel.newId()), done) },
                    onDelete = budget?.let { { viewModel.delete(it, done) } },
                )
            }
        }
        Editor.DUE -> {
            val payment = data.recurring.firstOrNull { it.id == recordId }
            val account = data.accounts.firstOrNull { it.id == payment?.accountId }
            if (payment != null && account != null && payment.nextEpochDay != null) {
                val original = remember(payment.id) { payment }
                RecordPaymentEditor(original, account, busy, failed, dismiss) { minor, day ->
                    viewModel.record(original, minor, day, done)
                }
            }
        }
        Editor.SMS -> MessageEditor(
            message, { message = it }, data, dismiss,
            onInbox = if (viewModel.inboxAvailable) ({ open(Editor.INBOX, null) }) else null,
        ) {
            prefill = it
            message = ""
            open(Editor.TRANSACTION, null)
        }
        Editor.INBOX -> PaymentInboxSheet(data, viewModel, dismiss) {
            reviewingInbox = true
            prefill = it
            message = ""
            open(Editor.TRANSACTION, null)
        }
        null -> Unit
    }
}

private enum class Editor { ACCOUNT, TRANSACTION, RECURRING, SMS, BUDGET, DUE, INBOX }

private enum class FinancePage { ACTIVITY, PLANS }

private enum class TransactionPeriod { ALL, TODAY, WEEK, MONTH }

@Composable
private fun LedgerList(
    ledger: Ledger,
    padding: androidx.compose.foundation.layout.PaddingValues,
    page: FinancePage,
    onPage: (FinancePage) -> Unit,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    query: String,
    open: (Editor, String?) -> Unit,
) {
    val balances = remember(ledger) { ledger.balances() }
    val accounts = remember(ledger.accounts) { ledger.accounts.associateBy { it.id } }
    var monthOffset by rememberSaveable { mutableStateOf(0L) }
    val month = YearMonth.from(today).plusMonths(monthOffset)
    val spending = remember(ledger, month) { ledger.monthlySpending(month) }
    var period by rememberSaveable { mutableStateOf(TransactionPeriod.ALL) }
    val firstDay = when (period) {
        TransactionPeriod.ALL -> Transaction.DATES.first
        TransactionPeriod.TODAY -> today.toEpochDay()
        TransactionPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).toEpochDay()
        TransactionPeriod.MONTH -> today.withDayOfMonth(1).toEpochDay()
    }
    val lastDay = if (period == TransactionPeriod.ALL) Transaction.DATES.last else today.toEpochDay()
    val term = query.trim()
    val shown = remember(ledger, firstDay, lastDay, term) {
        ledger.transactions.filter { transaction ->
            transaction.epochDay in firstDay..lastDay && (term.isBlank() ||
                listOf(transaction.note, transaction.category, accounts[transaction.accountId]?.name.orEmpty()).any {
                    it.contains(term, ignoreCase = true)
                })
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            ConnectedChoices(
                FinancePage.entries,
                page,
                { stringResource(if (it == FinancePage.ACTIVITY) R.string.finance_activity else R.string.finance_plans) },
                onPage,
            )
        }
        if (page == FinancePage.PLANS) {
            item { SectionHeader(stringResource(R.string.finance_budgets)) }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { monthOffset-- }) {
                        Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.finance_previous_month))
                    }
                    Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy")))
                    IconButton(shapes = IconButtonDefaults.shapes(), enabled = monthOffset < 0, onClick = { monthOffset++ }) {
                        Icon(Icons.Rounded.ChevronRight, stringResource(R.string.finance_next_month))
                    }
                }
            }
            itemsIndexed(ledger.accounts, key = { _, account -> "budget-" + account.id }) { index, account ->
                val spent = budgetAmount(spending.getOrDefault(account.id, BigInteger.ZERO), account.currency)
                val limit = account.monthlyBudgetMinor
                val label = if (limit == null) stringResource(R.string.finance_budget_unset, spent)
                else stringResource(R.string.finance_budget_progress, spent, budgetAmount(BigInteger.valueOf(limit), account.currency))
                ConnectedItem(index, ledger.accounts.size) {
                    ConnectedRow(account.name, supporting = label, onClick = { open(Editor.ACCOUNT, account.id) })
                }
            }
            itemsIndexed(ledger.budgets, key = { _, budget -> "category-" + budget.id }) { index, budget ->
                val account = accounts.getValue(budget.accountId)
                val spent = remember(ledger, budget, today) { ledger.spendingFor(budget, today) }
                val limit = BigInteger.valueOf(budget.minor)
                val label = stringResource(
                    R.string.finance_budget_progress, budgetAmount(spent, account.currency), budgetAmount(limit, account.currency)
                )
                ConnectedItem(index, ledger.budgets.size) {
                    ConnectedRow(
                        budget.category,
                        supporting = account.name + " · " + label,
                        trailing = if (spent > limit) ({ Text(stringResource(R.string.finance_over_budget), color = MaterialTheme.colorScheme.error) }) else null,
                        onClick = { open(Editor.BUDGET, budget.id) },
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.finance_recurring)) }
            if (ledger.recurring.isEmpty()) {
                item { Text(stringResource(R.string.finance_no_recurring), modifier = Modifier.padding(12.dp)) }
            }
            itemsIndexed(ledger.recurring, key = { _, payment -> "recurring-" + payment.id }) { index, payment ->
                val account = accounts.getValue(payment.accountId)
                val amount = ValueFormat.money(payment.minor, account.currency, Locale.getDefault())
                val due = payment.nextEpochDay?.let { ValueFormat.date(it, Locale.getDefault()) }
                ConnectedItem(index, ledger.recurring.size) {
                    ConnectedRow(
                        title = payment.note.ifBlank { account.name },
                        supporting = listOfNotNull(
                            amount, payment.kind.label(), payment.interval.label(), due,
                            if (payment.paused) stringResource(R.string.finance_paused) else null,
                        ).joinToString(" · "),
                        onClick = { open(Editor.RECURRING, payment.id) },
                    )
                }
            }
            return@LazyColumn
        }
        item { SectionHeader(stringResource(R.string.finance_accounts)) }
        itemsIndexed(ledger.accounts, key = { _, account -> "account-" + account.id }) { index, account ->
            val balance = BigDecimal(balances.getValue(account.id), MoneyAmount.fractionDigits(account.currency))
            ConnectedItem(index, ledger.accounts.size) {
                ConnectedRow(
                    title = account.name,
                    supporting = stringResource(R.string.finance_balance, balance.toPlainString(), account.currency),
                    onClick = { open(Editor.ACCOUNT, account.id) },
                )
            }
        }
        item { SectionHeader(stringResource(R.string.finance_activity)) }
        if (ledger.transactions.isNotEmpty()) {
            item {
                ConnectedChoices(
                    TransactionPeriod.entries, period,
                    { stringResource(when (it) {
                        TransactionPeriod.ALL -> R.string.finance_period_all
                        TransactionPeriod.TODAY -> R.string.finance_period_today
                        TransactionPeriod.WEEK -> R.string.finance_period_week
                        TransactionPeriod.MONTH -> R.string.finance_period_month
                    }) },
                    { period = it },
                )
            }
            val summaries = shown.groupBy { accounts.getValue(it.accountId).currency }
            summaries.forEach { (currency, transactions) ->
                item(key = "totals-" + currency) {
                    val income = transactions.total(TransactionKind.INCOME)
                    val expenses = transactions.total(TransactionKind.EXPENSE)
                    Text(
                        stringResource(R.string.finance_totals, budgetAmount(income, currency), budgetAmount(expenses, currency)),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        if (shown.isEmpty()) {
            item { Text(stringResource(if (ledger.transactions.isEmpty()) R.string.finance_empty_activity else R.string.finance_no_matches), modifier = Modifier.padding(12.dp)) }
        }
        itemsIndexed(shown, key = { _, transaction -> "transaction-" + transaction.id }) { index, transaction ->
            val source = accounts.getValue(transaction.accountId)
            val target = transaction.destinationId?.let(accounts::getValue)
            val accountNames = if (target == null) source.name
            else stringResource(R.string.finance_transfer_accounts, source.name, target.name)
            val amount = ValueFormat.money(transaction.minor, source.currency, Locale.getDefault())
            val title = stringResource(R.string.finance_transaction_title, transaction.kind.label(), amount)
            val date = ValueFormat.date(transaction.epochDay, Locale.getDefault())
            ConnectedItem(index, shown.size) {
                ConnectedRow(
                    title = title,
                    supporting = listOf(accountNames, date, transaction.category, transaction.note).filter { it.isNotBlank() }.joinToString(" · "),
                    onClick = { open(Editor.TRANSACTION, transaction.id) },
                )
            }
        }
    }
}

private fun List<Transaction>.total(kind: TransactionKind): BigInteger =
    asSequence().filter { it.kind == kind }.fold(BigInteger.ZERO) { sum, entry -> sum + BigInteger.valueOf(entry.minor) }

@Composable
private fun AccountEditor(
    account: Account?,
    defaultCurrency: String,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(account?.name.orEmpty()) }
    var code by rememberSaveable { mutableStateOf(account?.currency ?: defaultCurrency) }
    var opening by rememberSaveable { mutableStateOf(account?.let { moneyInput(it.openingMinor, it.currency) }.orEmpty()) }
    var budget by rememberSaveable {
        mutableStateOf(account?.let { existing -> existing.monthlyBudgetMinor?.let { moneyInput(it, existing.currency) } }.orEmpty())
    }
    val currency = code.trim().uppercase(Locale.ROOT)
    val known = MoneyAmount.isKnownCurrency(currency)
    val balance = if (opening.isBlank()) 0L else parseMoney(opening, currency)
    val monthly = parseMoney(budget, currency)?.takeIf { it > 0 }
    val title = stringResource(if (account == null) R.string.finance_new_account else R.string.finance_edit_account)
    EditorSheet(title, busy, failed, onDismiss) {
        FormField(name, { name = it }, stringResource(R.string.finance_name), maxLength = Account.NAME_LIMIT, enabled = !busy)
        FormField(
            code, { code = it }, stringResource(R.string.finance_currency),
            maxLength = 3, enabled = account == null && !busy, capitalization = KeyboardCapitalization.Characters,
        )
        FormField(
            opening, { opening = it }, stringResource(R.string.finance_opening),
            maxLength = 24, keyboardType = KeyboardType.Ascii, enabled = !busy, suffix = currency.takeIf { known },
        )
        FormField(
            budget, { budget = it }, stringResource(R.string.finance_monthly_limit),
            maxLength = 24, keyboardType = KeyboardType.Decimal, enabled = !busy, suffix = currency.takeIf { known },
        )
        SaveRow(
            canSave = !busy && name.isNotBlank() && known && balance != null && (budget.isBlank() || monthly != null),
            onSave = { balance?.let { onSave(name.trim(), currency, it, monthly) } },
            delete = onDelete?.let {
                Deletion(stringResource(R.string.finance_delete_account, account!!.name), R.string.finance_delete_account_message, it)
            },
            busy = busy,
        )
    }
}

@Composable
private fun TransactionEditor(
    transaction: Transaction?,
    accounts: List<Account>,
    today: LocalDate,
    prefill: PaymentMessage?,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, TransactionKind, Long, Long, String, String?, String, String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var kind by rememberSaveable { mutableStateOf(transaction?.kind ?: prefill?.kind ?: TransactionKind.EXPENSE) }
    var accountId by rememberSaveable {
        val matching = accounts.filter { it.currency == prefill?.currency }
        val byEnding = prefill?.accountEnding?.let { ending -> matching.firstOrNull { it.name.trim().endsWith(ending) } }
        mutableStateOf(transaction?.accountId ?: byEnding?.id ?: matching.firstOrNull()?.id ?: accounts.first().id)
    }
    var destinationId by rememberSaveable { mutableStateOf(transaction?.destinationId) }
    val account = accounts.firstOrNull { it.id == accountId } ?: accounts.first()
    val targets = accounts.filter { it.id != account.id && it.currency == account.currency }
    val destination = targets.firstOrNull { it.id == destinationId } ?: targets.firstOrNull()
    var amount by rememberSaveable {
        val initial = transaction?.minor ?: prefill?.takeIf { it.currency == account.currency }?.minor
        mutableStateOf(initial?.let { moneyInput(it, account.currency) }.orEmpty())
    }
    var date by rememberSaveable {
        val initial = transaction?.epochDay ?: prefill?.epochDay
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it).toString() } ?: today.toString())
    }
    val reference = prefill?.reference?.let { stringResource(R.string.finance_sms_reference, it) }
    val smsKey by rememberSaveable { mutableStateOf(transaction?.smsKey ?: prefill?.key) }
    var note by rememberSaveable { mutableStateOf(transaction?.note ?: listOfNotNull(prefill?.merchant, reference).joinToString(" · ")) }
    var category by rememberSaveable { mutableStateOf(transaction?.category ?: prefill?.category.orEmpty()) }
    val parsed = parseMoney(amount, account.currency)?.takeIf { it > 0 }
    val day = EpochDays.parse(date)?.takeIf { it in Transaction.DATES }
    val transfer = kind == TransactionKind.TRANSFER
    val title = stringResource(if (transaction == null) R.string.finance_new_transaction else R.string.finance_edit_transaction)

    EditorSheet(title, busy, failed, onDismiss) {
        ConnectedChoices(TransactionKind.entries, kind, { it.label() }, { if (!busy) kind = it })
        AccountChoice(stringResource(if (transfer) R.string.finance_from else R.string.finance_account), account, accounts, !busy) {
            if (it.currency != account.currency) amount = ""
            accountId = it.id
        }
        if (transfer) {
            if (destination == null) Text(stringResource(R.string.finance_no_destination, account.currency))
            else AccountChoice(stringResource(R.string.finance_to), destination, targets, !busy) { destinationId = it.id }
        }
        FormField(
            amount, { amount = it }, stringResource(R.string.finance_amount),
            keyboardType = KeyboardType.Decimal, suffix = account.currency, maxLength = 24, enabled = !busy,
        )
        FormField(date, { date = it }, stringResource(R.string.finance_date), keyboardType = KeyboardType.Ascii, maxLength = 10, enabled = !busy)
        FormField(note, { note = it }, stringResource(R.string.finance_note), maxLength = Transaction.NOTE_LIMIT, enabled = !busy)
        if (!transfer) {
            FormField(category, { category = it }, stringResource(R.string.finance_category), maxLength = Transaction.CATEGORY_LIMIT, enabled = !busy)
        }
        SaveRow(
            canSave = !busy && parsed != null && day != null && (!transfer || destination != null),
            onSave = {
                if (parsed != null && day != null) {
                    onSave(account.id, kind, parsed, day, note.trim(), destination?.id?.takeIf { transfer }, if (transfer) "" else category.trim(), smsKey)
                }
            },
            delete = onDelete?.let { Deletion(stringResource(R.string.finance_delete_transaction), R.string.finance_delete_transaction_message, it) },
            busy = busy,
        )
    }
}

@Composable
private fun MessageEditor(
    message: String,
    onMessage: (String) -> Unit,
    ledger: Ledger,
    onDismiss: () -> Unit,
    onInbox: (() -> Unit)?,
    onReview: (PaymentMessage) -> Unit,
) {
    var tried by remember { mutableStateOf(false) }
    val parsed = remember(message) { PaymentMessages.parse(message) }
    val duplicate = parsed?.key?.let { key -> ledger.transactions.any { it.smsKey == key } } == true
    val matching = parsed != null && ledger.accounts.any { it.currency == parsed.currency }
    val error = if (!tried) null else when {
        parsed == null -> stringResource(R.string.finance_sms_unknown)
        duplicate -> stringResource(R.string.finance_sms_duplicate)
        !matching -> stringResource(R.string.finance_sms_account, parsed.currency)
        else -> null
    }
    EditorSheet(stringResource(R.string.finance_import_sms), busy = false, failed = false, onDismiss = onDismiss) {
        FormField(
            value = message,
            onValueChange = { tried = false; onMessage(it) },
            label = stringResource(R.string.finance_payment_sms),
            error = error,
            singleLine = false,
            minLines = 3,
            maxLength = PaymentMessages.MAX_LENGTH,
        )
        Button(
            shapes = ButtonDefaults.shapes(),
            enabled = message.isNotBlank(),
            onClick = {
                tried = true
                if (parsed != null && matching && !duplicate) onReview(parsed)
            },
        ) { Text(stringResource(R.string.finance_sms_review)) }
        if (onInbox != null) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onInbox) { Text(stringResource(R.string.finance_scan_inbox)) }
        }
    }
}

@Composable
internal fun EditorSheet(
    title: String,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    EonslateBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, title = title) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (failed) Text(stringResource(R.string.finance_failed), color = MaterialTheme.colorScheme.error)
            content()
        }
    }
}

@Composable
internal fun AccountChoice(label: String, selected: Account, accounts: List<Account>, enabled: Boolean, onPick: (Account) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ConnectedRow(
            title = selected.name,
            supporting = label,
            trailing = { Icon(Icons.Rounded.ExpandMore, null) },
            enabled = enabled,
            onClick = { expanded = true },
        )
        EonslateMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            groups = listOf(accounts.map {
                MenuAction(it.name, supporting = it.currency, selected = it.id == selected.id, onClick = { onPick(it) })
            }),
        )
    }
}

internal data class Deletion(val title: String, val message: Int, val confirm: () -> Unit)

@Composable
internal fun SaveRow(canSave: Boolean, onSave: () -> Unit, delete: Deletion?, busy: Boolean) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
        if (delete != null) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirm = true }, enabled = !busy) {
                Text(stringResource(R.string.finance_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Button(shapes = ButtonDefaults.shapes(), onClick = onSave, enabled = canSave) { Text(stringResource(R.string.finance_save)) }
    }
    if (confirm && delete != null) {
        ConfirmDialog(
            icon = Icons.Rounded.Delete,
            title = delete.title,
            message = stringResource(delete.message),
            confirmLabel = stringResource(R.string.finance_delete),
            onConfirm = { confirm = false; delete.confirm() },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
internal fun TransactionKind.label() = stringResource(
    when (this) {
        TransactionKind.INCOME -> R.string.finance_income
        TransactionKind.EXPENSE -> R.string.finance_expense
        TransactionKind.TRANSFER -> R.string.finance_transfer
    }
)

@Composable
internal fun PaymentInterval.label() = stringResource(
    when (this) {
        PaymentInterval.DAILY -> R.string.finance_daily
        PaymentInterval.WEEKLY -> R.string.finance_weekly
        PaymentInterval.MONTHLY -> R.string.finance_monthly
        PaymentInterval.YEARLY -> R.string.finance_yearly
    }
)

private fun budgetAmount(minor: BigInteger, currency: String): String =
    BigDecimal(minor, MoneyAmount.fractionDigits(currency)).toPlainString() + " " + currency

internal fun moneyInput(minor: Long, currency: String): String =
    MoneyAmount.format(minor, currency).replace('.', DecimalFormatSymbols.getInstance().decimalSeparator)

private val decimalInput = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")

internal fun parseMoney(value: String, currency: String): Long? {
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
    val normalized = value.trim().map { digit ->
        if (digit == separator) '.' else digit.digitToIntOrNull()?.digitToChar() ?: digit
    }.joinToString("")
    return normalized.takeIf(decimalInput::matches)?.let { MoneyAmount.parse(it, currency) }
}
