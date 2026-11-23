package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

@Serializable
data class Account(
    val id: String,
    val name: String,
    val currency: String,
    @SerialName("opening_minor") val openingMinor: Long = 0,
    @SerialName("monthly_budget_minor") val monthlyBudgetMinor: Long? = null,
    val kind: AccountKind = AccountKind.BANK,
    /** The last four digits of the card or account number, which payment SMS name. */
    val digits: String? = null,
    /**
     * The least to keep in it, in minor units; a payment that takes it lower is warned about.
     * Below zero suits a credit card's limit.
     */
    @SerialName("floor_minor") val floorMinor: Long? = null,
    /** A fixed deposit's terms; set exactly when [kind] is [AccountKind.DEPOSIT]. */
    val deposit: Deposit? = null,
) {
    fun isValid(): Boolean = id.isNotBlank() && name.isNotBlank() &&
        name.length <= NAME_LIMIT && MoneyAmount.isKnownCurrency(currency) &&
        (monthlyBudgetMinor == null || monthlyBudgetMinor > 0) && (digits == null || DIGITS.matches(digits)) &&
        (kind == AccountKind.DEPOSIT) == (deposit != null) && deposit?.isValid() != false

    /** A fixed deposit that has been paid out. */
    val isClosed: Boolean get() = deposit?.isOpen == false

    /** The name, with its last digits when it has them: "HDFC •4821". */
    val label: String get() = digits?.let { "$name •$it" } ?: name

    companion object {
        const val NAME_LIMIT = 60
        val DIGITS = Regex("[0-9]{4}")
    }
}

/** What an account is, for its card and icon. */
@Serializable
enum class AccountKind(val storageKey: String) {
    @SerialName("bank") BANK("bank"),
    @SerialName("card") CARD("card"),
    @SerialName("wallet") WALLET("wallet"),
    @SerialName("cash") CASH("cash"),
    /** A fixed deposit; made and closed on its own page, not in the account editor. */
    @SerialName("deposit") DEPOSIT("deposit");

    companion object {
        fun fromKey(key: String?): AccountKind = entries.firstOrNull { it.storageKey == key } ?: BANK

        /** What the account editor offers; deposits have their own. */
        val EVERYDAY: List<AccountKind> = listOf(BANK, CARD, WALLET, CASH)
    }
}

/**
 * The account a payment message names by [ending], among those in [currency]: the one with those
 * last digits, else one whose name ends with them, else the first in that currency.
 */
fun List<Account>.forPayment(currency: String, ending: String?): Account? {
    // Payments do not come from fixed deposits.
    val matching = filter { it.currency == currency && it.deposit == null }
    return ending?.let { end -> matching.firstOrNull { it.digits == end } ?: matching.firstOrNull { it.name.trim().endsWith(end) } }
        ?: matching.firstOrNull()
}

@Serializable
enum class TransactionKind {
    @SerialName("income") INCOME,
    @SerialName("expense") EXPENSE,
    @SerialName("transfer") TRANSFER,
}

/** A transfer is one record so both sides always move together. Amounts are positive minor units. */
@Serializable
data class Transaction(
    val id: String,
    @SerialName("account_id") val accountId: String,
    val kind: TransactionKind,
    val minor: Long,
    @SerialName("epoch_day") val epochDay: Long,
    val note: String = "",
    @SerialName("destination_id") val destinationId: String? = null,
    val category: String = "",
    @SerialName("sms_key") val smsKey: String? = null,
    /** Longer notes, apart from [note], which names the transaction. */
    val details: String = "",
    /** When it was first saved, in epoch milliseconds; orders transactions on the same day. */
    @SerialName("created_at") val createdAt: Long = 0,
    /** The person or business paid, or paid by; null when not said. */
    @SerialName("party_id") val partyId: String? = null,
    /** Not cleared yet, such as a card hold or a cheque. It still counts in balances. */
    val pending: Boolean = false,
    /** What people owe you from this, or you owe them; see [Share]. */
    val shares: List<Share> = emptyList(),
) {
    fun isValid(accounts: Map<String, Account>): Boolean {
        val account = accounts[accountId] ?: return false
        if (id.isBlank() || minor <= 0 || note.length > NOTE_LIMIT || details.length > DETAILS_LIMIT ||
            createdAt < 0 || epochDay !in DATES ||
            category.length > CATEGORY_LIMIT || (smsKey != null && !SMS_KEY.matches(smsKey))
        ) return false
        if (!sharesValid()) return false
        return if (kind == TransactionKind.TRANSFER) {
            destinationId != accountId && accounts[destinationId]?.currency == account.currency
        } else destinationId == null
    }

    companion object {
        const val NOTE_LIMIT = 1024
        const val DETAILS_LIMIT = 2000
        const val CATEGORY_LIMIT = 100
        private val SMS_KEY = Regex("[0-9a-f]{64}")
        val DATES = LocalDate.of(1, 1, 1).toEpochDay()..LocalDate.of(9999, 12, 31).toEpochDay()
    }
}

data class Ledger(
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val recurring: List<RecurringPayment> = emptyList(),
    val budgets: List<CategoryBudget> = emptyList(),
    val categories: List<CategoryChoice> = emptyList(),
    val parties: List<Party> = emptyList(),
    @SerialName("piggy_banks") val piggyBanks: List<PiggyBank> = emptyList(),
    @SerialName("piggy_moves") val piggyMoves: List<PiggyMove> = emptyList(),
) {
    /** Each party by id, for names on rows. */
    val partiesById: Map<String, Party> get() = parties.associateBy { it.id }

    fun filteredTransactions(filter: TransactionFilter): List<Transaction> {
        val term = filter.query.trim()
        val names = accounts.associate { it.id to it.name }
        val partyNames = parties.associate { it.id to it.name }
        val newest = compareByDescending<Transaction> { it.epochDay }.thenByDescending { it.createdAt }.thenBy { it.id }
        val order = when (filter.sort) {
            TransactionSort.NEWEST -> newest
            TransactionSort.OLDEST -> compareBy<Transaction> { it.epochDay }.thenBy { it.createdAt }.thenBy { it.id }
            TransactionSort.LARGEST -> compareByDescending<Transaction> { it.minor }.then(newest)
            TransactionSort.SMALLEST -> compareBy<Transaction> { it.minor }.then(newest)
            TransactionSort.CATEGORY -> compareBy<Transaction> { it.category.isBlank() }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.category.trim() }.then(newest)
        }
        return transactions.asSequence().filter { transaction ->
            transaction.epochDay in filter.days &&
                (filter.kind == null || transaction.kind == filter.kind) &&
                (filter.category == null || transaction.category.trim().equals(filter.category, ignoreCase = true)) &&
                (term.isEmpty() || transaction.note.contains(term, ignoreCase = true) ||
                    transaction.details.contains(term, ignoreCase = true) ||
                    transaction.category.contains(term, ignoreCase = true) ||
                    names[transaction.accountId]?.contains(term, ignoreCase = true) == true ||
                    transaction.partyId?.let(partyNames::get)?.contains(term, ignoreCase = true) == true) &&
                (filter.partyId == null || transaction.partyId == filter.partyId) &&
                (!filter.pendingOnly || transaction.pending) &&
                (filter.accountId == null || transaction.accountId == filter.accountId || transaction.destinationId == filter.accountId)
        }.sortedWith(order).toList()
    }

    /** Balances may exceed the range of a single transaction. Each account retains its own currency. */
    fun balances(): Map<String, BigInteger> {
        val balances = accounts.associate { it.id to BigInteger.valueOf(it.openingMinor) }.toMutableMap()
        for (transaction in transactions) {
            val amount = BigInteger.valueOf(transaction.minor)
            val change = if (transaction.kind == TransactionKind.INCOME) amount else amount.negate()
            balances[transaction.accountId]?.let { balances[transaction.accountId] = it + change }
            transaction.destinationId?.let { id -> balances[id]?.let { balances[id] = it + amount } }
        }
        return balances
    }

    /** Every budget on [day]: category budgets, then accounts' monthly limits, each in its own currency. */
    fun budgetHealth(day: LocalDate): List<BudgetHealth> {
        val byId = accounts.associateBy { it.id }
        val month = YearMonth.from(day)
        val spent = monthlySpending(month)
        val monthDays = month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()
        return budgets.mapNotNull { budget ->
            val account = byId[budget.accountId] ?: return@mapNotNull null
            BudgetHealth(budget.title, account.currency, spendingFor(budget, day), BigInteger.valueOf(budget.minor), budget, budget.periodOn(day))
        } + accounts.mapNotNull { account ->
            account.monthlyBudgetMinor?.let {
                BudgetHealth(account.name, account.currency, spent.getOrDefault(account.id, BigInteger.ZERO), BigInteger.valueOf(it), period = monthDays, account = account)
            }
        }
    }

    fun monthlySpending(month: YearMonth): Map<String, BigInteger> {
        val days = month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()
        val spent = mutableMapOf<String, BigInteger>()
        for (transaction in transactions) {
            if (transaction.kind == TransactionKind.EXPENSE && transaction.epochDay in days) {
                spent[transaction.accountId] =
                    spent.getOrDefault(transaction.accountId, BigInteger.ZERO) + BigInteger.valueOf(transaction.own)
            }
        }
        return spent
    }

    fun spendingFor(budget: CategoryBudget, day: LocalDate): BigInteger {
        val period = budget.periodOn(day) ?: return BigInteger.ZERO
        val currency = accounts.firstOrNull { it.id == budget.accountId }?.currency
        val counted = if (budget.allAccounts) accounts.filter { it.currency == currency }.mapTo(HashSet()) { it.id }
        else setOf(budget.accountId)
        return transactions.asSequence()
            .filter {
                it.kind == TransactionKind.EXPENSE && it.accountId in counted &&
                    it.category.equals(budget.category, ignoreCase = true) && it.epochDay in period
            }
            .fold(BigInteger.ZERO) { sum, entry -> sum + BigInteger.valueOf(entry.own) }
    }
}

enum class TransactionSort { NEWEST, OLDEST, LARGEST, SMALLEST, CATEGORY }

/** What the transaction list shows: a day range, optionally one kind and one category, and a search. */
data class TransactionFilter(
    val days: LongRange = Transaction.DATES,
    val kind: TransactionKind? = null,
    val category: String? = null,
    val query: String = "",
    val sort: TransactionSort = TransactionSort.NEWEST,
    /** Only what moved money in or out of this account. */
    val accountId: String? = null,
    /** Only payments with this person or business. */
    val partyId: String? = null,
    /** Only transactions not cleared yet. */
    val pendingOnly: Boolean = false,
)

interface FinanceRepository {
    fun observeLedger(): Flow<Ledger>
    suspend fun saveAccount(account: Account): Boolean
    suspend fun deleteAccount(id: String): Boolean

    /**
     * Saves a fixed deposit's account with [transactions] that fund or close it, all or nothing;
     * false when the account is not a valid deposit or any transaction does not fit.
     */
    suspend fun saveDeposit(account: Account, transactions: List<Transaction> = emptyList()): Boolean

    /**
     * Removes a fixed deposit and every transaction in or out of it, as when it was added by
     * mistake. False, and nothing removed, when [id] is not a deposit.
     */
    suspend fun deleteDeposit(id: String): Boolean
    suspend fun saveTransaction(transaction: Transaction): Boolean
    /** Adds [transactions] in one go, saving [parties] they name first. */
    suspend fun appendTransactions(transactions: List<Transaction>, parties: List<Party> = emptyList()): Boolean

    /** Saves a person or business; false when another already has the name. */
    suspend fun saveParty(party: Party): Boolean

    /** Saves a piggy bank; false when it does not fit its account. */
    suspend fun savePiggy(piggy: PiggyBank): Boolean

    /** Removes a piggy bank and what was put in it; the money stays in the account. */
    suspend fun deletePiggy(id: String)

    /** Puts money in or takes it out; false when that would leave less than nothing in it. */
    suspend fun movePiggy(move: PiggyMove): Boolean

    suspend fun deletePiggyMove(id: String)

    /**
     * Removes a party; its transactions keep their amounts and lose the name. False, and nothing
     * removed, while any transaction shares money with them.
     */
    suspend fun deleteParty(id: String): Boolean
    suspend fun deleteTransaction(id: String)
    /** Deletes all of [ids] in one database transaction, or none of them. */
    suspend fun deleteTransactions(ids: Collection<String>)
    suspend fun addRecurring(payment: RecurringPayment, throughDay: Long, throughMinute: Int = 1439): Boolean
    suspend fun updateRecurring(expected: RecurringPayment, payment: RecurringPayment, throughDay: Long, throughMinute: Int = 1439): Boolean
    suspend fun deleteRecurring(id: String)
    suspend fun recordRecurring(expected: RecurringPayment, minor: Long, day: Long): Boolean
    suspend fun postDue(throughDay: Long, throughMinute: Int = 1439)
    suspend fun saveBudget(budget: CategoryBudget): Boolean
    suspend fun deleteBudget(id: String)
    suspend fun saveCategory(category: CategoryChoice, previous: CategoryChoice?): Boolean
    /** Removes [category] from the choices; at least one category always stays. */
    suspend fun deleteCategory(category: CategoryChoice): Boolean
}
