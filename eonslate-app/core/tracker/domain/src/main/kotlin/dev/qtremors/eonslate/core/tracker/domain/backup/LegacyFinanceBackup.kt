package dev.qtremors.eonslate.core.tracker.domain.backup

import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryKind
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentInterval
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Services
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64

/** The sections included in a v2.0.0 finance export. */
data class LegacyFinance(
    val transactions: Boolean,
    val budgets: Boolean,
    val subscriptions: Boolean,
    val categories: Boolean,
) {
    fun applyTo(imported: BackupSnapshot, current: BackupSnapshot): BackupSnapshot {
        val full = transactions && budgets && subscriptions
        val incoming = imported.accounts.singleOrNull()
        val existing = if (full) null else current.accounts.singleOrNull { it.currency == incoming?.currency }
        val accountId = existing?.id ?: incoming?.id
        val accounts = when {
            full -> imported.accounts
            incoming == null || existing != null -> current.accounts
            else -> (current.accounts + incoming).distinctBy(Account::id)
        }
        return current.copy(
            accounts = accounts,
            transactions = if (transactions) imported.transactions.map { it.copy(accountId = checkNotNull(accountId)) } else current.transactions,
            budgets = if (budgets) imported.budgets.map { it.copy(accountId = checkNotNull(accountId)) } else current.budgets,
            recurring = if (subscriptions) imported.recurring.map { it.copy(accountId = checkNotNull(accountId)) } else current.recurring,
            categories = if (categories) imported.categories else current.categories,
            icons = if (subscriptions) imported.icons else current.icons,
        )
    }
}

object LegacyFinanceBackup {
    private const val VERSION = "2.0.0"
    private const val MAX_FILE_BYTES = 25 * 1024 * 1024
    const val MAX_ICON_BYTES = 2 * 1024 * 1024
    private const val MAX_TOTAL_ICON_BYTES = 10 * 1024 * 1024
    private val jsonWhitespace = setOf(9, 10, 13, 32)
    private val json = Json { ignoreUnknownKeys = true }

    fun looksLikeJson(bytes: ByteArray): Boolean {
        val start = if (bytes.size >= 3 && bytes[0] == 0xef.toByte() &&
            bytes[1] == 0xbb.toByte() && bytes[2] == 0xbf.toByte()) 3 else 0
        return (start until bytes.size).firstOrNull { bytes[it].toInt() !in jsonWhitespace }
            ?.let { bytes[it] == '{'.code.toByte() } == true
    }

    fun decode(bytes: ByteArray, schema: Int): BackupSnapshot {
        if (bytes.size > MAX_FILE_BYTES) throw BackupException(BackupProblem.DAMAGED)
        val legacy = try {
            json.decodeFromString<Envelope>(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
        } catch (error: SerializationException) {
            throw BackupException(BackupProblem.DAMAGED, error)
        }
        if (legacy.version != VERSION || (legacy.transactions == null && legacy.budgets == null &&
                legacy.subscriptions == null && legacy.settings?.customCategories == null)
        ) throw BackupException(BackupProblem.NOT_A_BACKUP)
        return try {
            convert(legacy, schema).also { BackupCheck.requireSound(it, schema) }
        } catch (error: IllegalArgumentException) {
            throw BackupException(BackupProblem.INCONSISTENT, error)
        } catch (error: ArithmeticException) {
            throw BackupException(BackupProblem.INCONSISTENT, error)
        } catch (error: DateTimeException) {
            throw BackupException(BackupProblem.INCONSISTENT, error)
        }
    }

    private fun convert(legacy: Envelope, schema: Int): BackupSnapshot {
        val currency = legacy.settings?.currency ?: "INR"
        require(MoneyAmount.isKnownCurrency(currency))
        val account = Account("legacy-finance-$currency", currency, currency)
        val assets = icons(legacy.androidAssets.orEmpty(), legacy.subscriptions.orEmpty().mapTo(HashSet()) { it.id })
        val transactions = legacy.transactions.orEmpty().map {
            require(it.description.isNotBlank())
            Transaction(
                it.id, account.id, it.type.kind, minor(it.amount.content.toBigDecimal().abs(), currency),
                day(it.date).toEpochDay(),
                note = it.description.trim(),
                category = it.category.trim(),
                details = it.notes?.trim().orEmpty(),
            )
        }
        val budgets = legacy.budgets.orEmpty().map {
            CategoryBudget(
                it.id, account.id, it.category.trim(), minor(it.limit.content.toBigDecimal(), currency), it.period.budgetInterval,
                it.period.budgetCount, day(it.periodStartDate).toEpochDay(), it.name.trim().also { name -> require(name.isNotBlank()) },
                it.icon, color(it.color),
            )
        }
        val recurring = legacy.subscriptions.orEmpty().map {
            val next = day(it.nextBilling)
            val time = if (it.cycle.unit != UnitOfTime.hour) null
                else if ('T' in it.nextBilling) LocalDateTime.parse(it.nextBilling).toLocalTime()
                else next.atStartOfDay().toLocalTime()
            val service = Services.all.firstOrNull { service -> service.name.equals(it.name.trim(), ignoreCase = true) }
            val legacyIcon = financeIconName(it.icon)
            RecurringPayment(
                id = it.id, accountId = account.id, minor = minor(it.amount.content.toBigDecimal(), currency),
                note = it.name.trim().also { name -> require(name.isNotBlank()) },
                interval = it.cycle.interval, anchorDay = next.dayOfMonth, nextEpochDay = next.toEpochDay(),
                kind = it.type.kind, every = it.cycle.count, paused = !it.active, variable = it.isVariable,
                category = if (it.type == Kind.income) "Income" else "Subscriptions",
                startEpochDay = it.createdAt?.let { value -> day(value).toEpochDay() },
                details = it.notes?.trim().orEmpty(),
                icon = assets[it.id]?.let { asset -> "file:${asset.first}" }
                    ?: legacyIcon
                    ?: service?.let { item -> Services.PREFIX + item.id },
                color = color(it.color),
                nextMinute = time?.let { value -> value.hour * 60 + value.minute },
            )
        }
        val categories = legacy.settings?.customCategories.orEmpty().map {
            CategoryChoice(it.name.trim(), it.type.category, it.id, it.icon, color(it.color))
        }
        val sections = LegacyFinance(
            legacy.transactions != null, legacy.budgets != null, legacy.subscriptions != null,
            legacy.settings?.customCategories != null,
        )
        return BackupSnapshot(
            schema, legacy.exportDate?.let { Instant.parse(it).toEpochMilli() } ?: 0L, VERSION,
            accounts = if (sections.transactions || sections.budgets || sections.subscriptions) listOf(account) else emptyList(),
            transactions = transactions,
            budgets = budgets,
            recurring = recurring,
            categories = categories,
            icons = assets.values.associate { (hash, data) -> hash to data },
            legacy = sections,
        )
    }

    private fun financeIconName(icon: String?): String? = icon?.takeIf {
        it in setOf("Wallet", "CreditCard", "Restaurant", "DirectionsCar", "Movie", "ShoppingCart",
            "Lightbulb", "Favorite", "Work", "Home", "FitnessCenter", "Cloud", "Tag", "AutoAwesome")
    }?.let { "symbol:$it" }

    private fun day(value: String): LocalDate = LocalDate.parse(value.take(10))

    private fun color(hex: String?): Int? = hex?.let {
        require(it.matches(Regex("#[0-9a-fA-F]{6}")))
        it.drop(1).toInt(16)
    }

    private fun minor(value: BigDecimal, currency: String): Long {
        require(value.precision() <= 40 && value.scale() in -20..40)
        return value.movePointRight(MoneyAmount.fractionDigits(currency)).setScale(0, RoundingMode.HALF_UP)
            .longValueExact().also { require(it > 0) }
    }

    private fun icons(rows: List<Asset>, ids: Set<String>): Map<String, Pair<String, String>> {
        require(rows.size <= 100 && rows.map(Asset::entityId).distinct().size == rows.size)
        var total = 0L
        return rows.associate {
            require(it.entityId in ids && it.mimeType in setOf("image/svg+xml", "image/png", "image/jpeg", "image/webp"))
            val bytes = Base64.getDecoder().decode(it.data)
            require(bytes.isNotEmpty() && bytes.size <= MAX_ICON_BYTES)
            total += bytes.size
            require(total <= MAX_TOTAL_ICON_BYTES)
            if (it.mimeType == "image/svg+xml") {
                val svg = bytes.toString(Charsets.UTF_8)
                require("<svg" in svg && "<!doctype" !in svg.lowercase() && "<!entity" !in svg.lowercase() && "<script" !in svg.lowercase())
            }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte -> "%02x".format(byte) }
            it.entityId to (hash to it.data)
        }
    }

    @Serializable
    private data class Envelope(
        val version: String,
        val exportDate: String? = null,
        val settings: Settings? = null,
        val transactions: List<LegacyTransaction>? = null,
        val budgets: List<Budget>? = null,
        val subscriptions: List<Subscription>? = null,
        val androidAssets: List<Asset>? = null,
    )

    @Serializable
    private data class Settings(val currency: String = "INR", val customCategories: List<Category>? = null)

    @Serializable
    private data class LegacyTransaction(
        val id: String,
        val description: String,
        val amount: JsonPrimitive,
        val type: Kind,
        val date: String,
        val category: String,
        val notes: String? = null,
    )

    @Serializable
    private data class Budget(
        val id: String,
        val name: String,
        val limit: JsonPrimitive,
        val category: String,
        val periodStartDate: String,
        val period: Cycle = Cycle(),
        val icon: String = "Wallet",
        val color: String? = null,
    )

    @Serializable
    private data class Subscription(
        val id: String,
        val name: String,
        val amount: JsonPrimitive,
        val nextBilling: String,
        val cycle: Cycle = Cycle(),
        val active: Boolean = true,
        val isVariable: Boolean = false,
        val type: Kind = Kind.expense,
        val notes: String? = null,
        val icon: String? = null,
        val color: String? = null,
        val createdAt: String? = null,
    )

    @Serializable
    private data class Category(
        val id: String,
        val name: String,
        val type: Kind = Kind.both,
        val icon: String = "Tag",
        val color: String? = null,
    )

    @Serializable
    private data class Asset(val entityId: String, val mimeType: String, val data: String)

    @Serializable
    private enum class Kind {
        income, expense, both;

        val kind: TransactionKind get() = when (this) {
            income -> TransactionKind.INCOME
            expense -> TransactionKind.EXPENSE
            both -> throw IllegalArgumentException("Invalid transaction type")
        }

        val category: CategoryKind get() = when (this) {
            income -> CategoryKind.INCOME
            expense -> CategoryKind.EXPENSE
            both -> CategoryKind.BOTH
        }
    }

    @Serializable
    private enum class UnitOfTime { hour, day, week, month, year }

    @Serializable
    private data class Cycle(val count: Int = 1, val unit: UnitOfTime = UnitOfTime.month) {
        val interval: PaymentInterval get() = when (unit) {
            UnitOfTime.hour -> PaymentInterval.HOURLY
            UnitOfTime.day -> PaymentInterval.DAILY
            UnitOfTime.week -> PaymentInterval.WEEKLY
            UnitOfTime.month -> PaymentInterval.MONTHLY
            UnitOfTime.year -> PaymentInterval.YEARLY
        }

        val budgetInterval get() = if (unit == UnitOfTime.hour) PaymentInterval.DAILY else interval
        val budgetCount: Int get() {
            require(count in 1..10_000)
            return if (unit == UnitOfTime.hour) (count + 23) / 24 else count
        }
    }
}
