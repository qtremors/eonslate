package dev.qtremors.eonslate.core.tracker.domain.finance

import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class SubscriptionSpending(
    val recorded: BigInteger,
    val estimatedSinceStart: BigInteger?,
    val history: Map<YearMonth, BigInteger>,
    val forecast: Map<YearMonth, BigInteger>,
    val paymentCount: Int,
) {
    val projected: BigInteger get() = forecast.values.fold(BigInteger.ZERO, BigInteger::add)

    companion object {
        fun of(payment: RecurringPayment, ledger: Ledger, today: LocalDate): SubscriptionSpending {
            val accounts = ledger.accounts.associateBy(Account::id)
            val currency = accounts.getValue(payment.accountId).currency
            val unambiguousName = payment.note.isNotBlank() && ledger.recurring.count {
                it.accountId == payment.accountId && it.kind == payment.kind && it.note.equals(payment.note, ignoreCase = true)
            } == 1
            val legacyNotes = if (unambiguousName) listOf(
                "${payment.note} · Recurring subscription payment",
                "${payment.note} · Recurring income payout",
                "${payment.note} · Recurring income (variable estimate)",
            ) else emptyList()
            val payments = ledger.transactions.filter {
                it.epochDay <= today.toEpochDay() && it.kind == payment.kind &&
                    accounts[it.accountId]?.currency == currency &&
                    (it.id.startsWith(payment.transactionIdPrefix) ||
                        (it.accountId == payment.accountId && legacyNotes.any { note -> note.equals(it.note, ignoreCase = true) }))
            }
            val history = months(YearMonth.from(today).minusMonths(11))
            for (entry in payments) {
                val month = YearMonth.from(LocalDate.ofEpochDay(entry.epochDay))
                if (month in history) history[month] = history.getValue(month) + BigInteger.valueOf(entry.minor)
            }

            val amount = BigInteger.valueOf(payment.minor)
            val estimate = payment.startEpochDay?.let(LocalDate::ofEpochDay)
                ?.takeIf { it <= today && !payment.variable && !payment.paused }
                ?.let { amount * BigInteger.valueOf(payment.estimatedOccurrences(it, today)) }

            val forecast = months(YearMonth.from(today))
            if (!payment.paused) {
                val limit = forecast.keys.last().atEndOfMonth()
                var next = payment.nextOccurrence
                if (next != null && next.toLocalDate() < today) {
                    next = payment.firstOnOrAfter(next, today)
                }
                while (next != null && next.toLocalDate() <= limit) {
                    val month = YearMonth.from(next)
                    forecast[month] = forecast.getValue(month) + amount
                    next = payment.after(next, next.toLocalDate().atTime(LocalTime.MAX))
                }
            }

            return SubscriptionSpending(
                payments.fold(BigInteger.ZERO) { total, entry -> total + BigInteger.valueOf(entry.minor) },
                estimate, history, forecast, payments.size,
            )
        }

        private fun months(first: YearMonth) =
            (0L..11L).associateTo(linkedMapOf()) { first.plusMonths(it) to BigInteger.ZERO }

        private fun RecurringPayment.firstOnOrAfter(next: LocalDateTime, today: LocalDate): LocalDateTime? {
            val date = next.toLocalDate()
            if (interval == PaymentInterval.HOURLY) {
                val hours = ChronoUnit.HOURS.between(next, today.atStartOfDay())
                val candidate = next.plusHours((hours / every) * every)
                return if (candidate.toLocalDate() >= today) candidate else after(candidate)
            }
            val count = elapsedCycles(date, today) / every
            val candidate = interval.advance(date, count * every, anchorDay).atStartOfDay()
            return if (candidate.toLocalDate() >= today) candidate else after(candidate)
        }

        private fun RecurringPayment.estimatedOccurrences(start: LocalDate, today: LocalDate): Long {
            if (interval == PaymentInterval.HOURLY) {
                val hours = ChronoUnit.HOURS.between(start.atStartOfDay(), today.atTime(LocalTime.MAX))
                return if (every <= 24) ChronoUnit.DAYS.between(start, today) + 1 else hours / every + 1
            }
            val cycles = elapsedCycles(start, today) / every
            return if (interval.advance(start, cycles * every, start.dayOfMonth) > today) cycles else cycles + 1
        }

        private fun RecurringPayment.elapsedCycles(start: LocalDate, today: LocalDate): Long = when (interval) {
            PaymentInterval.HOURLY -> error("Hourly schedules use time of day")
            PaymentInterval.DAILY -> ChronoUnit.DAYS.between(start, today)
            PaymentInterval.WEEKLY -> ChronoUnit.WEEKS.between(start, today)
            PaymentInterval.MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(today))
            PaymentInterval.YEARLY -> (today.year - start.year).toLong()
        }
    }
}
