package dev.qtremors.eonslate.core.tracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction as LedgerTransaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "finance_accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String,
    @ColumnInfo(name = "opening_minor") val openingMinor: Long,
    @ColumnInfo(name = "monthly_budget_minor") val monthlyBudgetMinor: Long?,
) {
    fun toDomain() = Account(id, name, currency, openingMinor, monthlyBudgetMinor)
}

@Entity(
    tableName = "finance_transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["destination_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("account_id"), Index("destination_id"), Index(value = ["sms_key"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val kind: String,
    val minor: Long,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    val note: String,
    @ColumnInfo(name = "destination_id") val destinationId: String?,
    @ColumnInfo(defaultValue = "''") val category: String,
    @ColumnInfo(name = "sms_key") val smsKey: String?,
) {
    fun toDomain() = LedgerTransaction(id, accountId, TransactionKind.valueOf(kind), minor, epochDay, note, destinationId, category, smsKey)
}

@Entity(
    tableName = "finance_recurring",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("account_id"), Index("next_epoch_day")],
)
data class RecurringEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val minor: Long,
    val note: String,
    val interval: String,
    @ColumnInfo(name = "anchor_day") val anchorDay: Int,
    @ColumnInfo(name = "next_epoch_day") val nextEpochDay: Long?,
    @ColumnInfo(defaultValue = "'EXPENSE'") val kind: String,
    @ColumnInfo(defaultValue = "1") val every: Int,
    @ColumnInfo(defaultValue = "0") val paused: Boolean,
    @ColumnInfo(defaultValue = "0") val variable: Boolean,
    @ColumnInfo(defaultValue = "''") val category: String,
) {
    fun toDomain() = RecurringPayment(
        id, accountId, minor, note, PaymentInterval.valueOf(interval), anchorDay, nextEpochDay,
        TransactionKind.valueOf(kind), every, paused, variable, category,
    )
}

@Entity(
    tableName = "finance_budgets",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("account_id")],
)
data class CategoryBudgetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val category: String,
    val minor: Long,
    val interval: String,
    val every: Int,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
) {
    fun toDomain() = CategoryBudget(id, accountId, category, minor, PaymentInterval.valueOf(interval), every, startEpochDay)
}

internal fun Account.toEntity() = AccountEntity(id, name, currency, openingMinor, monthlyBudgetMinor)

internal fun RecurringPayment.toEntity() =
    RecurringEntity(id, accountId, minor, note, interval.name, anchorDay, nextEpochDay, kind.name, every, paused, variable, category)

internal fun CategoryBudget.toEntity() = CategoryBudgetEntity(id, accountId, category, minor, interval.name, every, startEpochDay)

internal fun LedgerTransaction.toEntity() =
    TransactionEntity(id, accountId, kind.name, minor, epochDay, note, destinationId, category, smsKey)

data class AccountWithTransactions(
    @Embedded val account: AccountEntity,
    @Relation(parentColumn = "id", entityColumn = "account_id") val transactions: List<TransactionEntity>,
    @Relation(parentColumn = "id", entityColumn = "account_id") val recurring: List<RecurringEntity>,
    @Relation(parentColumn = "id", entityColumn = "account_id") val budgets: List<CategoryBudgetEntity>,
)

@Dao
interface FinanceDao {
    @Transaction
    @Query("SELECT * FROM finance_accounts ORDER BY name COLLATE NOCASE, id")
    fun observeLedger(): Flow<List<AccountWithTransactions>>

    @Query("SELECT * FROM finance_accounts ORDER BY id")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM finance_transactions ORDER BY epoch_day DESC, id")
    suspend fun transactions(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM finance_accounts")
    suspend fun accountCount(): Int

    @Query("SELECT COUNT(*) FROM finance_transactions")
    suspend fun transactionCount(): Int

    @Query("SELECT * FROM finance_accounts WHERE id = :id")
    suspend fun account(id: String): AccountEntity?

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("""
        DELETE FROM finance_accounts WHERE id = :id
        AND NOT EXISTS (SELECT 1 FROM finance_transactions WHERE account_id = :id OR destination_id = :id)
        AND NOT EXISTS (SELECT 1 FROM finance_recurring WHERE account_id = :id)
        AND NOT EXISTS (SELECT 1 FROM finance_budgets WHERE account_id = :id)
    """)
    suspend fun deleteAccount(id: String): Int

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM finance_transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM finance_accounts")
    suspend fun clearAccounts()

    @Insert
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert
    suspend fun insertRecurring(payments: List<RecurringEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDue(transactions: List<TransactionEntity>)

    @Query("SELECT * FROM finance_recurring ORDER BY id")
    suspend fun recurring(): List<RecurringEntity>

    @Query("SELECT * FROM finance_recurring WHERE paused = 0 AND variable = 0 AND next_epoch_day <= :throughDay ORDER BY next_epoch_day, id")
    suspend fun due(throughDay: Long): List<RecurringEntity>

    @Query("UPDATE finance_recurring SET next_epoch_day = :nextDay WHERE id = :id")
    suspend fun advance(id: String, nextDay: Long?)

    @Query("DELETE FROM finance_recurring WHERE id = :id")
    suspend fun deleteRecurring(id: String)

    @Query("DELETE FROM finance_recurring")
    suspend fun clearRecurring()

    @Query("SELECT * FROM finance_recurring WHERE id = :id")
    suspend fun recurring(id: String): RecurringEntity?

    @Upsert
    suspend fun upsert(payment: RecurringEntity)

    @Query("SELECT id FROM finance_transactions WHERE sms_key = :key")
    suspend fun importedSms(key: String): String?

    @Query("SELECT * FROM finance_budgets ORDER BY id")
    suspend fun budgets(): List<CategoryBudgetEntity>

    @Upsert
    suspend fun upsert(budget: CategoryBudgetEntity)

    @Query("DELETE FROM finance_budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)

    @Query("DELETE FROM finance_budgets")
    suspend fun clearBudgets()

    @Insert
    suspend fun insertBudgets(budgets: List<CategoryBudgetEntity>)
}
