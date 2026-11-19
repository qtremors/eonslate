package dev.qtremors.eonslate.core.tracker.domain.csv

import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.io.Writer
import java.math.BigInteger
import java.time.LocalDate
import java.util.Locale

/** Budgets as measured on one day: each limit, what its current period has spent, and how much is left. */
object BudgetCsv {
    private val columns = listOf(
        "name", "category", "account", "currency", "limit", "spent", "remaining", "utilization",
        "interval", "every", "period_start", "period_end", "all_accounts",
    )

    fun write(ledger: Ledger, day: LocalDate, output: Writer) {
        Csv.writeRow(output, columns)
        for (health in ledger.budgetHealth(day)) {
            val budget = health.budget
            val period = health.period
            // Spending can pass what one Long holds, so amounts are written from the exact sum.
            fun money(minor: BigInteger) =
                minor.toBigDecimal().movePointLeft(MoneyAmount.fractionDigits(health.currency)).toPlainString()
            Csv.writeRow(
                output,
                listOf(
                    Csv.escapeText(health.title),
                    Csv.escapeText(budget?.category.orEmpty()),
                    Csv.escapeText(ledger.accounts.firstOrNull { it.id == (budget?.accountId ?: health.account?.id) }?.name.orEmpty()),
                    health.currency,
                    money(health.limit),
                    money(health.spent),
                    money(health.remaining),
                    "${health.percent}%",
                    (budget?.interval?.name ?: "MONTHLY").lowercase(Locale.ROOT),
                    (budget?.every ?: 1).toString(),
                    period?.let { EpochDays.format(it.first) }.orEmpty(),
                    period?.let { EpochDays.format(it.last) }.orEmpty(),
                    (budget?.allAccounts ?: false).toString(),
                ),
            )
        }
    }
}
