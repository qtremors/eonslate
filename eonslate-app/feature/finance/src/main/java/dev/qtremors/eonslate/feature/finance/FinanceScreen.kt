package dev.qtremors.eonslate.feature.finance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.FileDownload
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvFiles
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import dev.qtremors.eonslate.core.ui.components.RankedBars
import dev.qtremors.eonslate.core.ui.components.RankedBar
import androidx.compose.ui.res.pluralStringResource
import dev.qtremors.eonslate.core.ui.components.BadgeColors
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.runtime.CompositionLocalProvider
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.ui.components.PressableCard
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.AccountBalance
import dev.qtremors.eonslate.core.tracker.domain.finance.monthlyMinor
import dev.qtremors.eonslate.core.ui.theme.HeroTextStyle
import dev.qtremors.eonslate.core.ui.components.Meter
import dev.qtremors.eonslate.core.ui.components.MonogramBadge
import dev.qtremors.eonslate.core.ui.theme.LocalSignals
import dev.qtremors.eonslate.core.ui.components.IconBadge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.Surface
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import dev.qtremors.eonslate.core.ui.theme.ValueTextStyle
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionSort
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
import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The two money tabs: everything logged (with budgets on top), and subscriptions. */
enum class FinanceSection { TRANSACTIONS, SUBSCRIPTIONS }

@Composable
fun FinanceScreen(
    section: FinanceSection,
    onBack: (() -> Unit)? = null,
    viewModel: FinanceViewModel = hiltViewModel(),
    sharedMessage: String? = null,
    onMessageOpened: () -> Unit = {},
    openInbox: Boolean = false,
    onInboxOpened: () -> Unit = {},
    /** Opens a new transaction of this kind, as Home's quick add asks. */
    openNew: TransactionKind? = null,
    onNewOpened: () -> Unit = {},
    onManageCategories: (() -> Unit)? = null,
) {
    var newKind by rememberSaveable { mutableStateOf<TransactionKind?>(null) }
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val csv: FinanceCsvViewModel = hiltViewModel()
    val csvState by csv.state.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CsvFiles.MIME), csv::finishExport)
    val snackbar = remember { SnackbarHostState() }
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = Tracking.deviceCurrency())
    val firstDay by viewModel.firstDay.collectAsStateWithLifecycle(initialValue = Tracking().firstDayOfWeek())
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var recordId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewingInbox by rememberSaveable { mutableStateOf(false) }
    val page = if (section == FinanceSection.SUBSCRIPTIONS) FinancePage.PLANS else FinancePage.ACTIVITY
    var query by rememberSaveable { mutableStateOf("") }
    val today = viewModel.today()
    var period by rememberSaveable { mutableStateOf(TransactionPeriod.ALL) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(TransactionSort.NEWEST) }
    val startDay = when (period) {
        TransactionPeriod.ALL -> Transaction.DATES.first
        TransactionPeriod.TODAY -> today.toEpochDay()
        TransactionPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(firstDay)).toEpochDay()
        TransactionPeriod.MONTH -> today.withDayOfMonth(1).toEpochDay()
    }
    val lastDay = if (period == TransactionPeriod.ALL) Transaction.DATES.last else today.toEpochDay()
    val shown = remember(ledger, startDay, lastDay, query, category, sort) {
        ledger?.filteredTransactions(startDay..lastDay, category, query, sort).orEmpty()
    }
    val exportTransactions: () -> Unit = {
        ledger?.let { data ->
            csv.prepareExport(if (page == FinancePage.ACTIVITY) data.copy(transactions = shown) else data)
            export.launch("eonslate-transactions.csv")
        }
    }
    var message by remember { mutableStateOf("") }
    var prefill by remember { mutableStateOf<PaymentMessage?>(null) }
    val done = {
        newKind = null
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

    LaunchedEffect(openNew) {
        if (openNew != null) {
            newKind = openNew
            open(Editor.TRANSACTION, null)
            onNewOpened()
        }
    }
    LaunchedEffect(openInbox) {
        if (openInbox) {
            open(Editor.INBOX, null)
            onInboxOpened()
        }
    }
    LaunchedEffect(sharedMessage, busy) {
        if (sharedMessage != null && !busy) {
            message = sharedMessage
            open(Editor.SMS, null)
            onMessageOpened()
        }
    }

    FinanceCsvFeedback(csv, snackbar)
    EonslateScaffold(
        title = stringResource(if (section == FinanceSection.SUBSCRIPTIONS) R.string.finance_subscriptions else R.string.finance_activity),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
        filter = if ((page == FinancePage.ACTIVITY && ledger?.transactions?.isNotEmpty() == true) ||
            (page == FinancePage.PLANS && ledger?.recurring?.isNotEmpty() == true)
        ) {
            FilterTop(
                query, { query = it },
                stringResource(if (page == FinancePage.PLANS) R.string.finance_search_subscriptions else R.string.finance_search),
                actions = {
                    if (page == FinancePage.ACTIVITY) {
                        IconButton(shapes = IconButtonDefaults.shapes(), enabled = !csvState.busy, onClick = exportTransactions) {
                            Icon(Icons.Rounded.FileDownload, stringResource(R.string.finance_csv_export_matches))
                        }
                    }
                },
            )
        } else null,
        actions = {
            // Rarer actions live in one menu; a second account is opt-in from here.
            val more = listOfNotNull(
                MenuAction(stringResource(R.string.finance_import_sms), Icons.Rounded.Sms) { open(Editor.SMS, null) },
                if (page == FinancePage.ACTIVITY) MenuAction(stringResource(R.string.finance_new_budget), Icons.Rounded.Savings) { open(Editor.BUDGET, null) } else null,
                onManageCategories?.let { MenuAction(stringResource(R.string.finance_manage_categories), financeIcon("Tag"), onClick = it) },
                MenuAction(stringResource(R.string.finance_new_account), Icons.Rounded.AccountBalance) { open(Editor.ACCOUNT, null) },
            )
            FinanceCsvMenu(ledger, csv, exportTransactions, more)
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
            data == null || data.accounts.isEmpty() -> {
                LaunchedEffect(Unit) { viewModel.ensureAccount() }
                LoadingState(Modifier.fillMaxSize().padding(padding))
            }
            else -> LedgerList(
                data, padding, page, today, query, shown, period, sort, category,
                onPeriod = { period = it },
                onSort = { sort = it },
                onCategory = { category = it },
                open = open,
                onDeleteMany = viewModel::deleteTransactions,
            )
        }
    }

    val data = ledger ?: return
    val suggest = remember(data) { { kind: TransactionKind -> Categories.suggestions(data, kind) } }
    CompositionLocalProvider(LocalCategorySuggestions provides suggest, LocalCategoryStyles provides data.categories) {
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
                    initialKind = newKind,
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
}

private enum class Editor { ACCOUNT, TRANSACTION, RECURRING, SMS, BUDGET, DUE, INBOX }

private enum class FinancePage { ACTIVITY, PLANS }

private enum class TransactionPeriod { ALL, TODAY, WEEK, MONTH }

@Composable
private fun LedgerList(
    ledger: Ledger,
    padding: androidx.compose.foundation.layout.PaddingValues,
    page: FinancePage,
    today: LocalDate,
    query: String,
    shown: List<Transaction>,
    period: TransactionPeriod,
    sort: TransactionSort,
    category: String?,
    onPeriod: (TransactionPeriod) -> Unit,
    onSort: (TransactionSort) -> Unit,
    onCategory: (String?) -> Unit,
    open: (Editor, String?) -> Unit,
    onDeleteMany: (Set<String>, () -> Unit) -> Unit = { _, _ -> },
) {
    // Long-press starts selecting; then a tap adds or removes, and the bar deletes them together.
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            icon = Icons.Rounded.Delete,
            title = pluralStringResource(R.plurals.finance_delete_selected, selected.size, selected.size),
            message = stringResource(R.string.finance_delete_transaction_message),
            confirmLabel = stringResource(R.string.finance_delete),
            onConfirm = {
                confirmDelete = false
                onDeleteMany(selected) { selected = emptySet() }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    val numberLocale = LocalValueDisplay.current.numberLocale
    val balances = remember(ledger) { ledger.balances() }
    val accounts = remember(ledger.accounts) { ledger.accounts.associateBy { it.id } }
    var monthOffset by rememberSaveable { mutableStateOf(0L) }
    val month = YearMonth.from(today).plusMonths(monthOffset)
    val spending = remember(ledger, month) { ledger.monthlySpending(month) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (page == FinancePage.PLANS) {
            val term = query.trim()
            val main = ledger.accounts.first().currency
            val ids = ledger.accounts.filter { it.currency == main }.mapTo(HashSet()) { it.id }
            val active = ledger.recurring.filter { !it.paused && it.accountId in ids }
            item(key = "commitments") {
                PlansHero(
                    out = active.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.monthlyMinor() },
                    into = active.filter { it.kind == TransactionKind.INCOME }.sumOf { it.monthlyMinor() },
                    currency = main,
                )
            }
            // What each subscription costs a month, largest first.
            val costs = active.filter { it.kind == TransactionKind.EXPENSE }.sortedByDescending { it.monthlyMinor() }
            if (costs.size > 1) item(key = "costs") {
                ChartCard(stringResource(R.string.finance_monthly_cost)) {
                    RankedBars(
                        costs.take(6).map { RankedBar(it.note.ifBlank { "?" }, ValueFormat.money(Math.round(it.monthlyMinor()), main, numberLocale), it.monthlyMinor().toFloat()) },
                        MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (ledger.recurring.isEmpty()) {
                item { Text(stringResource(R.string.finance_no_recurring), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
            }
            val ordered = ledger.recurring.sortedWith(compareBy({ it.paused }, { it.nextEpochDay ?: Long.MAX_VALUE })).filter {
                term.isBlank() || listOf(it.note, it.category, it.details).any { text -> text.contains(term, ignoreCase = true) }
            }
            itemsIndexed(ordered, key = { _, payment -> "recurring-" + payment.id }) { index, payment ->
                val account = accounts.getValue(payment.accountId)
                val name = payment.note.ifBlank { account.name }
                val due = payment.nextEpochDay
                val time = payment.nextOccurrence?.takeIf { payment.nextMinute != null }
                    ?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                val since = payment.startEpochDay?.let { stringResource(R.string.finance_since, LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))) }
                val whenText = listOfNotNull(
                    if (payment.paused) stringResource(R.string.finance_paused) else payment.interval.label(),
                    if (!payment.paused && due != null) dueLabel(due - today.toEpochDay()) else null,
                    time.takeUnless { payment.paused },
                    since,
                ).joinToString(" · ")
                ConnectedItem(index, ordered.size) {
                    ConnectedRow(
                        title = name,
                        supporting = whenText,
                        leading = { SubscriptionIcon(payment.icon, name) },
                        trailing = { Amount(payment.kind, ValueFormat.money(payment.minor, account.currency, numberLocale)) },
                        onClick = { open(Editor.RECURRING, payment.id) },
                    )
                }
            }
            return@LazyColumn
        }
        // One account is the whole balance; the list matters only once there are several.
        if (ledger.accounts.size > 1) item { SectionHeader(stringResource(R.string.finance_accounts)) }
        if (ledger.accounts.size > 1) itemsIndexed(ledger.accounts, key = { _, account -> "account-" + account.id }) { index, account ->
            val balance = ValueFormat.money(balances.getValue(account.id), account.currency, numberLocale)
            ConnectedItem(index, ledger.accounts.size) {
                ConnectedRow(
                    title = account.name,
                    supporting = balance,
                    onClick = { open(Editor.ACCOUNT, account.id) },
                )
            }
        }
        // Budgets lead the page, as meters for this month; there is no heading when there are none.
        if (ledger.budgets.isNotEmpty() || ledger.accounts.any { it.monthlyBudgetMinor != null }) {
            item { SectionHeader(stringResource(R.string.finance_budgets)) }
            // How the limits are shared out, when there is more than one.
            val limits = ledger.budgets.map { it.title to it.minor } + ledger.accounts.mapNotNull { a -> a.monthlyBudgetMinor?.let { a.name to it } }
            if (limits.size > 1) item(key = "allocation") {
                val currency = ledger.accounts.first().currency
                ChartCard(stringResource(R.string.finance_allocation)) {
                    RankedBars(
                        limits.sortedByDescending { it.second }.map { (name, minor) -> RankedBar(name, ValueFormat.money(minor, currency, numberLocale), minor.toFloat()) },
                        MaterialTheme.colorScheme.primary,
                    )
                }
            }
        items(ledger.accounts.filter { it.monthlyBudgetMinor != null }, key = { "budget-" + it.id }) { account ->
            BudgetCard(
                name = account.name,
                spent = spending.getOrDefault(account.id, BigInteger.ZERO),
                limit = BigInteger.valueOf(account.monthlyBudgetMinor!!),
                currency = account.currency,
                onClick = { open(Editor.ACCOUNT, account.id) },
            )
        }
        items(ledger.budgets, key = { "category-" + it.id }) { budget ->
            BudgetCard(
                name = budget.title,
                spent = remember(ledger, budget, today) { ledger.spendingFor(budget, today) },
                limit = BigInteger.valueOf(budget.minor),
                currency = accounts.getValue(budget.accountId).currency,
                budget = budget,
                onClick = { open(Editor.BUDGET, budget.id) },
            )
        }
        }
        if (selected.isNotEmpty()) item(key = "selection") {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pluralStringResource(R.plurals.finance_selected, selected.size, selected.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { selected = emptySet() }) { Text(stringResource(R.string.finance_cancel), color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    Button(shapes = ButtonDefaults.shapes(), onClick = { confirmDelete = true }) { Text(stringResource(R.string.finance_delete)) }
                }
            }
        }
        if (ledger.transactions.isNotEmpty()) {
            item(key = "periods") { PeriodChips(period, sort, onSort, onPeriod) }
            val used = ledger.transactions.map { it.category.trim() }.filter { it.isNotEmpty() }
                .groupingBy { it.lowercase() }.eachCount().entries.sortedByDescending { it.value }
                .mapNotNull { (key, _) -> ledger.transactions.firstOrNull { it.category.trim().lowercase() == key }?.category?.trim() }
            if (used.size > 1) item(key = "categories") {
                CategoryChips(used, category) { picked -> onCategory(if (picked.equals(category, ignoreCase = true)) null else picked) }
            }
            val summaries = shown.groupBy { accounts.getValue(it.accountId).currency }
            summaries.forEach { (currency, transactions) ->
                item(key = "totals-" + currency) {
                    val locale = numberLocale
                    val income = transactions.total(TransactionKind.INCOME)
                    val expenses = transactions.total(TransactionKind.EXPENSE)
                    Totals(
                        income = ValueFormat.money(income, currency, locale),
                        expenses = ValueFormat.money(expenses, currency, locale),
                        net = ValueFormat.money(income - expenses, currency, locale),
                    )
                }
                // Where the shown expenses went, following the period and category filters.
                val byCategory = transactions.filter { it.kind == TransactionKind.EXPENSE }
                    .groupBy { it.category.trim().ifBlank { "" } }
                    .map { (name, list) -> name to list.total(TransactionKind.EXPENSE) }
                    .sortedByDescending { it.second }
                if (byCategory.size > 1) item(key = "breakdown-" + currency) {
                    val none = stringResource(R.string.overview_uncategorized)
                    ChartCard(stringResource(R.string.overview_by_category)) {
                        RankedBars(
                            byCategory.take(6).map { (name, total) -> RankedBar(name.ifBlank { none }, ValueFormat.money(total, currency, numberLocale), total.toFloat()) },
                            MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item { Text(stringResource(if (ledger.transactions.isEmpty()) R.string.finance_empty_activity else R.string.finance_no_matches), modifier = Modifier.padding(12.dp)) }
        }
        // By size the order is the point, so one list; by date each day has its heading.
        val groups = if (sort == TransactionSort.LARGEST) mapOf(Long.MIN_VALUE to shown) else shown.groupBy { it.epochDay }
        groups.forEach { (epochDay, day) ->
            if (epochDay != Long.MIN_VALUE) item(key = "day-$epochDay") { SectionHeader(dayLabel(epochDay, today)) }
            itemsIndexed(day, key = { _, transaction -> "transaction-" + transaction.id }) { index, transaction ->
                val source = accounts.getValue(transaction.accountId)
                val target = transaction.destinationId?.let(accounts::getValue)
                val accountNames = if (target == null) source.name
                else stringResource(R.string.finance_transfer_accounts, source.name, target.name)
                // The note names it best, then the category, then the kind.
                val title = transaction.note.ifBlank { transaction.category.ifBlank { transaction.kind.label() } }
                val details = listOfNotNull(
                    transaction.category.takeIf { it.isNotBlank() && it != title },
                    accountNames.takeIf { target != null || ledger.accounts.size > 1 },
                ).joinToString(" · ").ifBlank { null }
                val picked = transaction.id in selected
                val toggle = { selected = if (picked) selected - transaction.id else selected + transaction.id }
                ConnectedItem(index, day.size) {
                    ConnectedRow(
                        title = title,
                        supporting = details,
                        containerColor = if (picked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceBright,
                        onLongClick = toggle,
                        leading = {
                            if (picked) {
                                IconBadge(icon = Icons.Rounded.Check, key = "selected", colors = BadgeColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary))
                            } else if (transaction.kind == TransactionKind.TRANSFER) {
                                IconBadge(icon = Icons.Rounded.SwapHoriz, key = "transfer")
                            } else {
                                val label = transaction.category.ifBlank { title }
                                val style = ledger.categories.firstOrNull { it.name.equals(transaction.category.trim(), ignoreCase = true) }
                                val icon = categoryIcon(label)
                                if (style != null) FinanceBadge(style.icon, style.color, label)
                                else if (icon != null) IconBadge(icon = icon, key = label.lowercase())
                                else MonogramBadge(label, label)
                            }
                        },
                        trailing = { Amount(transaction.kind, ValueFormat.money(transaction.minor, source.currency, numberLocale)) },
                        onClick = { if (selected.isNotEmpty()) toggle() else open(Editor.TRANSACTION, transaction.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    PressableCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

/** In, out and net for the shown period. In is green and out red, each with its sign too. */
@Composable
private fun Totals(income: String, expenses: String, net: String) {
    val colors = MaterialTheme.colorScheme
    val signals = LocalSignals.current
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.padding(20.dp)) {
            listOf(
                Triple(R.string.overview_in, "+$income", signals.positive),
                Triple(R.string.overview_out, "−$expenses", colors.error),
                Triple(R.string.overview_net, net, colors.onSurface),
            ).forEach { (label, value, color) ->
                Column(Modifier.weight(1f)) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(value, style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = color, maxLines = 1)
                }
            }
        }
    }
}

/** A signed amount: green coming in, red going out, plain for transfers. */
@Composable
internal fun Amount(kind: TransactionKind, amount: String) {
    val (text, color) = when (kind) {
        TransactionKind.INCOME -> "+$amount" to LocalSignals.current.positive
        TransactionKind.EXPENSE -> "−$amount" to MaterialTheme.colorScheme.error
        TransactionKind.TRANSFER -> amount to MaterialTheme.colorScheme.onSurface
    }
    Text(text, style = ValueTextStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = color)
}

@Composable
private fun PeriodChips(period: TransactionPeriod, sort: TransactionSort, onSort: (TransactionSort) -> Unit, onPick: (TransactionPeriod) -> Unit) {
    var sorting by remember { mutableStateOf(false) }
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            FilterChip(
                selected = false,
                onClick = { sorting = true },
                label = { Text(sort.label()) },
                leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.medium,
            )
            EonslateMenu(
                expanded = sorting,
                onDismissRequest = { sorting = false },
                groups = listOf(TransactionSort.entries.map { option -> MenuAction(option.label(), selected = option == sort) { onSort(option) } }),
            )
        }
        TransactionPeriod.entries.forEach {
            FilterChip(
                selected = it == period,
                onClick = { onPick(it) },
                label = {
                    Text(stringResource(when (it) {
                        TransactionPeriod.ALL -> R.string.finance_period_all
                        TransactionPeriod.TODAY -> R.string.finance_period_today
                        TransactionPeriod.WEEK -> R.string.finance_period_week
                        TransactionPeriod.MONTH -> R.string.finance_period_month
                    }))
                },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}


@Composable
private fun TransactionSort.label() = stringResource(
    when (this) {
        TransactionSort.NEWEST -> R.string.finance_sort_newest
        TransactionSort.OLDEST -> R.string.finance_sort_oldest
        TransactionSort.LARGEST -> R.string.finance_sort_largest
    }
)

/** One chip per category in use, most used first; picking one shows only it. */
@Composable
private fun CategoryChips(categories: List<String>, selected: String?, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { name ->
            FilterChip(
                selected = name.equals(selected, ignoreCase = true),
                onClick = { onPick(name) },
                label = { Text(name) },
                leadingIcon = categoryIcon(name)?.let { icon -> { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun dayLabel(epochDay: Long, today: LocalDate): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (date) {
        today -> stringResource(R.string.finance_period_today)
        today.minusDays(1) -> stringResource(R.string.finance_yesterday)
        else -> if (LocalValueDisplay.current.datePattern != null) displayDate(epochDay)
        else date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, d MMM" else "d MMM yyyy", Locale.getDefault()))
    }
}

/** What recurring payments cost in an average month: going out, and coming in when there is any. */
@Composable
private fun PlansHero(out: Double, into: Double, currency: String) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalValueDisplay.current.numberLocale
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.finance_per_month), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Text(ValueFormat.money(Math.round(out), currency, locale), style = HeroTextStyle.copy(fontSize = MetricTextStyle.fontSize * 1.4f), color = colors.onSurface, maxLines = 1)
            if (into > 0) {
                Text("+" + ValueFormat.money(Math.round(into), currency, locale) + " " + stringResource(R.string.finance_coming_in), style = MaterialTheme.typography.bodyMedium, color = LocalSignals.current.positive)
            }
        }
    }
}

/** One budget: its name, how much is spent of the limit, and a meter that turns red past it. */
@Composable
private fun BudgetCard(
    name: String, spent: BigInteger, limit: BigInteger, currency: String,
    budget: CategoryBudget? = null, onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val over = spent > limit
    val fraction = if (limit.signum() == 0) 0f else spent.toFloat() / limit.toFloat()
    val locale = LocalValueDisplay.current.numberLocale
    val spentText = ValueFormat.money(spent, currency, locale)
    val limitText = ValueFormat.money(limit, currency, locale)
    PressableCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceBadge(budget?.icon, budget?.color, name, budget?.category.orEmpty(), fallback = "Wallet")
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    budget?.category?.takeUnless { it == name }?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                }
                if (over) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.finance_over_budget), style = MaterialTheme.typography.labelLarge, color = colors.error)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(spentText, style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = colors.onSurface)
                Text(" / " + limitText, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            val progressColor = if (over) colors.error
                else budget?.color?.let { financeBadgeColors(it, name).content } ?: LocalSignals.current.positive
            Meter(fraction, progressColor, "$name $spentText / $limitText")
        }
    }
}

@Composable
private fun dueLabel(days: Long): String = when {
    days <= 0L -> stringResource(R.string.finance_due_today)
    days == 1L -> stringResource(R.string.finance_due_tomorrow)
    else -> stringResource(R.string.finance_due_in, days)
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
    initialKind: TransactionKind? = null,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, TransactionKind, Long, Long, String, String?, String, String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var kind by rememberSaveable { mutableStateOf(transaction?.kind ?: prefill?.kind ?: initialKind ?: TransactionKind.EXPENSE) }
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
        // Transfers and the account picker exist only once there is a second account.
        val kinds = if (accounts.size > 1) TransactionKind.entries else TransactionKind.entries - TransactionKind.TRANSFER
        ConnectedChoices(kinds, kind, { it.label() }, { if (!busy) kind = it })
        if (accounts.size > 1) AccountChoice(stringResource(if (transfer) R.string.finance_from else R.string.finance_account), account, accounts, !busy) {
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
        DateField(date, { date = it }, stringResource(R.string.finance_date), busy)
        FormField(note, { note = it }, stringResource(R.string.finance_note), maxLength = Transaction.NOTE_LIMIT, enabled = !busy)
        if (!transfer) {
            CategoryPicker(category, { category = it }, kind, busy)
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
    // With one account there is nothing to choose.
    if (accounts.size <= 1) return
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
        PaymentInterval.HOURLY -> R.string.finance_hourly
        PaymentInterval.DAILY -> R.string.finance_daily
        PaymentInterval.WEEKLY -> R.string.finance_weekly
        PaymentInterval.MONTHLY -> R.string.finance_monthly
        PaymentInterval.YEARLY -> R.string.finance_yearly
    }
)

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
