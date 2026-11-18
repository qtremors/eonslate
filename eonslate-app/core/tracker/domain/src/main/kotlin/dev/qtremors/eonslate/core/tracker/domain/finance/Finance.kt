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
) {
    fun isValid(): Boolean = id.isNotBlank() && name.isNotBlank() &&
        name.length <= NAME_LIMIT && MoneyAmount.isKnownCurrency(currency) &&
        (monthlyBudgetMinor == null || monthlyBudgetMinor > 0)

    companion object {
        const val NAME_LIMIT = 60
    }
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
) {
    fun isValid(accounts: Map<String, Account>): Boolean {
        val account = accounts[accountId] ?: return false
        if (id.isBlank() || minor <= 0 || note.length > NOTE_LIMIT || epochDay !in DATES ||
            category.length > CATEGORY_LIMIT || (smsKey != null && !SMS_KEY.matches(smsKey))
        ) return false
        return if (kind == TransactionKind.TRANSFER) {
            destinationId != accountId && accounts[destinationId]?.currency == account.currency
        } else destinationId == null
    }

    companion object {
        const val NOTE_LIMIT = 1024
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
) {
    fun filteredTransactions(days: LongRange, category: String?, query: String, sort: TransactionSort): List<Transaction> {
        val term = query.trim()
        val names = accounts.associate { it.id to it.name }
        val order = when (sort) {
            TransactionSort.NEWEST -> compareByDescending<Transaction> { it.epochDay }
            TransactionSort.OLDEST -> compareBy<Transaction> { it.epochDay }
            TransactionSort.LARGEST -> compareByDescending<Transaction> { it.minor }
        }
        return transactions.asSequence().filter { transaction ->
            transaction.epochDay in days &&
                (category == null || transaction.category.trim().equals(category, ignoreCase = true)) &&
                (term.isEmpty() || transaction.note.contains(term, ignoreCase = true) ||
                    transaction.category.contains(term, ignoreCase = true) ||
                    names[transaction.accountId]?.contains(term, ignoreCase = true) == true)
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

    fun monthlySpending(month: YearMonth): Map<String, BigInteger> {
        val days = month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()
        val spent = mutableMapOf<String, BigInteger>()
        for (transaction in transactions) {
            if (transaction.kind == TransactionKind.EXPENSE && transaction.epochDay in days) {
                spent[transaction.accountId] =
                    spent.getOrDefault(transaction.accountId, BigInteger.ZERO) + BigInteger.valueOf(transaction.minor)
            }
        }
        return spent
    }

    fun spendingFor(budget: CategoryBudget, day: LocalDate): BigInteger {
        val period = budget.periodOn(day) ?: return BigInteger.ZERO
        return transactions.asSequence()
            .filter {
                it.kind == TransactionKind.EXPENSE && it.accountId == budget.accountId &&
                    it.category.equals(budget.category, ignoreCase = true) && it.epochDay in period
            }
            .fold(BigInteger.ZERO) { sum, entry -> sum + BigInteger.valueOf(entry.minor) }
    }
}

enum class TransactionSort { NEWEST, OLDEST, LARGEST }

interface FinanceRepository {
    fun observeLedger(): Flow<Ledger>
    suspend fun saveAccount(account: Account): Boolean
    suspend fun deleteAccount(id: String): Boolean
    suspend fun saveTransaction(transaction: Transaction): Boolean
    suspend fun appendTransactions(transactions: List<Transaction>): Boolean
    suspend fun deleteTransaction(id: String)
    suspend fun addRecurring(payment: RecurringPayment, throughDay: Long, throughMinute: Int = 1439): Boolean
    suspend fun updateRecurring(expected: RecurringPayment, payment: RecurringPayment, throughDay: Long, throughMinute: Int = 1439): Boolean
    suspend fun deleteRecurring(id: String)
    suspend fun recordRecurring(expected: RecurringPayment, minor: Long, day: Long): Boolean
    suspend fun postDue(throughDay: Long, throughMinute: Int = 1439)
    suspend fun saveBudget(budget: CategoryBudget): Boolean
    suspend fun deleteBudget(id: String)
    suspend fun saveCategory(category: CategoryChoice, previous: CategoryChoice?): Boolean
}
