package dev.qtremors.eonslate.core.tracker.data.repository

import androidx.room.withTransaction
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.toEntity
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
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
        dao.upsert(transaction.toEntity())
        true
    }

    override suspend fun deleteTransaction(id: String) = dao.deleteTransaction(id)

    override suspend fun addRecurring(payment: RecurringPayment, throughDay: Long): Boolean = database.withTransaction {
        val account = dao.account(payment.accountId) ?: return@withTransaction false
        val nextDay = payment.nextEpochDay ?: return@withTransaction false
        if (!payment.isValid(setOf(account.id))) return@withTransaction false
        dao.insertRecurring(listOf(payment.toEntity()))
        if (nextDay <= throughDay) postBatch(throughDay)
        true
    }

    override suspend fun deleteRecurring(id: String) = dao.deleteRecurring(id)

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
