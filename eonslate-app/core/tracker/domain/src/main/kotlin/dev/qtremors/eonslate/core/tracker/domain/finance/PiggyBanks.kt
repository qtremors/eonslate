package dev.qtremors.eonslate.core.tracker.domain.finance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Money set aside inside an account for something you want, such as a phone: the account keeps
 * it, and shows what is free to spend beside it. [targetMinor] is the goal, [targetEpochDay] an
 * optional day to reach it by.
 */
@Serializable
data class PiggyBank(
    val id: String,
    @SerialName("account_id") val accountId: String,
    val name: String,
    @SerialName("target_minor") val targetMinor: Long,
    @SerialName("target_epoch_day") val targetEpochDay: Long? = null,
    @SerialName("created_at") val createdAt: Long = 0,
) {
    fun isValid(accounts: Set<String>): Boolean =
        id.isNotBlank() && accountId in accounts && name.isNotBlank() && name.trim().length <= NAME_LIMIT && targetMinor > 0 &&
            (targetEpochDay == null || targetEpochDay in Transaction.DATES)

    companion object {
        const val NAME_LIMIT = 60
    }
}

/** Money put into a piggy bank (positive) or taken back out (negative) on a day. */
@Serializable
data class PiggyMove(
    val id: String,
    @SerialName("piggy_id") val piggyId: String,
    val minor: Long,
    @SerialName("epoch_day") val epochDay: Long,
    @SerialName("created_at") val createdAt: Long = 0,
) {
    fun isValid(): Boolean = id.isNotBlank() && piggyId.isNotBlank() && minor != 0L && epochDay in Transaction.DATES
}

/** Where a piggy bank stands: what is in it, toward its goal. */
data class PiggyStatus(val piggy: PiggyBank, val saved: BigInteger, val moves: List<PiggyMove>) {
    val target: BigInteger get() = BigInteger.valueOf(piggy.targetMinor)
    val left: BigInteger get() = (target - saved).max(BigInteger.ZERO)
    val fraction: Float get() = (saved.toDouble() / piggy.targetMinor).coerceIn(0.0, 1.0).toFloat()
    val reached: Boolean get() = saved >= target
}

object PiggyBanks {
    fun status(ledger: Ledger): List<PiggyStatus> {
        val moves = ledger.piggyMoves.groupBy { it.piggyId }
        return ledger.piggyBanks.map { piggy ->
            val mine = moves[piggy.id].orEmpty().sortedWith(compareBy<PiggyMove> { it.epochDay }.thenBy { it.createdAt })
            PiggyStatus(piggy, mine.fold(BigInteger.ZERO) { acc, m -> acc + BigInteger.valueOf(m.minor) }, mine)
        }
    }

    /** What each account has set aside in piggy banks. */
    fun setAside(ledger: Ledger): Map<String, BigInteger> =
        status(ledger).groupBy { it.piggy.accountId }.mapValues { (_, list) -> list.fold(BigInteger.ZERO) { acc, s -> acc + s.saved } }

    /** A move that leaves a piggy bank with less than nothing in it cannot be made. */
    fun canMove(ledger: Ledger, move: PiggyMove): Boolean {
        if (!move.isValid() || ledger.piggyBanks.none { it.id == move.piggyId }) return false
        val saved = ledger.piggyMoves.filter { it.piggyId == move.piggyId && it.id != move.id }.sumOf { it.minor }
        return saved + move.minor >= 0
    }
}
