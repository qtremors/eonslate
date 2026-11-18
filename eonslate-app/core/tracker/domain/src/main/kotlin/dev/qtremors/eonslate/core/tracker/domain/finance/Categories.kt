package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.Serializable
import java.util.Locale

/** Which kind of money a category is for. */
@Serializable
enum class CategoryKind {
    EXPENSE, INCOME, BOTH;

    fun fits(kind: TransactionKind) = when (kind) {
        TransactionKind.INCOME -> this != EXPENSE
        TransactionKind.EXPENSE -> this != INCOME
        TransactionKind.TRANSFER -> false
    }
}

/** A category name with the kind it suits. Names are data, as people type and see them. */
@Serializable
data class CategoryChoice(
    val name: String,
    val kind: CategoryKind,
    val id: String = "category:${name.trim().lowercase(Locale.ROOT)}",
    val icon: String? = null,
    val color: Int? = null,
) {
    val key: String get() = name.trim().lowercase(Locale.ROOT)

    fun isValid(): Boolean =
        id.isNotBlank() && id.length <= 150 && name.isNotBlank() && name == name.trim() &&
            name.length <= Transaction.CATEGORY_LIMIT &&
            (icon == null || icon.isNotBlank() && icon.length <= 64) &&
            (color == null || color in 0..0xFFFFFF)
}

object Categories {
    /** Offered before anything is used, so the first transaction needs no typing. */
    val defaults: List<CategoryChoice> = listOf(
        CategoryChoice("Food & Dining", CategoryKind.EXPENSE),
        CategoryChoice("Transport", CategoryKind.EXPENSE),
        CategoryChoice("Shopping", CategoryKind.EXPENSE),
        CategoryChoice("Bills", CategoryKind.EXPENSE),
        CategoryChoice("Health", CategoryKind.EXPENSE),
        CategoryChoice("Entertainment", CategoryKind.EXPENSE),
        CategoryChoice("Subscriptions", CategoryKind.EXPENSE),
        CategoryChoice("Salary", CategoryKind.INCOME),
        CategoryChoice("Other", CategoryKind.BOTH),
    )

    fun all(ledger: Ledger): List<CategoryChoice> {
        val catalog = ledger.categories.ifEmpty { defaults }
        val used = (ledger.transactions.asSequence().map { it.category to it.kind } +
            ledger.recurring.asSequence().map { it.category to it.kind } +
            ledger.budgets.asSequence().map { it.category to TransactionKind.EXPENSE })
            .filter { (name, kind) -> name.isNotBlank() && kind != TransactionKind.TRANSFER }
            .groupBy { (name, _) -> name.trim().lowercase(Locale.ROOT) }
            .values.map { uses ->
                val kind = when (uses.map { it.second }.distinct().singleOrNull()) {
                    TransactionKind.INCOME -> CategoryKind.INCOME
                    TransactionKind.EXPENSE -> CategoryKind.EXPENSE
                    else -> CategoryKind.BOTH
                }
                CategoryChoice(uses.first().first.trim(), kind)
            }
        return (catalog + used).distinctBy(CategoryChoice::key)
    }

    /**
     * What to offer for a [kind] of money: categories already used for it, most used first, then
     * the defaults that suit it. Each name once, whatever its case; the first spelling wins.
     */
    fun suggestions(ledger: Ledger, kind: TransactionKind): List<String> {
        if (kind == TransactionKind.TRANSFER) return emptyList()
        val used = (ledger.transactions.asSequence().filter { it.kind == kind }.map { it.category } +
            ledger.recurring.asSequence().filter { it.kind == kind }.map { it.category } +
            (if (kind == TransactionKind.EXPENSE) ledger.budgets.asSequence().map { it.category } else emptySequence()))
            .map { it.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
        val catalog = all(ledger).associateBy(CategoryChoice::key)
        val saved = ledger.categories.associateBy(CategoryChoice::key)
        val usedNames = used.associateBy { it.lowercase(Locale.ROOT) }
        val keys = used.map { it.lowercase(Locale.ROOT) } + catalog.keys
        return keys.distinct().mapNotNull { key ->
            val choice = saved[key]
            if (choice != null) choice.name.takeIf { choice.kind.fits(kind) }
            else usedNames[key] ?: catalog[key]?.takeIf { it.kind.fits(kind) }?.name
        }
    }
}
