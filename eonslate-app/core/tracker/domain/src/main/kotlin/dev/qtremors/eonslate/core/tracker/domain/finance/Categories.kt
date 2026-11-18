package dev.qtremors.eonslate.core.tracker.domain.finance

import java.util.Locale

/** Which kind of money a category is for. */
enum class CategoryKind { EXPENSE, INCOME, BOTH }

/** A category name with the kind it suits. Names are data, as people type and see them. */
data class CategoryChoice(val name: String, val kind: CategoryKind)

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

    private fun fits(choice: CategoryKind, kind: TransactionKind) = when (kind) {
        TransactionKind.INCOME -> choice != CategoryKind.EXPENSE
        TransactionKind.EXPENSE -> choice != CategoryKind.INCOME
        TransactionKind.TRANSFER -> false
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
        val seen = HashSet<String>()
        return (used + defaults.filter { fits(it.kind, kind) }.map { it.name })
            .filter { seen.add(it.lowercase(Locale.ROOT)) }
    }
}
