package dev.qtremors.eonslate.core.tracker.domain.finance

import java.math.BigInteger

/** The least you want to keep overall, in one currency, as minor units. */
data class OverallFloor(val currency: String, val minor: Long)

/**
 * A balance under the least you set: an account's own floor, or with [account] null, your
 * overall floor across every account in [currency].
 */
data class Breach(val account: Account?, val currency: String, val balance: BigInteger, val floor: BigInteger) {
    /** How far under the floor it is. */
    val under: BigInteger get() = floor - balance
}

object Floors {
    /** Every account below its floor now, and the overall floor when the total is below it. */
    fun below(ledger: Ledger, overall: OverallFloor?): List<Breach> {
        val balances = ledger.balances()
        val accounts = ledger.accounts.mapNotNull { account ->
            val floor = account.floorMinor ?: return@mapNotNull null
            val balance = balances[account.id] ?: BigInteger.ZERO
            Breach(account, account.currency, balance, BigInteger.valueOf(floor)).takeIf { balance < it.floor }
        }
        val total = overall?.let { floor ->
            val sum = ledger.accounts.filter { it.currency == floor.currency }.fold(BigInteger.ZERO) { acc, a -> acc + (balances[a.id] ?: BigInteger.ZERO) }
            Breach(null, floor.currency, sum, BigInteger.valueOf(floor.minor)).takeIf { ledger.accounts.any { it.currency == floor.currency } && sum < it.floor }
        }
        return accounts + listOfNotNull(total)
    }

    /**
     * The floors saving [transaction] would take a balance under, in place of [previous] when it
     * is an edit. Only what this payment lowers counts: a balance already under and left as it is,
     * or raised, is not its doing.
     */
    fun breaches(ledger: Ledger, transaction: Transaction, previous: Transaction?, overall: OverallFloor?): List<Breach> {
        val change = HashMap<String, BigInteger>()
        fun apply(t: Transaction, sign: Int) {
            val amount = BigInteger.valueOf(t.minor).multiply(BigInteger.valueOf(sign.toLong()))
            when (t.kind) {
                TransactionKind.INCOME -> change.merge(t.accountId, amount, BigInteger::add)
                TransactionKind.EXPENSE -> change.merge(t.accountId, amount.negate(), BigInteger::add)
                TransactionKind.TRANSFER -> {
                    change.merge(t.accountId, amount.negate(), BigInteger::add)
                    t.destinationId?.let { change.merge(it, amount, BigInteger::add) }
                }
            }
        }
        previous?.let { apply(it, -1) }
        apply(transaction, 1)
        val balances = ledger.balances()
        val byId = ledger.accounts.associateBy { it.id }
        val accounts = change.mapNotNull { (id, delta) ->
            val account = byId[id] ?: return@mapNotNull null
            val floor = account.floorMinor ?: return@mapNotNull null
            val after = (balances[id] ?: BigInteger.ZERO) + delta
            Breach(account, account.currency, after, BigInteger.valueOf(floor)).takeIf { delta.signum() < 0 && after < it.floor }
        }
        val total = overall?.let { floor ->
            val ids = ledger.accounts.filter { it.currency == floor.currency }.map { it.id }
            val delta = ids.fold(BigInteger.ZERO) { acc, id -> acc + (change[id] ?: BigInteger.ZERO) }
            val after = ids.fold(BigInteger.ZERO) { acc, id -> acc + (balances[id] ?: BigInteger.ZERO) } + delta
            Breach(null, floor.currency, after, BigInteger.valueOf(floor.minor)).takeIf { delta.signum() < 0 && after < it.floor }
        }
        return accounts + listOfNotNull(total)
    }
}
