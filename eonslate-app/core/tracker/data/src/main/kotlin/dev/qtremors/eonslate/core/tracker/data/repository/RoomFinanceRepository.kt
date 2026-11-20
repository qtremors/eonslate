package dev.qtremors.eonslate.core.tracker.data.repository

import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

class RoomFinanceRepository @Inject constructor(private val database: TrackerDatabase) : FinanceRepository {
    private val dao = database.financeDao()

    override fun observeLedger() = database.invalidationTracker.createFlow(
        "finance_accounts", "finance_transactions", "finance_recurring", "finance_budgets", "finance_categories",
    ).map { database.withTransaction { readLedger() } }

    private suspend fun readLedger(): Ledger {
        val rows = dao.ledger()
        return Ledger(
            accounts = rows.map { it.account.toDomain() },
            transactions = rows.flatMap { group -> group.transactions.map { it.toDomain() } }
                .sortedWith(compareByDescending<Transaction> { it.epochDay }.thenByDescending { it.createdAt }.thenBy { it.id }),
            recurring = rows.flatMap { group -> group.recurring.map { it.toDomain() } }
                .sortedWith(compareBy<RecurringPayment> { it.nextEpochDay ?: Long.MAX_VALUE }
                    .thenBy { it.nextMinute ?: 0 }.thenBy { it.id }),
            budgets = rows.flatMap { group -> group.budgets.map { it.toDomain() } }
                .sortedWith(compareBy<CategoryBudget> { it.title.lowercase(Locale.ROOT) }.thenBy { it.id }),
            categories = dao.categories().map { it.toDomain() },
        )
    }

    override suspend fun saveAccount(account: Account): Boolean = database.withTransaction {
        val old = dao.account(account.id)
        if (!account.isValid() || (old != null && old.currency != account.currency)) return@withTransaction false
        dao.upsert(account.toEntity())
        true
    }

    override suspend fun deleteAccount(id: String): Boolean = dao.deleteAccount(id) != 0

    override suspend fun saveTransaction(transaction: Transaction): Boolean = database.withTransaction {
        val accounts = listOfNotNull(dao.account(transaction.accountId), transaction.destinationId?.let { dao.account(it) })
            .associate { it.id to it.toDomain() }
        if (!transaction.isValid(accounts)) return@withTransaction false
        val alreadyImported = transaction.smsKey?.let { dao.importedSms(it) }
        if (alreadyImported != null && alreadyImported != transaction.id) return@withTransaction false
        // An edit keeps its first save time, so it stays where it was within its day.
        val created = dao.createdAt(transaction.id) ?: transaction.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        dao.upsert(transaction.copy(createdAt = created).toEntity())
        true
    }

    override suspend fun deleteTransaction(id: String) = dao.deleteTransaction(id)

    override suspend fun deleteTransactions(ids: Collection<String>) = database.withTransaction {
        ids.distinct().chunked(500).forEach { dao.deleteTransactions(it) }
    }

    override suspend fun appendTransactions(transactions: List<Transaction>): Boolean = database.withTransaction {
        val accounts = dao.accounts().associate { it.id to it.toDomain() }
        if (transactions.any { !it.isValid(accounts) }) return@withTransaction false
        dao.insertTransactions(stamped(transactions).map { it.toEntity() })
        true
    }

    /** Gives entries without a save time one, a millisecond apart so their order survives. */
    private fun stamped(transactions: List<Transaction>): List<Transaction> {
        val now = System.currentTimeMillis()
        return transactions.mapIndexed { index, entry -> if (entry.createdAt > 0) entry else entry.copy(createdAt = now + index) }
    }

    override suspend fun addRecurring(payment: RecurringPayment, throughDay: Long, throughMinute: Int): Boolean = database.withTransaction {
        val account = dao.account(payment.accountId) ?: return@withTransaction false
        val nextDay = payment.nextEpochDay ?: return@withTransaction false
        if (!payment.isValid(setOf(account.id))) return@withTransaction false
        dao.insertRecurring(listOf(payment.toEntity()))
        if (!payment.paused && payment.postsAutomatically && nextDay <= throughDay) postAll(throughDay, throughMinute)
        true
    }

    override suspend fun updateRecurring(expected: RecurringPayment, payment: RecurringPayment, throughDay: Long, throughMinute: Int): Boolean =
        database.withTransaction {
            if (expected.id != payment.id || dao.recurring(expected.id)?.toDomain() != expected) return@withTransaction false
            val account = dao.account(payment.accountId) ?: return@withTransaction false
            if (!payment.isValid(setOf(account.id))) return@withTransaction false
            dao.upsert(payment.toEntity())
            val nextDay = payment.nextEpochDay
            if (!payment.paused && payment.postsAutomatically && nextDay != null && nextDay <= throughDay) postAll(throughDay, throughMinute)
            true
        }

    override suspend fun deleteRecurring(id: String) = dao.deleteRecurring(id)

    override suspend fun recordRecurring(expected: RecurringPayment, minor: Long, day: Long): Boolean = database.withTransaction {
        val due = expected.nextOccurrence ?: return@withTransaction false
        if (expected.paused || minor <= 0 || day !in Transaction.DATES ||
            dao.recurring(expected.id)?.toDomain() != expected
        ) return@withTransaction false
        val transaction = expected.transactionOn(due.toLocalDate().toEpochDay(), expected.nextMinute).copy(minor = minor, epochDay = day)
        dao.insertTransactions(stamped(listOf(transaction)).map { it.toEntity() })
        advance(expected, expected.after(due))
        true
    }

    override suspend fun saveBudget(budget: CategoryBudget): Boolean = database.withTransaction {
        val account = dao.account(budget.accountId) ?: return@withTransaction false
        if (!budget.isValid(setOf(account.id))) return@withTransaction false
        dao.upsert(budget.toEntity())
        true
    }

    override suspend fun deleteBudget(id: String) = dao.deleteBudget(id)

    override suspend fun saveCategory(category: CategoryChoice, previous: CategoryChoice?): Boolean = database.withTransaction {
        val ledger = readLedger()
        val available = Categories.all(ledger)
        if (!category.isValid() ||
            available.firstOrNull { it.id == category.id } != previous ||
            available.any { it.id != category.id && it.key == category.key }
        ) return@withTransaction false

        if (ledger.categories.isEmpty()) dao.insertCategories(Categories.defaults.map { it.toEntity() })
        dao.forgetDeleted(category.key, category.id)
        dao.upsert(category.copy(deleted = false).toEntity())
        if (previous != null && previous.name != category.name) {
            val names = (ledger.transactions.asSequence().map { it.category } +
                ledger.recurring.asSequence().map { it.category } +
                ledger.budgets.asSequence().map { it.category })
                .filter { it.trim().lowercase(Locale.ROOT) == previous.key }
                .distinct()
            for (batch in names.chunked(500)) {
                dao.renameTransactionCategories(batch, category.name)
                dao.renameRecurringCategories(batch, category.name)
                dao.renameBudgetCategories(batch, category.name)
            }
        }
        true
    }

    override suspend fun deleteCategory(category: CategoryChoice): Boolean = database.withTransaction {
        val ledger = readLedger()
        val available = Categories.all(ledger)
        if (available.size <= 1 || available.none { it.id == category.id && it.key == category.key }) return@withTransaction false
        // The defaults become real rows first, so deleting one never brings the others back.
        if (ledger.categories.isEmpty()) dao.insertCategories(Categories.defaults.map { it.toEntity() })
        dao.forgetDeleted(category.key, category.id)
        dao.upsert(category.copy(deleted = true).toEntity())
        true
    }

    override suspend fun postDue(throughDay: Long, throughMinute: Int) {
        while (database.withTransaction { postBatch(throughDay, throughMinute) }) yield()
    }

    /** Every elapsed occurrence, as a save asks: nothing due is left for a later catch-up. */
    private suspend fun postAll(throughDay: Long, throughMinute: Int) {
        while (postBatch(throughDay, throughMinute)) Unit
    }

    /**
     * Posts one bounded batch. The entries and the next due dates commit together. An occurrence
     * already in the ledger by name, day and kind, as when it was entered by hand, is not repeated.
     */
    private suspend fun postBatch(throughDay: Long, throughMinute: Int): Boolean {
        val through = LocalDate.ofEpochDay(throughDay).atStartOfDay().plusMinutes(throughMinute.toLong())
        var remaining = 128
        for (row in dao.due(throughDay, throughMinute)) {
            val payment = row.toDomain()
            val entries = mutableListOf<Transaction>()
            var next = payment.nextOccurrence
            while (next != null && !next.isAfter(through) && remaining > 0) {
                entries += payment.transactionOn(next.toLocalDate().toEpochDay())
                next = payment.after(next, through)
                remaining--
            }
            val fresh = entries.filter { entry ->
                entry.note.isBlank() || dao.matching(entry.note, entry.epochDay, entry.kind.name, entry.id) == 0
            }
            dao.insertDue(stamped(fresh).map { it.toEntity() })
            advance(payment, next)
            if (remaining == 0) return true
        }
        return false
    }

    private suspend fun advance(payment: RecurringPayment, next: LocalDateTime?) {
        val minute = next?.takeIf { payment.interval == PaymentInterval.HOURLY }?.let { it.hour * 60 + it.minute }
        dao.advance(payment.id, next?.toLocalDate()?.toEpochDay(), minute)
    }
}
