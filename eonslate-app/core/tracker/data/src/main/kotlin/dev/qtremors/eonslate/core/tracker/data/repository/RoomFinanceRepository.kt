package dev.qtremors.eonslate.core.tracker.data.repository

import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield
import javax.inject.Inject

class RoomFinanceRepository @Inject constructor(private val database: TrackerDatabase) : FinanceRepository {
    private val dao = database.financeDao()

    override fun observeLedger() = dao.observeLedger().map { rows ->
        Ledger(
            accounts = rows.map { it.account.toDomain() },
            transactions = rows.flatMap { group -> group.transactions.map { it.toDomain() } }
                .sortedWith(compareByDescending<Transaction> { it.epochDay }.thenBy { it.id }),
            recurring = rows.flatMap { group -> group.recurring.map { it.toDomain() } }
                .sortedWith(compareBy<RecurringPayment> { it.nextEpochDay ?: Long.MAX_VALUE }.thenBy { it.id }),
            budgets = rows.flatMap { group -> group.budgets.map { it.toDomain() } }
                .sortedWith(compareBy<CategoryBudget> { it.category.lowercase() }.thenBy { it.id }),
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
        dao.upsert(transaction.toEntity())
        true
    }

    override suspend fun deleteTransaction(id: String) = dao.deleteTransaction(id)

    override suspend fun appendTransactions(transactions: List<Transaction>): Boolean = database.withTransaction {
        val accounts = dao.accounts().associate { it.id to it.toDomain() }
        if (transactions.any { !it.isValid(accounts) }) return@withTransaction false
        dao.insertTransactions(transactions.map { it.toEntity() })
        true
    }

    override suspend fun addRecurring(payment: RecurringPayment, throughDay: Long): Boolean = database.withTransaction {
        val account = dao.account(payment.accountId) ?: return@withTransaction false
        val nextDay = payment.nextEpochDay ?: return@withTransaction false
        if (!payment.isValid(setOf(account.id))) return@withTransaction false
        dao.insertRecurring(listOf(payment.toEntity()))
        if (!payment.paused && !payment.variable && nextDay <= throughDay) postBatch(throughDay)
        true
    }

    override suspend fun updateRecurring(expected: RecurringPayment, payment: RecurringPayment, throughDay: Long): Boolean =
        database.withTransaction {
            if (expected.id != payment.id || dao.recurring(expected.id)?.toDomain() != expected) return@withTransaction false
            val account = dao.account(payment.accountId) ?: return@withTransaction false
            if (!payment.isValid(setOf(account.id))) return@withTransaction false
            dao.upsert(payment.toEntity())
            val nextDay = payment.nextEpochDay
            if (!payment.paused && !payment.variable && nextDay != null && nextDay <= throughDay) postBatch(throughDay)
            true
        }

    override suspend fun deleteRecurring(id: String) = dao.deleteRecurring(id)

    override suspend fun recordRecurring(expected: RecurringPayment, minor: Long, day: Long): Boolean = database.withTransaction {
        val due = expected.nextEpochDay ?: return@withTransaction false
        if (expected.paused || minor <= 0 || day !in Transaction.DATES ||
            dao.recurring(expected.id)?.toDomain() != expected
        ) return@withTransaction false
        val transaction = expected.transactionOn(due).copy(minor = minor, epochDay = day)
        dao.insertTransactions(listOf(transaction.toEntity()))
        dao.advance(expected.id, expected.after(due))
        true
    }

    override suspend fun saveBudget(budget: CategoryBudget): Boolean = database.withTransaction {
        val account = dao.account(budget.accountId) ?: return@withTransaction false
        if (!budget.isValid(setOf(account.id))) return@withTransaction false
        dao.upsert(budget.toEntity())
        true
    }

    override suspend fun deleteBudget(id: String) = dao.deleteBudget(id)

    override suspend fun postDue(throughDay: Long) {
        while (database.withTransaction { postBatch(throughDay) }) yield()
    }

    /** Posts one bounded batch. The entries and the next due dates commit together. */
    private suspend fun postBatch(throughDay: Long): Boolean {
        var remaining = 128
        for (row in dao.due(throughDay)) {
            val payment = row.toDomain()
            val entries = mutableListOf<Transaction>()
            var next = payment.nextEpochDay
            while (next != null && next <= throughDay && remaining > 0) {
                entries += payment.transactionOn(next)
                next = payment.after(next)
                remaining--
            }
            dao.insertDue(entries.map { it.toEntity() })
            dao.advance(payment.id, next)
            if (remaining == 0) return true
        }
        return false
    }
}
