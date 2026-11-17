package dev.qtremors.eonslate.core.tracker.domain

import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class FinanceTest {
    @Test
    fun balancesStayExactBeyondTheRangeOfOneAmount() {
        val account = Account("bank", "Bank", "KWD", openingMinor = Long.MAX_VALUE)
        val income = Transaction("income", account.id, TransactionKind.INCOME, Long.MAX_VALUE, 0)
        val expense = Transaction("expense", account.id, TransactionKind.EXPENSE, 1, 0)
        val ledger = Ledger(listOf(account), listOf(income, expense))

        assertEquals(BigInteger("18446744073709551613"), ledger.balances()[account.id])
    }
}
