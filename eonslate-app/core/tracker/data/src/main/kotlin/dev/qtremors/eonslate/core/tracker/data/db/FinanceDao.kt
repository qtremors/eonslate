package dev.qtremors.eonslate.core.tracker.data.db

import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyMove
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyBank
import dev.qtremors.eonslate.core.tracker.domain.finance.PartyKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
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
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryKind
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction as LedgerTransaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Compounding
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposit

@Entity(tableName = "finance_accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String,
    @ColumnInfo(name = "opening_minor") val openingMinor: Long,
    @ColumnInfo(name = "monthly_budget_minor") val monthlyBudgetMinor: Long?,
    @ColumnInfo(defaultValue = "bank") val kind: String = AccountKind.BANK.storageKey,
    val digits: String? = null,
    @ColumnInfo(name = "floor_minor") val floorMinor: Long? = null,
    // A fixed deposit's terms, all set or all null.
    @ColumnInfo(name = "deposit_principal_minor") val depositPrincipalMinor: Long? = null,
    @ColumnInfo(name = "deposit_rate_bp") val depositRateBp: Int? = null,
    @ColumnInfo(name = "deposit_start_epoch_day") val depositStartEpochDay: Long? = null,
    @ColumnInfo(name = "deposit_maturity_epoch_day") val depositMaturityEpochDay: Long? = null,
    @ColumnInfo(name = "deposit_compounding") val depositCompounding: String? = null,
    @ColumnInfo(name = "deposit_closed_epoch_day") val depositClosedEpochDay: Long? = null,
) {
    fun toDomain() = Account(
        id, name, currency, openingMinor, monthlyBudgetMinor, AccountKind.fromKey(kind), digits, floorMinor,
        deposit = if (depositPrincipalMinor == null || depositRateBp == null || depositStartEpochDay == null || depositMaturityEpochDay == null) null
        else Deposit(
            depositPrincipalMinor, depositRateBp, depositStartEpochDay, depositMaturityEpochDay,
            Compounding.fromKey(depositCompounding), depositClosedEpochDay,
        ),
    )
}

@Entity(
    tableName = "finance_transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["destination_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("account_id"), Index("destination_id"), Index(value = ["sms_key"], unique = true), Index("party_id")],
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
    @ColumnInfo(defaultValue = "''") val details: String = "",
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0,
    @ColumnInfo(name = "party_id") val partyId: String? = null,
    @ColumnInfo(defaultValue = "0") val pending: Boolean = false,
    @ColumnInfo(defaultValue = "'[]'") val shares: String = "[]",
) {
    fun toDomain() = LedgerTransaction(
        id, accountId, TransactionKind.valueOf(kind), minor, epochDay, note, destinationId, category, smsKey, details, createdAt, partyId, pending,
        ShareColumn.decode(shares),
    )
}

/** Money set aside inside an account for a goal. */
@Entity(
    tableName = "finance_piggies",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("account_id")],
)
data class PiggyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val name: String,
    @ColumnInfo(name = "target_minor") val targetMinor: Long,
    @ColumnInfo(name = "target_epoch_day") val targetEpochDay: Long?,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long,
) {
    fun toDomain() = PiggyBank(id, accountId, name, targetMinor, targetEpochDay, createdAt)
}

/** Money put into a piggy bank, or taken out when negative. */
@Entity(
    tableName = "finance_piggy_moves",
    foreignKeys = [ForeignKey(entity = PiggyEntity::class, parentColumns = ["id"], childColumns = ["piggy_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("piggy_id")],
)
data class PiggyMoveEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "piggy_id") val piggyId: String,
    val minor: Long,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long,
) {
    fun toDomain() = PiggyMove(id, piggyId, minor, epochDay, createdAt)
}

internal fun PiggyBank.toEntity() = PiggyEntity(id, accountId, name.trim(), targetMinor, targetEpochDay, createdAt)
internal fun PiggyMove.toEntity() = PiggyMoveEntity(id, piggyId, minor, epochDay, createdAt)

/** A person or business payments are with; [nameKey] keeps names unique whatever their case. */
@Entity(tableName = "finance_parties", indices = [Index(value = ["name_key"], unique = true)])
data class PartyEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    @ColumnInfo(defaultValue = "'person'") val kind: String,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long,
) {
    fun toDomain() = Party(id, name, if (kind == "business") PartyKind.BUSINESS else PartyKind.PERSON, createdAt)
}

internal fun Party.toEntity() =
    PartyEntity(id, name.trim(), key, if (kind == PartyKind.BUSINESS) "business" else "person", createdAt)

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
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long? = null,
    @ColumnInfo(defaultValue = "''") val details: String = "",
    val icon: String? = null,
    val color: Int? = null,
    @ColumnInfo(name = "next_minute") val nextMinute: Int? = null,
    @ColumnInfo(name = "auto_estimate", defaultValue = "0") val autoEstimate: Boolean = false,
) {
    fun toDomain() = RecurringPayment(
        id, accountId, minor, note, PaymentInterval.valueOf(interval), anchorDay, nextEpochDay,
        TransactionKind.valueOf(kind), every, paused, variable, category, startEpochDay, details, icon, color, nextMinute, autoEstimate,
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
    @ColumnInfo(defaultValue = "''") val name: String,
    val icon: String?,
    val color: Int?,
    @ColumnInfo(name = "all_accounts", defaultValue = "0") val allAccounts: Boolean = false,
) {
    fun toDomain() =
        CategoryBudget(id, accountId, category, minor, PaymentInterval.valueOf(interval), every, startEpochDay, name, icon, color, allAccounts)
}

@Entity(tableName = "finance_categories", indices = [Index(value = ["name_key"], unique = true)])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    val kind: String,
    val icon: String?,
    val color: Int?,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
) {
    fun toDomain() = CategoryChoice(name, CategoryKind.valueOf(kind), id, icon, color, deleted)
}

internal fun Account.toEntity() = AccountEntity(
    id, name, currency, openingMinor, monthlyBudgetMinor, kind.storageKey, digits, floorMinor,
    deposit?.principalMinor, deposit?.rateBp, deposit?.startEpochDay, deposit?.maturityEpochDay,
    deposit?.compounding?.storageKey, deposit?.closedEpochDay,
)

internal fun RecurringPayment.toEntity() =
    RecurringEntity(
        id, accountId, minor, note, interval.name, anchorDay, nextEpochDay, kind.name, every, paused, variable, category,
        startEpochDay, details, icon, color, nextMinute, autoEstimate,
    )

internal fun CategoryBudget.toEntity() =
    CategoryBudgetEntity(id, accountId, category, minor, interval.name, every, startEpochDay, name, icon, color, allAccounts)

internal fun CategoryChoice.toEntity() = CategoryEntity(id, name, key, kind.name, icon, color, deleted)

internal fun LedgerTransaction.toEntity() =
    TransactionEntity(id, accountId, kind.name, minor, epochDay, note, destinationId, category, smsKey, details, createdAt, partyId, pending, ShareColumn.encode(shares))

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
    suspend fun ledger(): List<AccountWithTransactions>

    @Query("SELECT * FROM finance_accounts ORDER BY id")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM finance_transactions ORDER BY epoch_day DESC, created_at DESC, id")
    suspend fun transactions(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM finance_accounts")
    suspend fun accountCount(): Int

    @Query("SELECT COUNT(*) FROM finance_transactions")
    suspend fun transactionCount(): Int

    @Query("SELECT COUNT(*) FROM finance_recurring")
    suspend fun recurringCount(): Int

    @Query("SELECT COUNT(*) FROM finance_budgets")
    suspend fun budgetCount(): Int

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
        AND NOT EXISTS (SELECT 1 FROM finance_piggies WHERE account_id = :id)
    """)
    suspend fun deleteAccount(id: String): Int

    @Query("DELETE FROM finance_transactions WHERE account_id = :id OR destination_id = :id")
    suspend fun deleteTransactionsOf(id: String)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM finance_transactions WHERE id IN (:ids)")
    suspend fun deleteTransactions(ids: List<String>)

    @Query("SELECT created_at FROM finance_transactions WHERE id = :id")
    suspend fun createdAt(id: String): Long?

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

    @Query("""
        SELECT * FROM finance_recurring WHERE paused = 0 AND (variable = 0 OR auto_estimate = 1)
          AND (next_epoch_day < :throughDay OR (next_epoch_day = :throughDay AND COALESCE(next_minute, 0) <= :throughMinute))
        ORDER BY next_epoch_day, next_minute, id
    """)
    suspend fun due(throughDay: Long, throughMinute: Int): List<RecurringEntity>

    @Query("UPDATE finance_recurring SET next_epoch_day = :nextDay, next_minute = :minute WHERE id = :id")
    suspend fun advance(id: String, nextDay: Long?, minute: Int?)

    @Query("DELETE FROM finance_recurring WHERE id = :id")
    suspend fun deleteRecurring(id: String)

    @Query("DELETE FROM finance_recurring")
    suspend fun clearRecurring()

    @Query("SELECT * FROM finance_recurring WHERE id = :id")
    suspend fun recurring(id: String): RecurringEntity?

    @Upsert
    suspend fun upsert(payment: RecurringEntity)

    /** Transactions already in the ledger that a posting would repeat: same name, day and kind. */
    @Query("""
        SELECT COUNT(*) FROM finance_transactions
        WHERE epoch_day = :day AND kind = :kind AND note = :note COLLATE NOCASE AND id != :id
    """)
    suspend fun matching(note: String, day: Long, kind: String, id: String): Int

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

    @Query("SELECT * FROM finance_categories ORDER BY name COLLATE NOCASE, id")
    suspend fun categories(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Insert
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM finance_categories")
    suspend fun clearCategories()

    /** Drops a deleted category's marker when its name is used for a new one. */
    @Query("DELETE FROM finance_categories WHERE name_key = :key AND id != :id AND deleted = 1")
    suspend fun forgetDeleted(key: String, id: String)

    @Query("SELECT * FROM finance_piggies ORDER BY created_at, id")
    suspend fun piggies(): List<PiggyEntity>

    @Query("SELECT * FROM finance_piggy_moves ORDER BY epoch_day, created_at, id")
    suspend fun piggyMoves(): List<PiggyMoveEntity>

    @Upsert
    suspend fun upsert(piggy: PiggyEntity)

    @Upsert
    suspend fun upsert(move: PiggyMoveEntity)

    @Insert
    suspend fun insertPiggies(piggies: List<PiggyEntity>)

    @Insert
    suspend fun insertPiggyMoves(moves: List<PiggyMoveEntity>)

    @Query("DELETE FROM finance_piggies WHERE id = :id")
    suspend fun deletePiggy(id: String)

    @Query("DELETE FROM finance_piggy_moves WHERE id = :id")
    suspend fun deletePiggyMove(id: String)

    @Query("DELETE FROM finance_piggy_moves")
    suspend fun clearPiggyMoves()

    @Query("DELETE FROM finance_piggies")
    suspend fun clearPiggies()

    @Query("SELECT * FROM finance_parties ORDER BY name COLLATE NOCASE, id")
    suspend fun parties(): List<PartyEntity>

    @Query("SELECT * FROM finance_parties WHERE id = :id")
    suspend fun party(id: String): PartyEntity?

    @Query("SELECT id FROM finance_parties WHERE name_key = :key")
    suspend fun partyByKey(key: String): String?

    @Upsert
    suspend fun upsert(party: PartyEntity)

    @Insert
    suspend fun insertParties(parties: List<PartyEntity>)

    @Query("SELECT COUNT(*) FROM finance_transactions WHERE shares LIKE '%' || :id || '%'")
    suspend fun sharesNaming(id: String): Int

    @Query("UPDATE finance_transactions SET party_id = NULL WHERE party_id = :id")
    suspend fun forgetParty(id: String)

    @Query("DELETE FROM finance_parties WHERE id = :id")
    suspend fun deleteParty(id: String)

    @Query("DELETE FROM finance_parties")
    suspend fun clearParties()

    @Query("UPDATE finance_transactions SET category = :name WHERE category IN (:previous)")
    suspend fun renameTransactionCategories(previous: List<String>, name: String)

    @Query("UPDATE finance_recurring SET category = :name WHERE category IN (:previous)")
    suspend fun renameRecurringCategories(previous: List<String>, name: String)

    @Query("UPDATE finance_budgets SET category = :name WHERE category IN (:previous)")
    suspend fun renameBudgetCategories(previous: List<String>, name: String)
}
