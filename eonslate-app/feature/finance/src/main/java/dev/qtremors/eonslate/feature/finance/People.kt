package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.paidThisYear
import dev.qtremors.eonslate.core.tracker.domain.finance.totalsWith
import dev.qtremors.eonslate.core.tracker.domain.finance.PartyTotals
import dev.qtremors.eonslate.core.tracker.domain.finance.Floors
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
import dev.qtremors.eonslate.core.tracker.domain.finance.PartyKind
import dev.qtremors.eonslate.core.tracker.domain.finance.ShareKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.finance.owed
import dev.qtremors.eonslate.core.tracker.domain.finance.shareKind
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.Avatar
import dev.qtremors.eonslate.core.ui.components.ConfirmDialog
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateMenu
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FilterTop
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.MenuAction
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.theme.LocalSignals
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import java.math.BigInteger
import java.time.LocalDate

/**
 * Each person or business: their last day with you, what you paid them this year, and where you
 * stand with them, each by currency.
 */
internal data class PersonLine(val party: Party, val last: Long?, val owed: Map<String, BigInteger>, val paid: Map<String, BigInteger> = emptyMap())

/** Everyone you deal with, who you paid most this year first, then the most recent. */
internal fun Ledger.people(today: LocalDate = LocalDate.now()): List<PersonLine> {
    val owed = owed()
    val paid = paidThisYear(today)
    val last = HashMap<String, Long>()
    for (t in transactions) {
        val ids = listOfNotNull(t.partyId) + t.shares.map { it.partyId }
        for (id in ids) if (t.epochDay > (last[id] ?: Long.MIN_VALUE)) last[id] = t.epochDay
    }
    // Amounts in different currencies do not compare; the most paid in any one decides.
    val most = { line: PersonLine -> line.paid.values.maxOrNull() ?: BigInteger.ZERO }
    return parties.map { PersonLine(it, last[it.id], owed[it.id].orEmpty(), paid[it.id].orEmpty()) }
        .sortedWith(compareByDescending(most).thenByDescending { it.last ?: Long.MIN_VALUE }.thenBy { it.party.key })
}

/** "₹2,328 this year", in each currency paid. */
@Composable
internal fun paidLine(paid: Map<String, BigInteger>): String? {
    if (paid.isEmpty()) return null
    val locale = LocalValueDisplay.current.numberLocale
    return stringResource(R.string.people_paid_year, paid.entries.joinToString(" + ") { (currency, minor) -> ValueFormat.money(minor, currency, locale) })
}

/** Every transaction with [partyId]: paid to or by them, or shared with them. */
internal fun Ledger.withParty(partyId: String): List<Transaction> =
    transactions.filter { t -> t.partyId == partyId || t.shares.any { it.partyId == partyId } }
        .sortedWith(compareBy<Transaction> { it.epochDay }.thenBy { it.createdAt }.thenBy { it.id })

@Composable
internal fun PartyBadge(party: Party, size: androidx.compose.ui.unit.Dp = 40.dp) {
    if (party.kind == PartyKind.BUSINESS) IconBadge(party.kind.icon(), party.id, size = size)
    else Avatar(party.name, size = size)
}

/** "Owes you ₹800", "You owe ₹700", or "Settled", in the right colors. */
@Composable
internal fun owedText(owed: Map<String, BigInteger>): Pair<String, Color> {
    val locale = LocalValueDisplay.current.numberLocale
    val (currency, amount) = owed.entries.maxByOrNull { it.value.abs() }?.toPair() ?: return stringResource(R.string.people_settled) to MaterialTheme.colorScheme.onSurfaceVariant
    val money = ValueFormat.money(amount.abs(), currency, locale)
    return if (amount.signum() > 0) stringResource(R.string.people_owes_you, money) to LocalSignals.current.positive
    else stringResource(R.string.people_you_owe, money) to MaterialTheme.colorScheme.error
}

@Composable
internal fun ShareKind.label(): String = stringResource(
    when (this) {
        ShareKind.LENT -> R.string.share_lent
        ShareKind.SPLIT -> R.string.share_split
        ShareKind.PAID_BACK -> R.string.share_paid_back
        ShareKind.BORROWED -> R.string.share_borrowed
        ShareKind.GOT_BACK -> R.string.share_got_back
    },
)

/** Everyone you have paid or been paid by, who owes whom first; a tap opens their chat. */
@Composable
fun PeopleScreen(onBack: () -> Unit, onParty: (String) -> Unit, viewModel: FinanceViewModel = hiltViewModel()) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    EonslateScaffold(
        title = stringResource(R.string.people_and_businesses),
        onBack = onBack,
        filter = FilterTop(query, { query = it }, stringResource(R.string.people_search)),
    ) { padding ->
        val data = ledger ?: return@EonslateScaffold
        val people = remember(data, query) { data.people().filter { query.isBlank() || it.party.name.contains(query.trim(), ignoreCase = true) } }
        if (people.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.Groups, stringResource(R.string.people_empty_title), stringResource(R.string.people_empty_message))
            }
            return@EonslateScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            // People first, then businesses, each in its own group.
            people.groupBy { it.party.kind }.toSortedMap().forEach { (kind, group) ->
            item(key = "head-$kind") {
                SectionHeader(stringResource(if (kind == PartyKind.BUSINESS) R.string.people_businesses else R.string.people_title))
            }
            items(group.size, key = { group[it].party.id }) { index ->
                val line = group[index]
                val (owed, color) = owedText(line.owed)
                ConnectedItem(index, group.size) {
                    ConnectedRow(
                        title = line.party.name,
                        // What you paid them leads; who owes whom only when someone does.
                        supporting = listOfNotNull(
                            paidLine(line.paid),
                            owed.takeIf { line.owed.isNotEmpty() },
                            line.last?.let { dayLabel(it, LocalDate.now()) },
                        ).joinToString(" · ").ifBlank { null },
                        leading = { PartyBadge(line.party) },
                        trailing = if (line.owed.isNotEmpty()) ({ Text(owed.substringAfterLast(' '), color = color, style = MaterialTheme.typography.titleSmall) }) else null,
                        onClick = { onParty(line.party.id) },
                    )
                }
            }
            }
        }
    }
}

/**
 * One person or business, as a chat: where you stand, every payment between you as a bubble
 * (yours on the right), and buttons to record what you paid, what you got, or to settle up.
 */
@Composable
fun PartyScreen(partyId: String, onBack: () -> Unit, viewModel: FinanceViewModel = hiltViewModel()) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val floor by viewModel.overallFloor.collectAsStateWithLifecycle(initialValue = null)
    val data = ledger
    val party = data?.parties?.firstOrNull { it.id == partyId }
    LaunchedEffect(data, party) { if (data != null && party == null) onBack() }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var creating by remember { mutableStateOf<NewWith?>(null) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    EonslateScaffold(
        title = party?.name.orEmpty(),
        onBack = onBack,
        actions = {
            Box {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.people_more))
                }
                EonslateMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    groups = listOf(
                        listOf(
                            MenuAction(stringResource(R.string.people_edit), Icons.Rounded.Edit) { renaming = true },
                            MenuAction(stringResource(R.string.people_delete), Icons.Rounded.Delete, destructive = true) { deleting = true },
                        ),
                    ),
                )
            }
        },
    ) { padding ->
        if (data == null || party == null) return@EonslateScaffold
        val owed = remember(data) { data.owed()[partyId].orEmpty() }
        val chat = remember(data) { data.withParty(partyId) }
        val accounts = remember(data) { data.accounts.associateBy { it.id } }
        val totals = remember(data) { data.totalsWith(partyId, viewModel.today()) }
        val plans = remember(data) { data.recurring.filter { it.partyId == partyId } }
        val listState = rememberLazyListState()
        // The newest payment is at the bottom, as in a chat.
        LaunchedEffect(chat.size) { if (chat.isNotEmpty()) listState.scrollToItem(chat.size + chat.map { it.epochDay }.distinct().size) }
        val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        // What you paid them stays in view however far the chat scrolls.
        if (totals.isNotEmpty()) PaidSummary(totals, Modifier.padding(start = padding.calculateStartPadding(direction), end = padding.calculateEndPadding(direction)))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = padding.calculateStartPadding(direction),
                end = padding.calculateEndPadding(direction),
                bottom = padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "head") { PartyHead(party, owed, onNew = { creating = it }) }
            totals.firstOrNull()?.takeIf { it.byCategory.size > 1 || it.byCategory.singleOrNull()?.first?.isNotEmpty() == true }?.let { main ->
                item(key = "categories") { PaidByCategory(main) }
            }
            if (plans.isNotEmpty()) item(key = "plans") { PartyPlans(plans, accounts) }
            if (chat.isEmpty()) item(key = "none") {
                Text(
                    stringResource(R.string.people_no_payments),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
            chat.groupBy { it.epochDay }.forEach { (day, list) ->
                item(key = "day-$day") {
                    Text(
                        dayLabel(day, LocalDate.now()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                items(list, key = { it.id }) { t -> Bubble(t, partyId, accounts[t.accountId]?.let { it.label to it.currency }) { editing = t } }
            }
        }
        }
        val parties = remember(data) { data.partiesByUse() }
        val suggest = remember(data) { { kind: TransactionKind -> Categories.suggestions(data, kind) } }
        CompositionLocalProvider(LocalCategorySuggestions provides suggest, LocalCategoryStyles provides data.categories) {
        editing?.let { t ->
            key(t.id) {
                TransactionEditor(
                    transaction = t, accounts = usualAccounts(data), today = viewModel.today(), prefill = null,
                    busy = busy, failed = failed, onDismiss = { if (!busy) editing = null },
                    onSave = { account, kind, amount, day, note, destination, category, smsKey, details, partyName, pending, share, attached ->
                        viewModel.save(
                            t.copy(accountId = account, kind = kind, minor = amount, epochDay = day, note = note, destinationId = destination,
                                category = category, smsKey = smsKey, details = details, pending = pending),
                            partyName, share, attached,
                        ) { editing = null }
                    },
                    initialAttachments = data.attachmentsByTransaction[t.id].orEmpty(),
                    newId = viewModel::newId,
                    onDelete = { viewModel.delete(t) { editing = null } },
                    parties = parties,
                    floorCheck = { candidate -> Floors.breaches(data, candidate, t, floor) },
                )
            }
        }
        creating?.let { new ->
            TransactionEditor(
                transaction = null, accounts = usualAccounts(data), today = viewModel.today(), prefill = null, initialKind = new.kind,
                busy = busy, failed = failed, onDismiss = { if (!busy) creating = null },
                onSave = { account, kind, amount, day, note, destination, category, smsKey, details, partyName, pending, share, attached ->
                    viewModel.save(
                        Transaction(viewModel.newId(), account, kind, amount, day, note, destination, category, smsKey, details, pending = pending),
                        partyName, share, attached,
                    ) { creating = null }
                },
                newId = viewModel::newId,
                onDelete = null,
                parties = parties,
                floorCheck = { candidate -> Floors.breaches(data, candidate, null, floor) },
                initialParty = party.name,
                initialShare = new.share,
                initialMinor = new.minor,
            )
        }
        }
        if (renaming) PartyEditor(party, onDismiss = { renaming = false }) { name, kind ->
            viewModel.saveParty(party.copy(name = name, kind = kind)) { renaming = false }
        }
        if (deleting) ConfirmDialog(
            icon = Icons.Rounded.Delete,
            title = stringResource(R.string.people_delete_title, party.name),
            message = stringResource(if (owed.isEmpty() && data.transactions.none { t -> t.shares.any { it.partyId == partyId } }) R.string.people_delete_message else R.string.people_delete_blocked),
            confirmLabel = stringResource(R.string.people_delete),
            onConfirm = {
                deleting = false
                viewModel.deleteParty(partyId, onBack)
            },
            onDismiss = { deleting = false },
        )
    }
}

/** A new payment from the chat: its kind, and when settling up, the share and amount. */
internal data class NewWith(val kind: TransactionKind, val share: ShareDraft = ShareDraft(), val minor: Long? = null)

@Composable
private fun PartyHead(party: Party, owed: Map<String, BigInteger>, onNew: (NewWith) -> Unit) {
    val (text, color) = owedText(owed)
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PartyBadge(party, 72.dp)
            // Who owes whom matters only once money is lent, borrowed or split with them.
            if (owed.isNotEmpty()) Text(text, style = MaterialTheme.typography.titleLarge, color = color)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onNew(NewWith(TransactionKind.EXPENSE)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.people_you_paid))
                }
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = { onNew(NewWith(TransactionKind.INCOME)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.people_you_got))
                }
            }
            // Settling up records the payment that clears what is between you.
            owed.entries.maxByOrNull { it.value.abs() }?.let { (_, amount) ->
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        onNew(
                            if (amount.signum() > 0) NewWith(TransactionKind.INCOME, ShareDraft(ShareMode.GOT_BACK), amount.toLong())
                            else NewWith(TransactionKind.EXPENSE, ShareDraft(ShareMode.PAID_BACK), amount.abs().toLong()),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.people_settle)) }
            }
        }
    }
}

/** What you paid them this year, large, with this month, in all, and what came back from them. */
@Composable
private fun PaidSummary(totals: List<PartyTotals>, modifier: Modifier = Modifier) {
    val locale = LocalValueDisplay.current.numberLocale
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = RoundedCornerShape(24.dp), modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.people_paid_this_year), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            totals.forEach { t ->
                Text(ValueFormat.money(t.paidThisYear, t.currency, locale), style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOfNotNull(
                        stringResource(R.string.people_paid_month, ValueFormat.money(t.paidThisMonth, t.currency, locale)),
                        stringResource(R.string.people_paid_all, ValueFormat.money(t.paidInAll, t.currency, locale)),
                        t.receivedInAll.takeIf { it.signum() > 0 }?.let { stringResource(R.string.people_got_all, ValueFormat.money(it, t.currency, locale)) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** What you paid them, by category, in all: Apple Music and iCloud for Apple. */
@Composable
private fun PaidByCategory(totals: PartyTotals) {
    val locale = LocalValueDisplay.current.numberLocale
    Column {
        SectionHeader(stringResource(R.string.people_by_category))
        ConnectedGroup {
            totals.byCategory.forEachIndexed { index, (category, minor) ->
                ConnectedItem(index, totals.byCategory.size) {
                    ConnectedRow(
                        title = category.ifEmpty { stringResource(R.string.people_no_category) },
                        trailing = { Text(ValueFormat.money(minor, totals.currency, locale), style = MaterialTheme.typography.titleSmall) },
                    )
                }
            }
        }
    }
}

/** The recurring payments with them, such as Apple Music and iCloud, and what each costs. */
@Composable
private fun PartyPlans(plans: List<RecurringPayment>, accounts: Map<String, Account>) {
    val locale = LocalValueDisplay.current.numberLocale
    Column {
        SectionHeader(stringResource(R.string.people_recurring))
        ConnectedGroup {
            plans.forEachIndexed { index, plan ->
                ConnectedItem(index, plans.size) {
                    ConnectedRow(
                        title = plan.note,
                        supporting = listOfNotNull(plan.interval.label(), plan.category.ifBlank { null }).joinToString(" · "),
                        trailing = {
                            accounts[plan.accountId]?.let { Text(ValueFormat.money(plan.minor, it.currency, locale), style = MaterialTheme.typography.titleSmall) }
                        },
                    )
                }
            }
        }
    }
}

/** One payment: yours on the right, theirs on the left, with what it was between you. */
@Composable
private fun Bubble(t: Transaction, partyId: String, account: Pair<String, String>?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val mine = t.kind == TransactionKind.EXPENSE
    val locale = LocalValueDisplay.current.numberLocale
    val currency = account?.second ?: return
    val share = t.shares.firstOrNull { it.partyId == partyId }
    val kind = t.shareKind
    val what = when {
        kind == ShareKind.SPLIT && share != null ->
            stringResource(R.string.people_their_share, kind.label(), ValueFormat.money(share.minor, currency, locale))
        kind != null -> kind.label()
        else -> null
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (mine) colors.primaryContainer else colors.surfaceBright,
            contentColor = if (mine) colors.onPrimaryContainer else colors.onSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = if (mine) 24.dp else 6.dp, bottomEnd = if (mine) 6.dp else 24.dp),
            modifier = Modifier.widthIn(max = 300.dp).clickable(role = Role.Button, onClick = onClick),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(if (mine) R.string.people_bubble_paid else R.string.people_bubble_got),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(ValueFormat.money(t.minor, currency, locale), style = MaterialTheme.typography.headlineSmall)
                listOfNotNull(
                    what,
                    t.note.takeIf { it.isNotBlank() } ?: t.category.takeIf { it.isNotBlank() },
                    stringResource(R.string.finance_pending).takeIf { t.pending },
                ).joinToString(" · ").takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(account.first, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Renames a person or business, or makes it one or the other. */
@Composable
private fun PartyEditor(party: Party, onDismiss: () -> Unit, onSave: (String, PartyKind) -> Unit) {
    var name by rememberSaveable { mutableStateOf(party.name) }
    var kind by rememberSaveable { mutableStateOf(party.kind) }
    EditorSheet(stringResource(R.string.people_edit), busy = false, failed = false, onDismiss = onDismiss) {
        ConnectedChoices(PartyKind.entries, kind, { stringResource(if (it == PartyKind.BUSINESS) R.string.people_business else R.string.people_person) }, { kind = it })
        FormField(name, { name = it }, stringResource(R.string.finance_name), maxLength = Party.NAME_LIMIT)
        SaveRow(canSave = name.isNotBlank(), onSave = { onSave(name.trim(), kind) }, delete = null, busy = false)
    }
}

