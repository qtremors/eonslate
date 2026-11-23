package dev.qtremors.eonslate.core.tracker.domain.insights

import dev.qtremors.eonslate.core.tracker.domain.finance.OverallFloor
import dev.qtremors.eonslate.core.tracker.domain.finance.Floors
import dev.qtremors.eonslate.core.tracker.domain.finance.Breach
import dev.qtremors.eonslate.core.tracker.domain.finance.BudgetHealth
import dev.qtremors.eonslate.core.tracker.domain.finance.DepositStatus
import dev.qtremors.eonslate.core.tracker.domain.finance.Deposits
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import java.time.LocalDate

/** Something that wants attention, for the bell. Only [counts] ones add to its badge. */
sealed interface Alert {
    /** Payment messages found in SMS, waiting to be added. */
    data class FoundPayments(val count: Int) : Alert

    /** A payment that waits for its amount and has fallen due. */
    data class PaymentDue(val payment: RecurringPayment, val currency: String, val daysLate: Long) : Alert

    /** A payment coming up within [Alerts.SOON_DAYS] days; shown, not counted. */
    data class PaymentSoon(val payment: RecurringPayment, val currency: String, val daysLeft: Long) : Alert

    /** A budget spent past its limit this period. */
    data class OverBudget(val health: BudgetHealth) : Alert

    /** A balance under the least you set: an account's floor, or your overall one. */
    data class BelowFloor(val breach: Breach) : Alert

    /** A tracker's interval that has come round. */
    data class IntervalDue(val tracker: Tracker, val status: IntervalStatus) : Alert

    /**
     * A fixed deposit that has matured and waits to be closed, or one maturing within
     * [Alerts.DEPOSIT_DAYS] days, which is shown and not counted.
     */
    data class DepositMatures(val status: DepositStatus) : Alert

    val counts: Boolean get() = this !is PaymentSoon && !(this is DepositMatures && !status.matured)
}

object Alerts {
    const val SOON_DAYS = 3L
    const val DEPOSIT_DAYS = 7L

    /**
     * Everything waiting on [today], most pressing first: payments found in SMS, balances under
     * their floor, payments due, deposits matured, budgets over, intervals due, then payments
     * and deposits coming up. [intervals] is each tracker with
     * where its intervals stand.
     */
    fun of(
        ledger: Ledger,
        today: LocalDate,
        found: Int,
        intervals: List<Pair<Tracker, List<IntervalStatus>>>,
        overall: OverallFloor? = null,
    ): List<Alert> {
        val day = today.toEpochDay()
        val currencies = ledger.accounts.associate { it.id to it.currency }
        val live = ledger.recurring.filter { !it.paused && it.nextEpochDay != null && it.accountId in currencies }
        val due = live.filter { !it.postsAutomatically && it.nextEpochDay!! <= day }
            .sortedBy { it.nextEpochDay }
            .map { Alert.PaymentDue(it, currencies.getValue(it.accountId), day - it.nextEpochDay!!) }
        val soon = live.filter { it.nextEpochDay!! - day in 1..SOON_DAYS }
            .sortedBy { it.nextEpochDay }
            .map { Alert.PaymentSoon(it, currencies.getValue(it.accountId), it.nextEpochDay!! - day) }
        val over = ledger.budgetHealth(today).filter { it.over }.sortedByDescending { it.fraction }.map { Alert.OverBudget(it) }
        val intervalsDue = intervals.flatMap { (tracker, statuses) ->
            statuses.filter { it.due }.map { Alert.IntervalDue(tracker, it) }
        }
        val floors = Floors.below(ledger, overall).map { Alert.BelowFloor(it) }
        val (matured, maturing) = Deposits.status(ledger, day)
            .filter { it.open && it.daysLeft <= DEPOSIT_DAYS }
            .map { Alert.DepositMatures(it) }
            .partition { it.status.matured }
        return listOfNotNull(found.takeIf { it > 0 }?.let { Alert.FoundPayments(it) }) +
            floors + due + matured + over + intervalsDue + soon + maturing
    }
}
