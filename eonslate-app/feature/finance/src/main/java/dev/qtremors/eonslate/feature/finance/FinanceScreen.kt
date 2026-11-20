package dev.qtremors.eonslate.feature.finance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.SwipeToDismissBoxValue
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionFilter
import dev.qtremors.eonslate.core.tracker.domain.finance.source
import dev.qtremors.eonslate.core.tracker.domain.finance.BudgetHealth
import dev.qtremors.eonslate.core.tracker.domain.csv.BudgetCsv
import dev.qtremors.eonslate.core.tracker.domain.csv.RecurringCsv
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.FileDownload
import dev.qtremors.eonslate.core.tracker.domain.csv.CsvFiles
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.ui.res.pluralStringResource
import dev.qtremors.eonslate.core.ui.components.FitText
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
    onSettings: (() -> Unit)? = null,
    /** Opens the new budget editor, as Home's empty budgets card asks. */
    openBudget: Boolean = false,
    onBudgetOpened: () -> Unit = {},
    /** Shows this month in one category, as a tap on the Home category chart asks. */
    openCategory: String? = null,
    onCategoryOpened: () -> Unit = {},
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
    var kindFilter by rememberSaveable { mutableStateOf<TransactionKind?>(null) }
    var customFrom by rememberSaveable { mutableStateOf<Long?>(null) }
    var customTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(TransactionSort.NEWEST) }
    val startDay = when (period) {
        TransactionPeriod.ALL -> Transaction.DATES.first
        TransactionPeriod.TODAY -> today.toEpochDay()
        TransactionPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(firstDay)).toEpochDay()
        TransactionPeriod.MONTH -> today.withDayOfMonth(1).toEpochDay()
        TransactionPeriod.DAYS_90 -> today.minusDays(89).toEpochDay()
        TransactionPeriod.CUSTOM -> customFrom ?: Transaction.DATES.first
    }
    val lastDay = when (period) {
        TransactionPeriod.ALL -> Transaction.DATES.last
        TransactionPeriod.CUSTOM -> customTo ?: Transaction.DATES.last
        else -> today.toEpochDay()
    }
    val filter = TransactionFilter(startDay..lastDay, kindFilter, category, query, sort)
    var planKind by rememberSaveable { mutableStateOf<TransactionKind?>(null) }
    var jumpToTop by remember { mutableStateOf(false) }
    val plans = remember(ledger, query, planKind) { ledger?.plans(query, planKind).orEmpty() }
    val shown = remember(ledger, filter) { ledger?.filteredTransactions(filter).orEmpty() }
    val scope = rememberCoroutineScope()
    val deletedText = stringResource(R.string.finance_transaction_deleted)
    val undoText = stringResource(R.string.finance_undo)
    // A swiped-away transaction can come back from the snackbar, with its id and save time.
    val deleteOne: (Transaction) -> Unit = { transaction ->
        viewModel.delete(transaction) {
            scope.launch {
                if (snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                    viewModel.restore(transaction)
                }
            }
        }
    }
    // Each page exports what it shows: the matching transactions, or the matching subscriptions.
    val exportShown: () -> Unit = {
        ledger?.let { data ->
            if (page == FinancePage.ACTIVITY) {
                csv.prepareExport(data.copy(transactions = shown))
                export.launch("eonslate-transactions.csv")
            } else {
                csv.prepareExport(data) { ledger, out -> RecurringCsv.write(ledger, plans, out) }
                export.launch("eonslate-subscriptions.csv")
            }
        }
    }
    var message by remember { mutableStateOf("") }
    var prefill by remember { mutableStateOf<PaymentMessage?>(null) }
    val done = {
        // A subscription editor may have left an unused icon file behind.
        if (editor == Editor.RECURRING || editor == Editor.SUBSCRIPTION_DETAILS) viewModel.pruneIcons()
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
    LaunchedEffect(openCategory) {
        if (openCategory != null) {
            category = openCategory
            kindFilter = null
            period = TransactionPeriod.MONTH
            query = ""
            onCategoryOpened()
        }
    }
    LaunchedEffect(openBudget) {
        if (openBudget) {
            jumpToTop = true
            open(Editor.BUDGET, null)
            onBudgetOpened()
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
                    IconButton(shapes = IconButtonDefaults.shapes(), enabled = !csvState.busy, onClick = exportShown) {
                        Icon(
                            Icons.Rounded.FileDownload,
                            stringResource(if (page == FinancePage.ACTIVITY) R.string.finance_csv_export_matches else R.string.finance_export_subscriptions),
                        )
                    }
                },
            )
        } else null,
        actions = {
            // Rarer actions live in one menu; a second account is opt-in from here.
            val more = listOfNotNull(
                MenuAction(stringResource(R.string.finance_import_sms), Icons.Rounded.Sms) { open(Editor.SMS, null) },
                if (page == FinancePage.ACTIVITY) MenuAction(stringResource(R.string.finance_new_budget), Icons.Rounded.Savings) { open(Editor.BUDGET, null) } else null,
                ledger?.takeIf { page == FinancePage.ACTIVITY && it.budgetHealth(today).isNotEmpty() }?.let { data ->
                    MenuAction(stringResource(R.string.finance_export_budgets), Icons.Rounded.FileDownload) {
                        csv.prepareExport(data) { ledger, out -> BudgetCsv.write(ledger, today, out) }
                        export.launch("eonslate-budgets.csv")
                    }
                },
                onManageCategories?.let { MenuAction(stringResource(R.string.finance_manage_categories), financeIcon("Tag"), onClick = it) },
                MenuAction(stringResource(R.string.finance_new_account), Icons.Rounded.AccountBalance) { open(Editor.ACCOUNT, null) },
            )
            onSettings?.let {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = it) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.finance_settings))
                }
            }
            FinanceCsvMenu(
                ledger, csv, exportShown, more,
                exportLabel = stringResource(if (page == FinancePage.ACTIVITY) R.string.finance_csv_export else R.string.finance_export_subscriptions),
            )
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
                            Editor.SUBSCRIPTION_DETAILS -> R.string.finance_subscription_history
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
                data, padding, page, today, query, shown, filter, period,
                custom = customFrom?.let { from -> customTo?.let { to -> from..to } },
                onPeriod = { period = it },
                onCustom = { from, to ->
                    customFrom = from
                    customTo = to
                    period = TransactionPeriod.CUSTOM
                },
                onKind = { picked ->
                    kindFilter = picked
                    // A category of the other kind would hide everything.
                    if (picked != null && category != null &&
                        Categories.suggestions(data, picked).none { it.equals(category, ignoreCase = true) }
                    ) category = null
                },
                onSort = { sort = it },
                onCategory = { category = it },
                open = open,
                onDeleteMany = viewModel::deleteTransactions,
                onDeleteOne = deleteOne,
                plans = plans,
                planKind = planKind,
                onPlanKind = { planKind = it },
                planActions = PlanActions(
                    open = { open(Editor.SUBSCRIPTION_DETAILS, it.id) },
                    edit = { open(Editor.RECURRING, it.id) },
                    setPaused = { payment, paused -> viewModel.setPaused(payment, paused) },
                    delete = { viewModel.delete(it) { viewModel.pruneIcons() } },
                    add = { open(Editor.RECURRING, null) },
                ),
                scrollToTop = jumpToTop,
                chosenCurrency = currency,
                onCurrency = viewModel::showCurrency,
                onScrolledToTop = { jumpToTop = false },
                detection = viewModel.detection,
                onReviewPayments = { open(Editor.INBOX, null) },
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
                    accounts = usualAccounts(data),
                    today = viewModel.today(),
                    prefill = prefill,
                    initialKind = newKind,
                    busy = busy,
                    failed = failed,
                    onDismiss = { if (!busy) finishReview() },
                    onSave = { account, kind, amount, day, note, destination, category, smsKey, details ->
                        viewModel.save(
                            Transaction(
                                transaction?.id ?: viewModel.newId(), account, kind, amount, day, note,
                                destination, category, smsKey, details, transaction?.createdAt ?: 0,
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
                    original, usualAccounts(data), viewModel.today(), busy, failed, dismiss,
                    onSave = { viewModel.schedule(original, it.copy(id = original?.id ?: viewModel.newId()), done) },
                    onDelete = existing?.let { { viewModel.delete(it, done) } },
                    onRecord = existing?.takeIf { !it.paused && it.nextEpochDay != null }?.let {
                        { open(Editor.DUE, it.id) }
                    },
                )
            }
        }
        Editor.SUBSCRIPTION_DETAILS -> {
            val payment = data.recurring.firstOrNull { it.id == recordId }
            val account = data.accounts.firstOrNull { it.id == payment?.accountId }
            if (payment != null && account != null) key(payment.id) {
                SubscriptionDetails(
                    payment, account, data, today,
                    onEdit = { open(Editor.RECURRING, payment.id) },
                    onDismiss = dismiss,
                )
            }
        }
        Editor.BUDGET -> {
            val budget = data.budgets.firstOrNull { it.id == recordId }
            if (data.accounts.isNotEmpty() && (recordId == null || budget != null)) key(recordId) {
                CategoryBudgetEditor(
                    budget, usualAccounts(data), viewModel.today(), busy, failed, dismiss,
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

private enum class Editor { ACCOUNT, TRANSACTION, RECURRING, SUBSCRIPTION_DETAILS, SMS, BUDGET, DUE, INBOX }

private enum class FinancePage { ACTIVITY, PLANS }

private enum class TransactionPeriod { ALL, TODAY, WEEK, MONTH, DAYS_90, CUSTOM }

/** Rows shown before "Show older"; each tap shows this many more. */
private const val PAGE = 50


@Composable
private fun LedgerList(
    ledger: Ledger,
    padding: androidx.compose.foundation.layout.PaddingValues,
    page: FinancePage,
    today: LocalDate,
    query: String,
    shown: List<Transaction>,
    filter: TransactionFilter,
    period: TransactionPeriod,
    custom: LongRange?,
    onPeriod: (TransactionPeriod) -> Unit,
    onCustom: (Long, Long) -> Unit,
    onKind: (TransactionKind?) -> Unit,
    onSort: (TransactionSort) -> Unit,
    onCategory: (String?) -> Unit,
    open: (Editor, String?) -> Unit,
    onDeleteMany: (Set<String>, () -> Unit) -> Unit = { _, _ -> },
    onDeleteOne: (Transaction) -> Unit = {},
    plans: List<RecurringPayment> = emptyList(),
    planKind: TransactionKind? = null,
    onPlanKind: (TransactionKind?) -> Unit = {},
    planActions: PlanActions = PlanActions({}, {}, { _, _ -> }, {}),
    detection: SmsDetection? = null,
    onReviewPayments: () -> Unit = {},
    scrollToTop: Boolean = false,
    onScrolledToTop: () -> Unit = {},
    chosenCurrency: String = "",
    onCurrency: (String) -> Unit = {},
) {
    val sort = filter.sort
    val category = filter.category
    // Long-press starts selecting; then a tap adds or removes, and the bar deletes them together.
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    // Only what is on screen stays selected, so a filter change never deletes hidden rows.
    val shownIds = remember(shown) { shown.mapTo(HashSet()) { it.id } }
    LaunchedEffect(shownIds) { if (!shownIds.containsAll(selected)) selected = selected intersect shownIds }
    var limit by rememberSaveable(filter) { mutableStateOf(PAGE) }
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
    val budgetHealth = remember(ledger, today) { ledger.budgetHealth(today) }
    val slots = remember(ledger) { chartSlots(ledger) }
    val listState = rememberLazyListState()
    // Budgets lead the page, so opening one from Home brings the page back to its top.
    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            listState.scrollToItem(0)
            onScrolledToTop()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (page == FinancePage.PLANS) {
            planItems(ledger, plans, planKind, onPlanKind, today, planActions, chosenCurrency, onCurrency)
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
        if (budgetHealth.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.finance_budgets)) }
            val openBudget = { health: BudgetHealth ->
                health.budget?.let { open(Editor.BUDGET, it.id) } ?: health.account?.let { open(Editor.ACCOUNT, it.id) }
            }
            // What each budget has spent this period, one chart per currency so amounts compare.
            // All budgets in a currency at once: the total limit, spent and left, with a ring.
            val budgetCurrency = ledger.shownCurrency(chosenCurrency)
            budgetHealth.filter { it.currency == budgetCurrency }.takeIf { it.isNotEmpty() }?.let { group ->
                item(key = "budget-totals") { BudgetTotals(group, budgetCurrency) }
            }
            items(budgetHealth, key = { "budget-" + (it.budget?.id ?: "account-" + it.account?.id) }) { health ->
                BudgetCard(health, onClick = { openBudget(health) })
            }
        }
        if (detection != null) item(key = "found-payments") { FoundPaymentsBanner(detection, onReviewPayments, Modifier.padding(vertical = 4.dp)) }
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
            item(key = "periods") { PeriodChips(period, custom, filter.kind, sort, onSort, onPeriod, onCustom, onKind) }
            // Every category that suits the kind shown: used ones first, then the rest set up.
            val choices = (filter.kind?.let(::listOf) ?: listOf(TransactionKind.EXPENSE, TransactionKind.INCOME))
                .flatMap { Categories.suggestions(ledger, it) }
                .let { names -> if (category != null && names.none { it.equals(category, ignoreCase = true) }) listOf(category) + names else names }
                .distinctBy { it.lowercase() }
            if (choices.isNotEmpty()) item(key = "categories") {
                CategoryChips(choices, category) { picked -> onCategory(if (picked.equals(category, ignoreCase = true)) null else picked) }
            }
            item(key = "count") {
                Text(
                    pluralStringResource(R.plurals.finance_matching, shown.size, shown.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
            // One currency at a time, the same one on every tab; the rows below still list all.
            val currency = ledger.shownCurrency(chosenCurrency)
            val transactions = shown.filter { accounts.getValue(it.accountId).currency == currency }
            run {
                item(key = "totals") {
                    val locale = numberLocale
                    val income = transactions.total(TransactionKind.INCOME)
                    val expenses = transactions.total(TransactionKind.EXPENSE)
                    Totals(
                        income = ValueFormat.money(income, currency, locale),
                        expenses = ValueFormat.money(expenses, currency, locale),
                        net = ValueFormat.money(income - expenses, currency, locale),
                        badge = if (ledger.currencies().size > 1) ({ CurrencyPicker(currency, ledger.currencies(), onCurrency) }) else null,
                    )
                }
                // Where the shown expenses went, following the period and category filters.
                val byCategory = transactions.filter { it.kind == TransactionKind.EXPENSE }
                    .groupBy { it.category.trim().ifBlank { "" } }
                    .map { (name, list) -> name to list.total(TransactionKind.EXPENSE) }
                    .sortedByDescending { it.second }
                if (byCategory.isNotEmpty()) item(key = "breakdown") {
                    ChartCard(stringResource(R.string.overview_by_category)) {
                        CategoryBreakdown(
                            byCategory, slots, { ValueFormat.money(it, currency, numberLocale) }, category,
                            onPick = { picked -> onCategory(if (picked.equals(category, ignoreCase = true)) null else picked) },
                        )
                    }
                }
            }
        }
        if (shown.isEmpty()) item(key = "empty") {
            EmptyWithAction(
                stringResource(if (ledger.transactions.isEmpty()) R.string.finance_empty_activity else R.string.finance_no_matches),
                stringResource(R.string.finance_new_transaction),
            ) { open(Editor.TRANSACTION, null) }
        }
        // By size the order is the point, so one list; by date each day has its heading.
        val page = shown.take(limit)
        val byDate = sort == TransactionSort.NEWEST || sort == TransactionSort.OLDEST
        val groups = if (byDate) page.groupBy { it.epochDay } else mapOf(Long.MIN_VALUE to page)
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
                    // Out of date order, each row says its own day.
                    if (byDate) null else dayLabel(transaction.epochDay, today),
                    transaction.details.lineSequence().firstOrNull { it.isNotBlank() }?.trim(),
                ).joinToString(" · ").ifBlank { null }
                val picked = transaction.id in selected
                val toggle = { selected = if (picked) selected - transaction.id else selected + transaction.id }
                ConnectedItem(index, day.size) {
                    SwipeToDelete(enabled = selected.isEmpty(), onDelete = { onDeleteOne(transaction) }) {
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
        if (shown.size > limit) item(key = "older") {
            FilledTonalButton(
                shapes = ButtonDefaults.shapes(),
                onClick = { limit += PAGE },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) { Text(stringResource(R.string.finance_show_older, shown.size - limit)) }
        }
    }
}


/** Swiping a row toward the start deletes it; the caller offers Undo. */
@Composable
private fun SwipeToDelete(enabled: Boolean, onDelete: () -> Unit, content: @Composable () -> Unit) {
    // Not saved: a row brought back by Undo has the same key, and a saved "swiped away" state
    // would dismiss it again the moment it returns.
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    val colors = MaterialTheme.colorScheme
    // The box calls back from an effect keyed on the callback itself, so a new lambda each
    // recomposition would delete again and again. One stable callback deletes exactly once.
    val currentDelete by rememberUpdatedState(onDelete)
    val dismiss = remember {
        var done = false
        { _: SwipeToDismissBoxValue -> if (!done) { done = true; currentDelete() } }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = enabled,
        onDismiss = dismiss,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(colors.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.finance_delete), tint = colors.onErrorContainer)
            }
        },
    ) { content() }
}

@Composable
internal fun ChartCard(title: String, content: @Composable () -> Unit) {
    PressableCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

/** In, out and net for the shown period. In is green and out red, each with its sign too. */
@Composable
internal fun Totals(
    income: String,
    expenses: String,
    net: String,
    title: String? = null,
    /** Beside the title, such as the shown currency. */
    badge: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val signals = LocalSignals.current
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null || badge != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                title?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
                badge?.invoke()
            }
            Row {
                listOf(
                    Triple(R.string.overview_in, "+$income", signals.positive),
                    Triple(R.string.overview_out, "−$expenses", colors.error),
                    Triple(R.string.overview_net, net, colors.onSurface),
                ).forEach { (label, value, color) ->
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        FitText(value, style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = color)
                    }
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
private fun PeriodChips(
    period: TransactionPeriod,
    custom: LongRange?,
    kind: TransactionKind?,
    sort: TransactionSort,
    onSort: (TransactionSort) -> Unit,
    onPick: (TransactionPeriod) -> Unit,
    onCustom: (Long, Long) -> Unit,
    onKind: (TransactionKind?) -> Unit,
) {
    var sorting by remember { mutableStateOf(false) }
    var kinds by remember { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val selectedColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
    )
    if (picking) DateRangeDialog(custom, onDismiss = { picking = false }) { from, to ->
        picking = false
        onCustom(from, to)
    }
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
        Box {
            FilterChip(
                selected = kind != null,
                onClick = { kinds = true },
                label = { Text(kind?.label() ?: stringResource(R.string.finance_kind_all)) },
                trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.medium,
                colors = selectedColors,
            )
            EonslateMenu(
                expanded = kinds,
                onDismissRequest = { kinds = false },
                groups = listOf(
                    listOf(null, TransactionKind.EXPENSE, TransactionKind.INCOME).map { option ->
                        MenuAction(option?.label() ?: stringResource(R.string.finance_kind_all), selected = option == kind) { onKind(option) }
                    },
                ),
            )
        }
        TransactionPeriod.entries.forEach {
            FilterChip(
                selected = it == period,
                onClick = { if (it == TransactionPeriod.CUSTOM) picking = true else onPick(it) },
                label = {
                    Text(
                        if (it == TransactionPeriod.CUSTOM && custom != null) {
                            displayDate(custom.first) + " – " + displayDate(custom.last)
                        } else stringResource(
                            when (it) {
                                TransactionPeriod.ALL -> R.string.finance_period_all
                                TransactionPeriod.TODAY -> R.string.finance_period_today
                                TransactionPeriod.WEEK -> R.string.finance_period_week
                                TransactionPeriod.MONTH -> R.string.finance_period_month
                                TransactionPeriod.DAYS_90 -> R.string.finance_period_90
                                TransactionPeriod.CUSTOM -> R.string.finance_period_custom
                            }
                        )
                    )
                },
                leadingIcon = if (it == TransactionPeriod.CUSTOM) {
                    { Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                shape = MaterialTheme.shapes.medium,
                colors = selectedColors,
            )
        }
    }
}

/** Picks the From and To days of the custom period; one day alone is a one-day period. */
@Composable
private fun DateRangeDialog(initial: LongRange?, onDismiss: () -> Unit, onPick: (Long, Long) -> Unit) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.first?.times(DAY_MS),
        initialSelectedEndDateMillis = initial?.last?.times(DAY_MS),
    )
    val from = state.selectedStartDateMillis?.let { Math.floorDiv(it, DAY_MS) }
    val to = state.selectedEndDateMillis?.let { Math.floorDiv(it, DAY_MS) } ?: from
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(shapes = ButtonDefaults.shapes(), enabled = from != null, onClick = { if (from != null && to != null) onPick(from, to) }) {
                Text(stringResource(R.string.finance_apply))
            }
        },
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text(stringResource(R.string.finance_cancel)) }
        },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f).padding(top = 12.dp))
    }
}

private const val DAY_MS = 86_400_000L


@Composable
private fun TransactionSort.label() = stringResource(
    when (this) {
        TransactionSort.NEWEST -> R.string.finance_sort_newest
        TransactionSort.OLDEST -> R.string.finance_sort_oldest
        TransactionSort.LARGEST -> R.string.finance_sort_largest
        TransactionSort.SMALLEST -> R.string.finance_sort_smallest
        TransactionSort.CATEGORY -> R.string.finance_sort_category
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

/**
 * One budget: its name and how it renews, spent of the limit, what is left (or over) and the
 * share used, with a meter that turns amber near the limit and red past it.
 */
@Composable
private fun BudgetCard(health: BudgetHealth, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val budget = health.budget
    val name = health.title
    val locale = LocalValueDisplay.current.numberLocale
    val money = { minor: BigInteger -> ValueFormat.money(minor, health.currency, locale) }
    val spentText = money(health.spent)
    val limitText = money(health.limit)
    val renews = budget?.let { intervalLabel(it.interval, it.every) } ?: PaymentInterval.MONTHLY.label()
    val about = listOfNotNull(
        budget?.category?.takeUnless { it == name },
        renews,
        if (budget?.allAccounts == true) stringResource(R.string.finance_all_accounts) else null,
    ).joinToString(" · ")
    PressableCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceBadge(budget?.icon, budget?.color, name, budget?.category.orEmpty(), fallback = "Wallet")
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(about, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
                if (health.over) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.finance_over_budget), style = MaterialTheme.typography.labelLarge, color = colors.error)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(spentText, style = MetricTextStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize), color = colors.onSurface)
                Text(" / " + limitText, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    if (health.over) stringResource(R.string.finance_budget_over_by, money(health.remaining.negate()), budgetPercent(health.percent))
                    else stringResource(R.string.finance_budget_left, money(health.remaining), budgetPercent(health.percent)),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        health.over -> colors.error
                        health.warning -> LocalSignals.current.warning
                        else -> colors.onSurfaceVariant
                    },
                )
            }
            val progressColor = when {
                health.over -> colors.error
                health.warning -> LocalSignals.current.warning
                else -> budget?.color?.let { financeBadgeColors(it, name).content } ?: LocalSignals.current.positive
            }
            Meter(health.fraction, progressColor, "$name $spentText / $limitText")
        }
    }
}

/** How often something renews, as people say it: "Monthly", or "Every 3 months". */
@Composable
internal fun intervalLabel(interval: PaymentInterval, every: Int): String =
    if (every <= 1) interval.label()
    else pluralStringResource(
        when (interval) {
            PaymentInterval.HOURLY -> R.plurals.finance_every_hours
            PaymentInterval.DAILY -> R.plurals.finance_every_days
            PaymentInterval.WEEKLY -> R.plurals.finance_every_weeks
            PaymentInterval.MONTHLY -> R.plurals.finance_every_months
            PaymentInterval.YEARLY -> R.plurals.finance_every_years
        },
        every, every,
    )

/** Accounts most used first, so a new entry starts in the account you use, not the first by id. */
private fun usualAccounts(ledger: Ledger): List<Account> {
    val uses = ledger.transactions.groupingBy { it.accountId }.eachCount()
    return ledger.accounts.sortedByDescending { uses[it.id] ?: 0 }
}

@Composable
internal fun dueLabel(days: Long): String = when {
    days < 0L -> pluralStringResource(R.plurals.finance_overdue, (-days).toInt(), (-days).toInt())
    days == 0L -> stringResource(R.string.finance_due_today)
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
    val entered = listOf<Any?>(name, code, opening, budget)
    val opened = remember { entered }
    EditorSheet(title, busy, failed, onDismiss, dirty = entered != opened) {
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
    onSave: (String, TransactionKind, Long, Long, String, String?, String, String?, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val suggest = LocalCategorySuggestions.current
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
    // A reviewed payment keeps where it came from in its notes.
    var details by rememberSaveable { mutableStateOf(transaction?.details ?: prefill?.source().orEmpty()) }
    // A new transaction starts in its kind's most used category, and follows the kind until changed.
    var autoCategory by rememberSaveable {
        mutableStateOf(if (transaction == null && prefill?.category.isNullOrBlank()) suggest(kind).firstOrNull() else null)
    }
    var category by rememberSaveable { mutableStateOf(transaction?.category ?: prefill?.category ?: autoCategory.orEmpty()) }
    LaunchedEffect(kind) {
        if (autoCategory != null && category == autoCategory) {
            autoCategory = suggest(kind).firstOrNull()
            category = autoCategory.orEmpty()
        }
    }
    val parsed = parseMoney(amount, account.currency)?.takeIf { it > 0 }
    val day = EpochDays.parse(date)?.takeIf { it in Transaction.DATES }
    val transfer = kind == TransactionKind.TRANSFER
    val title = stringResource(if (transaction == null) R.string.finance_new_transaction else R.string.finance_edit_transaction)

    val entered = listOf<Any?>(kind, accountId, destinationId, amount, date, note, details, category)
    val opened = remember { entered }
    EditorSheet(title, busy, failed, onDismiss, dirty = entered != opened) {
        if (prefill != null) {
            val payee = prefill.merchant ?: prefill.kind.label()
            PressableCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonogramBadge(payee, payee)
                    Column(Modifier.weight(1f)) {
                        Text(payee, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        listOfNotNull(prefill.issuer?.takeUnless { it == payee }, reference).joinToString(" · ")
                            .takeIf(String::isNotBlank)?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                    }
                    Amount(prefill.kind, ValueFormat.money(prefill.minor, prefill.currency, LocalValueDisplay.current.numberLocale))
                }
            }
        }
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
        FormField(note, { note = it }, stringResource(R.string.finance_description), maxLength = Transaction.NOTE_LIMIT, enabled = !busy)
        if (!transfer) {
            CategoryPicker(category, { category = it }, kind, busy)
        }
        FormField(
            details, { details = it }, stringResource(R.string.finance_notes),
            singleLine = false, minLines = 2, maxLength = Transaction.DETAILS_LIMIT, enabled = !busy,
        )
        SaveRow(
            // Income and expenses need a category, so every one is counted somewhere.
            canSave = !busy && parsed != null && day != null && (if (transfer) destination != null else category.isNotBlank()),
            onSave = {
                if (parsed != null && day != null) {
                    onSave(
                        account.id, kind, parsed, day, note.trim(), destination?.id?.takeIf { transfer },
                        if (transfer) "" else category.trim(), smsKey, details.trim(),
                    )
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
    /** Something was entered: closing by swipe, Back or a tap outside asks before it is lost. */
    dirty: Boolean = false,
    content: @Composable () -> Unit,
) {
    var asking by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val unsaved by rememberUpdatedState(dirty)
    val state = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (value == androidx.compose.material3.SheetValue.Hidden && unsaved && !leaving) {
                asking = true
                false
            } else true
        },
    )
    if (asking) {
        ConfirmDialog(
            icon = Icons.Rounded.Delete,
            title = stringResource(R.string.finance_discard_title),
            message = stringResource(R.string.finance_discard_message),
            confirmLabel = stringResource(R.string.finance_discard),
            onConfirm = {
                asking = false
                leaving = true
                onDismiss()
            },
            onDismiss = { asking = false },
        )
    }
    EonslateBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, title = title, sheetState = state) {
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

/** Nothing to show yet: a line saying so, and the button that adds the first one. */
@Composable
internal fun EmptyWithAction(message: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onAction) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(action, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
