package dev.qtremors.eonslate.core.tracker.domain.csv

import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.monthlyMinor
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.io.Writer
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Subscriptions and recurring income: what each costs or pays, how often, when next, and per month. */
object RecurringCsv {
    private val columns = listOf(
        "name", "kind", "amount", "currency", "account", "category", "interval", "every",
        "next_billing", "active", "variable", "monthly_equivalent",
    )

    fun write(ledger: Ledger, payments: List<RecurringPayment>, output: Writer) {
        val accounts = ledger.accounts.associateBy { it.id }
        Csv.writeRow(output, columns)
        for (payment in payments) {
            val account = accounts[payment.accountId] ?: continue
            val digits = MoneyAmount.fractionDigits(account.currency)
            val monthly = BigDecimal(payment.monthlyMinor()).movePointLeft(digits).setScale(digits, RoundingMode.HALF_UP)
            Csv.writeRow(
                output,
                listOf(
                    Csv.escapeText(payment.note),
                    payment.kind.name.lowercase(Locale.ROOT),
                    MoneyAmount.format(payment.minor, account.currency),
                    account.currency,
                    Csv.escapeText(account.name),
                    Csv.escapeText(payment.category),
                    payment.interval.name.lowercase(Locale.ROOT),
                    payment.every.toString(),
                    payment.nextEpochDay?.let(EpochDays::format).orEmpty(),
                    (!payment.paused).toString(),
                    payment.variable.toString(),
                    monthly.toPlainString(),
                ),
            )
        }
    }
}
