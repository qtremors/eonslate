package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.domain.finance.Attachment
import androidx.compose.material.icons.rounded.LockClock
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposits
import dev.qtremors.eonslate.core.tracker.domain.finance.MoneySummary
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.DonutLarge
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyBanks
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import dev.qtremors.eonslate.core.tracker.domain.finance.OverallFloor
import dev.qtremors.eonslate.core.tracker.domain.finance.Floors
import dev.qtremors.eonslate.core.tracker.domain.finance.Breach
import dev.qtremors.eonslate.core.tracker.domain.finance.own
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import androidx.compose.material.icons.rounded.HourglassTop
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
import dev.qtremors.eonslate.core.tracker.domain.finance.forPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import dev.qtremors.eonslate.core.ui.components.LocalAccountActions
import dev.qtremors.eonslate.core.ui.components.rememberFocusOnOpen
import dev.qtremors.eonslate.core.ui.components.WeekdayBars
import dev.qtremors.eonslate.core.tracker.domain.insights.Weekdays
import dev.qtremors.eonslate.core.ui.components.YearHeatMap
import dev.qtremors.eonslate.core.tracker.domain.insights.HeatLevels
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
/**
 * The money pages: the transaction list and recurring payments are tabs; accounts, budgets,
 * piggy banks and insights are pages of their own, opened from the list's top row.
 */
enum class FinanceSection { TRANSACTIONS, SUBSCRIPTIONS, ACCOUNTS, BUDGETS, PIGGY_BANKS, DEPOSITS, INSIGHTS }

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
    /** Shows only this account's transactions, from its card on Home. */
    openAccount: String? = null,
    onAccountOpened: () -> Unit = {},
    openNewAccount: Boolean = false,
    onNewAccountOpened: () -> Unit = {},
    /** Opens a piggy bank, from Home. */
    openPiggy: String? = null,
    onPiggyOpened: () -> Unit = {},
    /** Opens one person or business's chat, and everyone. */
    onParty: ((String) -> Unit)? = null,
    onPeople: () -> Unit = {},
    /** Opens one of the money pages. */
    onSection: (FinanceSection) -> Unit = {},
) {
    var newKind by rememberSaveable { mutableStateOf<TransactionKind?>(null) }
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val csv: FinanceCsvViewModel = hiltViewModel()
    val csvState by csv.state.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CsvFiles.MIME), csv::finishExport)
    val snackbar = remember { SnackbarHostState() }
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = Tracking.deviceCurrency())
    val overallFloor by viewModel.overallFloor.collectAsStateWithLifecycle(initialValue = null)
    val firstDay by viewModel.firstDay.collectAsStateWithLifecycle(initialValue = Tracking().firstDayOfWeek())
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    var editor by rememberSaveable { mutableStateOf<Editor?>(null) }
    var recordId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewingInbox by rememberSaveable { mutableStateOf(false) }
    val page = if (section == FinanceSection.SUBSCRIPTIONS) FinancePage.PLANS else FinancePage.ACTIVITY
    var query by rememberSaveable { mutableStateOf("") }
    val today = viewModel.today()
    var period by rememberSaveable { mutableStateOf(TransactionPeriod.MONTH) }
    var kindFilter by rememberSaveable { mutableStateOf<TransactionKind?>(null) }
    var customFrom by rememberSaveable { mutableStateOf<Long?>(null) }
    var customTo by rememberSaveable { mutableStateOf<Long?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var accountFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOnly by rememberSaveable { mutableStateOf(false) }
    var piggyAdding by rememberSaveable { mutableStateOf(true) }
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
    // A deleted account no longer filters anything.
    val accountShown = accountFilter?.takeIf { id -> ledger?.accounts?.any { it.id == id } != false }
    val filter = TransactionFilter(startDay..lastDay, kindFilter, category, query, sort, accountShown, pendingOnly = pendingOnly)
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
    LaunchedEffect(openAccount, ledger == null) {
        val data = ledger ?: return@LaunchedEffect
        // A deposit's card opens the deposit, not a list of its few transactions.
        if (openAccount != null && data.accounts.firstOrNull { it.id == openAccount }?.deposit != null) {
            open(Editor.DEPOSIT_VIEW, openAccount)
            onAccountOpened()
        } else if (openAccount != null) {
            accountFilter = openAccount
            category = null
            kindFilter = null
            period = TransactionPeriod.ALL
            query = ""
            onAccountOpened()
        }
    }
    LaunchedEffect(openPiggy) {
        if (openPiggy != null) {
            open(Editor.PIGGY_VIEW, openPiggy)
            onPiggyOpened()
        }
    }
    LaunchedEffect(openNewAccount) {
        if (openNewAccount) {
            open(Editor.ACCOUNT, null)
            onNewAccountOpened()
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
        title = stringResource(section.title()),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
        filter = if ((section == FinanceSection.TRANSACTIONS && ledger?.transactions?.isNotEmpty() == true) ||
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
            // Each page's rarer actions, and only its own.
            when (section) {
                FinanceSection.TRANSACTIONS, FinanceSection.SUBSCRIPTIONS -> FinanceCsvMenu(
                    ledger, csv, exportShown,
                    listOf(MenuAction(stringResource(R.string.finance_import_sms), Icons.Rounded.Sms) { open(Editor.SMS, null) }),
                    exportLabel = stringResource(if (page == FinancePage.ACTIVITY) R.string.finance_csv_export else R.string.finance_export_subscriptions),
                )
                FinanceSection.BUDGETS -> ledger?.takeIf { it.budgetHealth(today).isNotEmpty() }?.let { data ->
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                        csv.prepareExport(data) { ledger, out -> BudgetCsv.write(ledger, today, out) }
                        export.launch("eonslate-budgets.csv")
                    }) { Icon(Icons.Rounded.FileDownload, stringResource(R.string.finance_export_budgets)) }
                }
                else -> Unit
            }
            // The bell and profile on the two tabs; the pages under them have Back instead.
            val account = LocalAccountActions.current.takeIf { onBack == null }
            if (account != null) account() else if (onBack == null) onSettings?.let {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = it) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.finance_settings))
                }
            }
        },
        floatingActionButton = {
            ledger?.let { data ->
                val empty = data.accounts.isEmpty()
                val action = when {
                    empty -> Editor.ACCOUNT
                    else -> when (section) {
                        FinanceSection.TRANSACTIONS -> Editor.TRANSACTION
                        FinanceSection.SUBSCRIPTIONS -> Editor.RECURRING
                        FinanceSection.ACCOUNTS -> Editor.ACCOUNT
                        FinanceSection.BUDGETS -> Editor.BUDGET
                        FinanceSection.PIGGY_BANKS -> Editor.PIGGY
                        FinanceSection.DEPOSITS -> Editor.DEPOSIT
                        FinanceSection.INSIGHTS -> null
                    }
                } ?: return@let
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
                            Editor.FLOOR -> R.string.floor_overall_title
                            Editor.PIGGY, Editor.PIGGY_VIEW, Editor.PIGGY_MOVE -> R.string.piggy_new
                            Editor.DEPOSIT, Editor.DEPOSIT_VIEW, Editor.DEPOSIT_CLOSE -> R.string.deposit_new
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
                data, padding, page, today, query, shown, filter, period, section = section, onSection = onSection,
                overallFloor = overallFloor, onOverallFloor = { open(Editor.FLOOR, null) },
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
                onAccount = { accountFilter = it },
                onPending = { pendingOnly = it },
                onParty = onParty,
                onPeople = onPeople,
                onPiggy = { open(Editor.PIGGY_VIEW, it) },
                onNewPiggy = { open(Editor.PIGGY, null) },
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
                firstDay = firstDay,
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
            } && data.recurring.none { it.accountId == account.id } && data.budgets.none { it.accountId == account.id } &&
                data.piggyBanks.none { it.accountId == account.id }
            if (recordId == null || account != null) key(recordId) {
                AccountEditor(
                    account = account,
                    defaultCurrency = currency,
                    busy = busy,
                    failed = failed,
                    onDismiss = dismiss,
                    onSave = { name, code, opening, budget, kind, digits, floor ->
                        viewModel.save(Account(account?.id ?: viewModel.newId(), name, code, opening, budget, kind, digits, floor), done)
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
                    onSave = { account, kind, amount, day, note, destination, category, smsKey, details, party, pending, share, attached ->
                        viewModel.save(
                            Transaction(
                                transaction?.id ?: viewModel.newId(), account, kind, amount, day, note,
                                destination, category, smsKey, details, transaction?.createdAt ?: 0, pending = pending,
                            ),
                            party,
                            share,
                            attached,
                            finishReview,
                        )
                    },
                    parties = remember(data) { data.partiesByUse() },
                    initialAttachments = transaction?.let { data.attachmentsByTransaction[it.id] }.orEmpty(),
                    newId = viewModel::newId,
                    floorCheck = { candidate -> Floors.breaches(data, candidate, transaction, overallFloor) },
                    onDelete = transaction?.let { { viewModel.delete(it, done) } },
                )
            }
        }
        Editor.RECURRING -> {
            val existing = data.recurring.firstOrNull { it.id == recordId }
            if (data.accounts.isNotEmpty() && (recordId == null || existing != null)) key(recordId) {
                val original = remember { existing }
                RecurringEditor(
                    original, spendingAccounts(data), viewModel.today(), busy, failed, dismiss,
                    onSave = { payment, party, priceFrom ->
                        viewModel.schedule(original, payment.copy(id = original?.id ?: viewModel.newId()), party, priceFrom, done)
                    },
                    onDelete = existing?.let { { viewModel.delete(it, done) } },
                    onRecord = existing?.takeIf { !it.paused && it.nextEpochDay != null }?.let {
                        { open(Editor.DUE, it.id) }
                    },
                    parties = remember(data) { data.partiesByUse() },
                    initialParty = original?.partyId?.let { id -> data.parties.firstOrNull { it.id == id }?.name }.orEmpty(),
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
                    budget, spendingAccounts(data), viewModel.today(), busy, failed, dismiss,
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
        Editor.PIGGY -> {
            val piggy = data.piggyBanks.firstOrNull { it.id == recordId }
            if (data.accounts.isNotEmpty() && (recordId == null || piggy != null)) key(recordId) {
                PiggyEditor(
                    piggy, spendingAccounts(data), busy, failed, viewModel.today(), dismiss,
                    onSave = { viewModel.savePiggy(it, done) },
                    onDelete = piggy?.let { { viewModel.deletePiggy(it.id, done) } },
                    newId = viewModel::newId,
                )
            }
        }
        Editor.PIGGY_VIEW -> PiggyBanks.status(data).firstOrNull { it.piggy.id == recordId }?.let { status ->
            PiggySheet(
                status, data.accounts.firstOrNull { it.id == status.piggy.accountId }, dismiss,
                onMove = { adding ->
                    piggyAdding = adding
                    open(Editor.PIGGY_MOVE, status.piggy.id)
                },
                onEdit = { open(Editor.PIGGY, status.piggy.id) },
                onDeleteMove = viewModel::deletePiggyMove,
            )
        }
        Editor.PIGGY_MOVE -> PiggyBanks.status(data).firstOrNull { it.piggy.id == recordId }?.let { status ->
            val account = data.accounts.firstOrNull { it.id == status.piggy.accountId } ?: return@let
            val free = (data.balances()[account.id] ?: java.math.BigInteger.ZERO) - (PiggyBanks.setAside(data)[account.id] ?: java.math.BigInteger.ZERO)
            PiggyMoveSheet(
                status, account.currency, piggyAdding, free, busy, failed, viewModel.today(), dismiss,
            ) { minor, day -> viewModel.movePiggy(status.piggy.id, minor, day) { open(Editor.PIGGY_VIEW, status.piggy.id) } }
        }
        Editor.DEPOSIT -> {
            val account = data.accounts.firstOrNull { it.id == recordId && it.deposit != null }
            if (recordId == null || account != null) key(recordId) {
                DepositEditor(
                    account, spendingAccounts(data), currency, busy, failed, viewModel.today(), dismiss,
                    onSave = { deposit, from -> viewModel.saveDeposit(deposit, from) { open(Editor.DEPOSIT_VIEW, deposit.id) } },
                    onDelete = account?.let { { viewModel.deleteDeposit(it.id, done) } },
                    newId = viewModel::newId,
                )
            }
        }
        Editor.DEPOSIT_VIEW -> Deposits.status(data, viewModel.today().toEpochDay()).firstOrNull { it.account.id == recordId }?.let { status ->
            DepositSheet(
                status, dismiss,
                onClose = { open(Editor.DEPOSIT_CLOSE, status.account.id) },
                onEdit = { open(Editor.DEPOSIT, status.account.id) },
            )
        }
        Editor.DEPOSIT_CLOSE -> Deposits.status(data, viewModel.today().toEpochDay()).firstOrNull { it.account.id == recordId && it.open }?.let { status ->
            DepositCloseSheet(status, spendingAccounts(data), viewModel.today(), busy, failed, dismiss) { received, into, day, interest, charges ->
                viewModel.closeDeposit(status, received, into, day, interest, charges) { open(Editor.DEPOSIT_VIEW, status.account.id) }
            }
        }
        Editor.FLOOR -> OverallFloorSheet(overallFloor, currency, onDismiss = dismiss) { minor ->
            viewModel.setOverallFloor(minor)
            dismiss()
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

private enum class Editor {
    ACCOUNT, TRANSACTION, RECURRING, SUBSCRIPTION_DETAILS, SMS, BUDGET, DUE, INBOX, FLOOR, PIGGY, PIGGY_VIEW, PIGGY_MOVE,
    DEPOSIT, DEPOSIT_VIEW, DEPOSIT_CLOSE,
}

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
    section: FinanceSection = FinanceSection.TRANSACTIONS,
    onSection: (FinanceSection) -> Unit = {},
    overallFloor: OverallFloor? = null,
    onOverallFloor: () -> Unit = {},
    custom: LongRange?,
    onPeriod: (TransactionPeriod) -> Unit,
    onCustom: (Long, Long) -> Unit,
    onKind: (TransactionKind?) -> Unit,
    onSort: (TransactionSort) -> Unit,
    onCategory: (String?) -> Unit,
    open: (Editor, String?) -> Unit,
    onAccount: (String?) -> Unit = {},
    onPending: (Boolean) -> Unit = {},
    onParty: ((String) -> Unit)? = null,
    onPeople: () -> Unit = {},
    onPiggy: ((String) -> Unit)? = null,
    onNewPiggy: () -> Unit = {},
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
    firstDay: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
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
    val setAside = remember(ledger) { PiggyBanks.setAside(ledger) }
    val accounts = remember(ledger.accounts) { ledger.accounts.associateBy { it.id } }
    val parties = remember(ledger.parties) { ledger.partiesById }
    val attachedCount = remember(ledger.attachments) { ledger.attachments.groupingBy { it.transactionId }.eachCount() }
    // The account most payments use goes unsaid on rows; only the others are named.
    val usual = remember(ledger) { usualAccounts(ledger).firstOrNull()?.id.takeIf { ledger.accounts.size > 1 } }
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
        val list = section == FinanceSection.TRANSACTIONS
        val insights = section == FinanceSection.INSIGHTS
        // The list's top row: one tap to each money page, and to people and businesses.
        if (list) item(key = "links") {
            MoneyLinks(onSection, onPeople.takeIf { ledger.parties.isNotEmpty() })
        }
        if (section == FinanceSection.ACCOUNTS) {
            item(key = "overall-floor") {
                ConnectedGroup {
                    ConnectedRow(
                        title = stringResource(R.string.floor_overall_title),
                        supporting = overallFloor?.let { ValueFormat.money(it.minor, it.currency, numberLocale) } ?: stringResource(R.string.floor_none),
                        leading = { IconBadge(Icons.Rounded.VerticalAlignBottom, "overall-floor") },
                        onClick = onOverallFloor,
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.finance_accounts)) }
        }
        // Deposits have their own page.
        val everyday = ledger.accounts.filter { it.deposit == null }
        if (section == FinanceSection.ACCOUNTS) itemsIndexed(everyday, key = { _, account -> "account-" + account.id }) { index, account ->
            val aside = setAside[account.id]?.takeIf { it.signum() > 0 }
            val balance = ValueFormat.money(balances.getValue(account.id), account.currency, numberLocale) + (aside?.let {
                " · " + stringResource(R.string.piggy_aside_line, ValueFormat.money(it, account.currency, numberLocale))
            } ?: "")
            ConnectedItem(index, everyday.size) {
                ConnectedRow(
                    title = account.label,
                    supporting = balance,
                    leading = { IconBadge(account.kind.icon(), account.id) },
                    onClick = { open(Editor.ACCOUNT, account.id) },
                )
            }
        }
        if (section == FinanceSection.DEPOSITS) item(key = "deposits") {
            val deposits = remember(ledger, today) { Deposits.status(ledger, today.toEpochDay()) }
            if (deposits.isEmpty()) EmptyWithAction(stringResource(R.string.deposit_empty), stringResource(R.string.deposit_new)) { open(Editor.DEPOSIT, null) }
            else DepositSection(deposits) { open(Editor.DEPOSIT_VIEW, it) }
        }
        val piggies = PiggyBanks.status(ledger)
        if (section == FinanceSection.PIGGY_BANKS) item(key = "piggies") {
            if (piggies.isEmpty()) EmptyWithAction(stringResource(R.string.piggy_empty), stringResource(R.string.piggy_new), onNewPiggy)
            else if (onPiggy != null) PiggySection(piggies, accounts, onOpen = onPiggy, onNew = null)
        }
        if (section == FinanceSection.BUDGETS && budgetHealth.isEmpty()) item(key = "no-budgets") {
            EmptyWithAction(stringResource(R.string.finance_no_budgets), stringResource(R.string.finance_new_budget)) { open(Editor.BUDGET, null) }
        }
        // Budgets as meters for this period.
        if (section == FinanceSection.BUDGETS && budgetHealth.isNotEmpty()) {
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
        if (list && detection != null) item(key = "found-payments") { FoundPaymentsBanner(detection, onReviewPayments, Modifier.padding(vertical = 4.dp)) }
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
        if ((list || insights) && ledger.transactions.isNotEmpty()) {
            item(key = "periods") { PeriodChips(
                period, custom, filter.kind, sort, onSort, onPeriod, onCustom, onKind, ledger.accounts, filter.accountId, onAccount,
                listOnly = list,
                pendingOnly = filter.pendingOnly, onPending = onPending.takeIf { filter.pendingOnly || ledger.transactions.any { it.pending } },
            )
        }
            // Every category that suits the kind shown: used ones first, then the rest set up.
            val choices = (filter.kind?.let(::listOf) ?: listOf(TransactionKind.EXPENSE, TransactionKind.INCOME))
                .flatMap { Categories.suggestions(ledger, it) }
                .let { names -> if (category != null && names.none { it.equals(category, ignoreCase = true) }) listOf(category) + names else names }
                .distinctBy { it.lowercase() }
            if (list && choices.isNotEmpty()) item(key = "categories") {
                CategoryChips(choices, category) { picked -> onCategory(if (picked.equals(category, ignoreCase = true)) null else picked) }
            }
            if (list) item(key = "count") {
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
                // The list totals what it shows; Insights has its own ranges below.
                if (list) item(key = "totals") {
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
                if (insights && byCategory.isNotEmpty()) item(key = "breakdown") {
                    ChartCard(stringResource(R.string.overview_by_category)) {
                        CategoryBreakdown(
                            byCategory, slots, { ValueFormat.money(it, currency, numberLocale) }, category,
                            onPick = { picked -> onCategory(if (picked.equals(category, ignoreCase = true)) null else picked) },
                        )
                    }
                }
                // Which weekdays the shown spending falls on, for the range, currency and category shown.
                if (insights) item(key = "spending-weekday") {
                    val totals = remember(transactions, firstDay) {
                        Weekdays.totals(
                            transactions.filter { it.kind == TransactionKind.EXPENSE }
                                .groupBy { java.time.LocalDate.ofEpochDay(it.epochDay) }
                                .mapValues { (_, list) -> list.sumOf { t -> t.own.toDouble() } },
                            firstDay,
                        )
                    }
                    Weekdays.busiest(totals)?.let { busiest ->
                        val name = busiest.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
                        val peak = ValueFormat.money(java.math.BigInteger.valueOf(totals.maxOf { it.second }.toLong()), currency, numberLocale)
                        ChartCard(stringResource(R.string.finance_spending_by_weekday)) {
                            WeekdayBars(
                                totals = totals,
                                color = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(R.string.finance_busiest, name, peak),
                                peakLabel = peak,
                            )
                            Text(
                                stringResource(R.string.finance_busiest, name, peak),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                // A year of spending by day, in the shown currency, category and account; a tap shows that day.
                if (insights) item(key = "spending-heat") {
                    val since = today.minusYears(1).toEpochDay()
                    val account = filter.accountId
                    val levels = remember(ledger.transactions, currency, category, account, today) {
                        HeatLevels.of(
                            ledger.transactions.asSequence()
                                .filter { it.kind == TransactionKind.EXPENSE && it.epochDay >= since && accounts[it.accountId]?.currency == currency }
                                .filter { category == null || it.category.equals(category, ignoreCase = true) }
                                .filter { account == null || it.accountId == account }
                                .groupBy { java.time.LocalDate.ofEpochDay(it.epochDay) }
                                .mapValues { (_, list) -> list.sumOf { t -> t.own.toDouble() } },
                        )
                    }
                    if (levels.isNotEmpty()) ChartCard(stringResource(R.string.finance_spending_by_day)) {
                        YearHeatMap(
                            levels = levels,
                            today = today,
                            firstDay = firstDay,
                            color = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.finance_spending_by_day),
                            selected = custom?.takeIf { period == TransactionPeriod.CUSTOM && it.first == it.last }?.let { java.time.LocalDate.ofEpochDay(it.first) },
                            onDay = { day -> onCustom(day.toEpochDay(), day.toEpochDay()) },
                        )
                    }
                }
                // How money moved month by month, over a range of its own.
                if (insights) item(key = "cash-flow") {
                    MoneySummary.of(ledger, today, currency)?.takeIf { it.entries.isNotEmpty() }?.let { CashFlowCard(it, today) }
                }
            }
        }
        if (!list) return@LazyColumn
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
                // Out of date order, each row says its own day.
                val (title, details) = rowText(
                    transaction, parties, accounts, usual, if (byDate) null else dayLabel(transaction.epochDay, today),
                    attached = attachedCount[transaction.id] ?: 0,
                )
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
    accounts: List<Account> = emptyList(),
    accountId: String? = null,
    onAccount: (String?) -> Unit = {},
    pendingOnly: Boolean = false,
    /** Null hides the chip, as when nothing is pending. */
    onPending: ((Boolean) -> Unit)? = null,
    /** The list's own chips: how it sorts, which kind, and pending. Charts need only the period and account. */
    listOnly: Boolean = true,
) {
    var sorting by remember { mutableStateOf(false) }
    var choosingAccount by remember { mutableStateOf(false) }
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
        if (listOnly) Box {
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
        if (listOnly) Box {
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
        if (listOnly && onPending != null) FilterChip(
            selected = pendingOnly,
            onClick = { onPending(!pendingOnly) },
            label = { Text(stringResource(R.string.finance_pending)) },
            leadingIcon = { Icon(Icons.Rounded.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = MaterialTheme.shapes.medium,
            colors = selectedColors,
        )
        // With several accounts, one can be picked; its card on Home opens here with it picked.
        if (accounts.size > 1) Box {
            val chosen = accounts.firstOrNull { it.id == accountId }
            FilterChip(
                selected = chosen != null,
                onClick = { choosingAccount = true },
                label = { Text(chosen?.label ?: stringResource(R.string.finance_account_all)) },
                leadingIcon = chosen?.let { { Icon(it.kind.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) } },
                trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.medium,
                colors = selectedColors,
            )
            EonslateMenu(
                expanded = choosingAccount,
                onDismissRequest = { choosingAccount = false },
                groups = listOf(
                    listOf(MenuAction(stringResource(R.string.finance_account_all), selected = chosen == null) { onAccount(null) }) +
                        accounts.map { account -> MenuAction(account.label, account.kind.icon(), selected = account.id == accountId) { onAccount(account.id) } },
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
internal fun dayLabel(epochDay: Long, today: LocalDate): String {
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
internal fun usualAccounts(ledger: Ledger): List<Account> {
    val uses = ledger.transactions.groupingBy { it.accountId }.eachCount()
    // Deposits come last, closed ones after them, so they are never picked first for a payment.
    return ledger.accounts.sortedWith(compareBy<Account> { it.deposit != null }.thenBy { it.isClosed }.thenByDescending { uses[it.id] ?: 0 })
}

/** Accounts money is spent from: no deposits, for schedules, budgets and piggy banks. */
internal fun spendingAccounts(ledger: Ledger): List<Account> = usualAccounts(ledger).filter { it.deposit == null }

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
    onSave: (String, String, Long, Long?, AccountKind, String?, Long?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(account?.name.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(account?.kind ?: AccountKind.BANK) }
    var digits by rememberSaveable { mutableStateOf(account?.digits.orEmpty()) }
    var floor by rememberSaveable { mutableStateOf(account?.floorMinor?.let { moneyInput(it, account.currency) }.orEmpty()) }
    // Banks and cards have numbers payment messages name; wallets and cash do not.
    val numbered = kind == AccountKind.BANK || kind == AccountKind.CARD
    val lastDigits = digits.trim().takeIf { numbered && it.isNotEmpty() }
    val digitsValid = lastDigits == null || Account.DIGITS.matches(lastDigits)
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
    val floorMinor = if (floor.isBlank()) null else parseMoney(floor, currency)
    val floorValid = floor.isBlank() || floorMinor != null
    val entered = listOf<Any?>(name, code, opening, budget, kind, digits, floor)
    val opened = remember { entered }
    EditorSheet(title, busy, failed, onDismiss, dirty = entered != opened) {
        ConnectedChoices(AccountKind.EVERYDAY, kind, { it.label() }, { kind = it })
        FormField(name, { name = it }, stringResource(R.string.finance_name), maxLength = Account.NAME_LIMIT, enabled = !busy)
        if (numbered) FormField(
            digits, { next -> digits = next.filter(Char::isDigit) }, stringResource(R.string.finance_digits),
            supporting = stringResource(R.string.finance_digits_hint),
            error = if (digitsValid) null else stringResource(R.string.finance_digits_error),
            maxLength = 4, keyboardType = KeyboardType.NumberPassword, enabled = !busy,
        )
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
        FormField(
            floor, { floor = it }, stringResource(R.string.floor_keep),
            supporting = stringResource(R.string.floor_keep_hint),
            error = if (floorValid) null else stringResource(R.string.floor_invalid),
            maxLength = 24, keyboardType = KeyboardType.Ascii, enabled = !busy, suffix = currency.takeIf { known },
        )
        SaveRow(
            canSave = !busy && name.isNotBlank() && known && balance != null && (budget.isBlank() || monthly != null) && digitsValid && floorValid,
            onSave = { balance?.let { onSave(name.trim(), currency, it, monthly, kind, lastDigits, floorMinor) } },
            delete = onDelete?.let {
                Deletion(stringResource(R.string.finance_delete_account, account!!.name), R.string.finance_delete_account_message, it)
            },
            busy = busy,
        )
    }
}

@Composable
internal fun TransactionEditor(
    transaction: Transaction?,
    accounts: List<Account>,
    today: LocalDate,
    prefill: PaymentMessage?,
    initialKind: TransactionKind? = null,
    busy: Boolean,
    failed: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, TransactionKind, Long, Long, String, String?, String, String?, String, String, Boolean, ShareDraft, List<Attachment>) -> Unit,
    onDelete: (() -> Unit)?,
    /** People and businesses to pick from, the most recently paid first. */
    parties: List<Party> = emptyList(),
    /** What it already keeps: receipts and links. */
    initialAttachments: List<Attachment> = emptyList(),
    newId: () -> String = { java.util.UUID.randomUUID().toString() },
    /** For a new payment from someone's chat: them, how it is shared, and an amount to settle up. */
    initialParty: String = "",
    initialShare: ShareDraft? = null,
    initialMinor: Long? = null,
    /** The floors a payment would take a balance under. */
    floorCheck: (Transaction) -> List<Breach> = { emptyList() },
) {
    val suggest = LocalCategorySuggestions.current
    var kind by rememberSaveable { mutableStateOf(transaction?.kind ?: prefill?.kind ?: initialKind ?: TransactionKind.EXPENSE) }
    var accountId by rememberSaveable {
        val byEnding = prefill?.let { accounts.forPayment(it.currency, it.accountEnding) }
        mutableStateOf(transaction?.accountId ?: byEnding?.id ?: accounts.first().id)
    }
    var destinationId by rememberSaveable { mutableStateOf(transaction?.destinationId) }
    val account = accounts.firstOrNull { it.id == accountId } ?: accounts.first()
    val targets = accounts.filter { it.id != account.id && it.currency == account.currency }
    val destination = targets.firstOrNull { it.id == destinationId } ?: targets.firstOrNull()
    var amount by rememberSaveable {
        val initial = transaction?.minor ?: initialMinor ?: prefill?.takeIf { it.currency == account.currency }?.minor
        mutableStateOf(initial?.let { moneyInput(it, account.currency) }.orEmpty())
    }
    var date by rememberSaveable {
        val initial = transaction?.epochDay ?: prefill?.epochDay
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it).toString() } ?: today.toString())
    }
    val reference = prefill?.reference?.let { stringResource(R.string.finance_sms_reference, it) }
    val smsKey by rememberSaveable { mutableStateOf(transaction?.smsKey ?: prefill?.key) }
    // A found payment's payee becomes who it was with, so the note keeps only the reference.
    var party by rememberSaveable {
        mutableStateOf(transaction?.partyId?.let { id -> parties.firstOrNull { it.id == id }?.name } ?: prefill?.merchant ?: initialParty)
    }
    var share by remember { mutableStateOf(initialShare ?: transaction.shareDraft(parties.associateBy { it.id })) }
    var pending by rememberSaveable { mutableStateOf(transaction?.pending ?: false) }
    var note by rememberSaveable { mutableStateOf(transaction?.note ?: listOfNotNull(reference).joinToString(" · ")) }
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

    var attachments by remember { mutableStateOf(initialAttachments) }
    val entered = listOf<Any?>(kind, accountId, destinationId, amount, date, note, details, category, party, pending, share, attachments)
    // A share that does not suit the kind goes back to all yours.
    val shown = if (share.mode.fits(kind)) share else ShareDraft()
    val shareReady = !(shown.needsParty && party.isBlank()) && !(shown.mode == ShareMode.SPLIT && shown.splitWith.isEmpty())
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
            modifier = rememberFocusOnOpen(transaction == null && prefill == null),
        )
        if (!transfer) PartyField(party, { party = it }, kind, parties, !busy)
        DateField(date, { date = it }, stringResource(R.string.finance_date), busy)
        FormField(note, { note = it }, stringResource(R.string.finance_description), maxLength = Transaction.NOTE_LIMIT, enabled = !busy)
        if (!transfer) {
            CategoryPicker(category, { category = it }, kind, busy)
        }
        // A receipt or the product's page, kept with it; always in reach, since it is often why you open it.
        AttachmentsSection(attachments, { attachments = it }, !busy, newId)
        // Notes, people and pending wait behind More, so a plain payment stays short. They open on
        // their own when one is already set.
        var more by rememberSaveable { mutableStateOf(details.isNotBlank() || pending || shown.mode != ShareMode.MINE) }
        if (!more) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = { more = true }, enabled = !busy) {
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                Text(stringResource(if (transfer) R.string.finance_more_notes else R.string.finance_more), modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            FormField(
                details, { details = it }, stringResource(R.string.finance_notes),
                singleLine = false, minLines = 2, maxLength = Transaction.DETAILS_LIMIT, enabled = !busy,
            )
            if (!transfer) ShareEditor(shown, { share = it }, kind, party, parties, !busy)
            ConnectedGroup {
                ToggleRow(
                    title = stringResource(R.string.finance_pending),
                    supporting = stringResource(R.string.finance_pending_hint),
                    checked = pending,
                    onCheckedChange = { if (!busy) pending = it },
                    icon = Icons.Rounded.HourglassTop,
                )
            }
        }
        // Before going under a floor: say which and by how much, and let Save go ahead anyway.
        val breaches = if (parsed != null && day != null) {
            floorCheck(
                Transaction(
                    transaction?.id ?: "", account.id, kind, parsed, day,
                    destinationId = destination?.id?.takeIf { transfer },
                ),
            )
        } else emptyList()
        breaches.firstOrNull()?.let { breach ->
            val locale = LocalValueDisplay.current.numberLocale
            Text(
                breach.account?.let { owner -> stringResource(
                    R.string.floor_warn_account, owner.label,
                    ValueFormat.money(breach.under, breach.currency, locale), ValueFormat.money(breach.floor, breach.currency, locale),
                ) } ?: stringResource(
                    R.string.floor_warn_overall,
                    ValueFormat.money(breach.under, breach.currency, locale), ValueFormat.money(breach.floor, breach.currency, locale),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        SaveRow(
            label = if (breaches.isEmpty()) null else stringResource(R.string.floor_save_anyway),
            // A category is up to you; without one it counts as uncategorized.
            canSave = !busy && parsed != null && day != null && (if (transfer) destination != null else shareReady),
            onSave = {
                if (parsed != null && day != null) {
                    onSave(
                        account.id, kind, parsed, day, note.trim(), destination?.id?.takeIf { transfer },
                        if (transfer) "" else category.trim(), smsKey, details.trim(),
                        if (transfer) "" else party.trim(), pending, if (transfer) ShareDraft() else shown, attachments,
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
            title = selected.label,
            supporting = label,
            leading = { Icon(selected.kind.icon(), contentDescription = null) },
            trailing = { Icon(Icons.Rounded.ExpandMore, null) },
            enabled = enabled,
            onClick = { expanded = true },
        )
        EonslateMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            groups = listOf(accounts.map {
                MenuAction(it.label, it.kind.icon(), supporting = it.currency, selected = it.id == selected.id, onClick = { onPick(it) })
            }),
        )
    }
}

internal data class Deletion(val title: String, val message: Int, val confirm: () -> Unit)

@Composable
internal fun SaveRow(canSave: Boolean, onSave: () -> Unit, delete: Deletion?, busy: Boolean, label: String? = null) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
        if (delete != null) {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirm = true }, enabled = !busy) {
                Text(stringResource(R.string.finance_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Button(shapes = ButtonDefaults.shapes(), onClick = onSave, enabled = canSave) { Text(label ?: stringResource(R.string.finance_save)) }
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

/** The least to keep across every account in the app's currency; empty clears it. */
@Composable
private fun OverallFloorSheet(current: OverallFloor?, currency: String, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    var amount by rememberSaveable { mutableStateOf(current?.let { moneyInput(it.minor, it.currency) }.orEmpty()) }
    val minor = if (amount.isBlank()) null else parseMoney(amount, currency)
    val valid = amount.isBlank() || minor != null
    EditorSheet(stringResource(R.string.floor_overall_title), busy = false, failed = false, onDismiss = onDismiss) {
        Text(stringResource(R.string.floor_overall_hint, currency), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FormField(
            amount, { amount = it }, stringResource(R.string.floor_keep),
            error = if (valid) null else stringResource(R.string.floor_invalid),
            maxLength = 24, keyboardType = KeyboardType.Decimal, suffix = currency,
        )
        SaveRow(canSave = valid, onSave = { onSave(minor) }, delete = null, busy = false)
    }
}

private fun FinanceSection.title(): Int = when (this) {
    FinanceSection.TRANSACTIONS -> R.string.finance_activity
    FinanceSection.SUBSCRIPTIONS -> R.string.finance_subscriptions
    FinanceSection.ACCOUNTS -> R.string.finance_accounts
    FinanceSection.BUDGETS -> R.string.finance_budgets
    FinanceSection.PIGGY_BANKS -> R.string.piggy_title
    FinanceSection.DEPOSITS -> R.string.deposit_title
    FinanceSection.INSIGHTS -> R.string.money_insights
}

/** One row of ways into the money pages, above the list. */
@Composable
private fun MoneyLinks(onSection: (FinanceSection) -> Unit, onPeople: (() -> Unit)?) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val links = listOf(
            FinanceSection.ACCOUNTS to Icons.Rounded.AccountBalance,
            FinanceSection.BUDGETS to Icons.Rounded.DonutLarge,
            FinanceSection.PIGGY_BANKS to Icons.Rounded.Savings,
            FinanceSection.DEPOSITS to Icons.Rounded.LockClock,
            FinanceSection.INSIGHTS to Icons.Rounded.Insights,
        )
        links.forEach { (target, icon) ->
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onSection(target) }) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(target.title()), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (onPeople != null) FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onPeople) {
            Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.people_and_businesses), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
