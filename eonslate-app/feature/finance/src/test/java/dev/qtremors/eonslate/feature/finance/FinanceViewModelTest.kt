package dev.qtremors.eonslate.feature.finance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.qtremors.eonslate.core.settings.InMemorySettingsRepository
import dev.qtremors.eonslate.core.settings.Tracking
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Attachment
import dev.qtremors.eonslate.core.tracker.domain.finance.AttachmentKind
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Party
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyBank
import dev.qtremors.eonslate.core.tracker.domain.finance.PiggyMove
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FinanceViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val bank = Account("bank", "Bank", "JPY", openingMinor = 10_000)
    private val finance = Finance()
    private val settings = InMemorySettingsRepository()
    private val clock = object : AppClock {
        override fun nowMillis() = 1_700_000_000_000
        override fun zoneId() = "UTC"
    }
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A view model with its ledger collected, as a screen would, so it follows the repository. */
    private fun TestScope.newViewModel(): FinanceViewModel {
        val viewModel = FinanceViewModel(context, finance, settings, ids, clock, SmsDetection(context, settings, finance, ids, clock))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.ledger.collect {} }
        return viewModel
    }

    private fun payment(id: String, minor: Long = 500) = Transaction(id, bank.id, TransactionKind.EXPENSE, minor, 19_700)

    @Test
    fun moneyStartsWithOneAccountInTheAppsCurrencyMadeOnce() = runTest {
        settings.updateTracking { Tracking(currency = "JPY") }
        val viewModel = newViewModel()
        viewModel.ensureAccount()
        viewModel.ensureAccount()
        val account = finance.ledger.value.accounts.single()
        assertEquals("JPY", account.currency)
        assertEquals(context.getString(R.string.finance_default_account), account.name)
    }

    @Test
    fun aNameTypedTwiceIsOnePayee() = runTest {
        finance.ledger.update { it.copy(accounts = listOf(bank)) }
        val viewModel = newViewModel()
        var done = 0
        viewModel.save(payment("a"), "Someone", null) { done++ }
        viewModel.save(payment("b"), "  someone ", null) { done++ }
        assertEquals(2, done)
        val payee = finance.ledger.value.parties.single()
        assertEquals("Someone", payee.name)
        assertEquals(listOf(payee.id, payee.id), finance.ledger.value.transactions.map { it.partyId })
    }

    @Test
    fun undoBringsBackADeletedTransactionsAttachments() = runTest {
        finance.ledger.update { it.copy(accounts = listOf(bank)) }
        val viewModel = newViewModel()
        val link = Attachment("link", "", AttachmentKind.LINK, "https://example.com")
        viewModel.save(payment("a"), null, null, listOf(link)) {}
        val saved = finance.ledger.value.transactions.single()
        assertEquals(listOf("link"), finance.ledger.value.attachments.map { it.id })

        viewModel.delete(saved) {}
        assertTrue(finance.ledger.value.transactions.isEmpty())
        assertTrue(finance.ledger.value.attachments.isEmpty())

        viewModel.restore(saved)
        assertEquals(listOf(saved), finance.ledger.value.transactions)
        assertEquals(listOf(link.copy(transactionId = "a")), finance.ledger.value.attachments)
    }

    @Test
    fun aRefusedSaveSaysSoAndLeavesTheEditorOpen() = runTest {
        finance.ledger.update { it.copy(accounts = listOf(bank)) }
        finance.refuse = true
        val viewModel = newViewModel()
        var done = false
        viewModel.save(payment("a")) { done = true }
        assertFalse(done)
        assertTrue(viewModel.failed.value)
        assertFalse(viewModel.busy.value)
        viewModel.clearError()
        assertFalse(viewModel.failed.value)
    }

    @Test
    fun aSecondSaveWhileOneIsWritingIsDropped() = runTest {
        finance.ledger.update { it.copy(accounts = listOf(bank)) }
        val gate = CompletableDeferred<Unit>()
        finance.gate = gate
        val viewModel = newViewModel()
        viewModel.save(payment("a")) {}
        assertTrue(viewModel.busy.value)
        viewModel.save(payment("b")) {}
        gate.complete(Unit)
        assertFalse(viewModel.busy.value)
        assertEquals(listOf("a"), finance.ledger.value.transactions.map { it.id })
    }

    /** Keeps the ledger in memory; [refuse] turns every save down and [gate] holds saves until it completes. */
    private class Finance : FinanceRepository {
        val ledger = MutableStateFlow(Ledger())
        var refuse = false
        var gate: CompletableDeferred<Unit>? = null

        override fun observeLedger(): Flow<Ledger> = ledger

        override suspend fun saveAccount(account: Account): Boolean {
            ledger.update { it.copy(accounts = it.accounts.filter { a -> a.id != account.id } + account) }
            return true
        }

        override suspend fun saveTransaction(transaction: Transaction): Boolean {
            gate?.await()
            if (refuse) return false
            ledger.update { it.copy(transactions = it.transactions.filter { t -> t.id != transaction.id } + transaction) }
            return true
        }

        override suspend fun saveParty(party: Party): Boolean {
            ledger.update { it.copy(parties = it.parties + party) }
            return true
        }

        override suspend fun saveAttachments(transactionId: String, attachments: List<Attachment>): Boolean {
            ledger.update { it.copy(attachments = it.attachments.filter { a -> a.transactionId != transactionId } + attachments) }
            return true
        }

        override suspend fun deleteTransaction(id: String) {
            ledger.update { l -> l.copy(transactions = l.transactions.filter { it.id != id }, attachments = l.attachments.filter { it.transactionId != id }) }
        }

        override suspend fun deleteAccount(id: String) = false
        override suspend fun saveDeposit(account: Account, transactions: List<Transaction>) = false
        override suspend fun deleteDeposit(id: String) = false
        override suspend fun appendTransactions(transactions: List<Transaction>, parties: List<Party>) = false
        override suspend fun deleteParty(id: String) = false
        override suspend fun savePiggy(piggy: PiggyBank) = false
        override suspend fun deletePiggy(id: String) = Unit
        override suspend fun movePiggy(move: PiggyMove) = false
        override suspend fun deletePiggyMove(id: String) = Unit
        override suspend fun deleteTransactions(ids: Collection<String>) = Unit
        override suspend fun addRecurring(payment: RecurringPayment, throughDay: Long, throughMinute: Int) = false
        override suspend fun updateRecurring(expected: RecurringPayment, payment: RecurringPayment, throughDay: Long, throughMinute: Int, priceFrom: Long?) = false
        override suspend fun deleteRecurring(id: String) = Unit
        override suspend fun recordRecurring(expected: RecurringPayment, minor: Long, day: Long) = false
        override suspend fun postDue(throughDay: Long, throughMinute: Int) = Unit
        override suspend fun saveBudget(budget: CategoryBudget) = false
        override suspend fun deleteBudget(id: String) = Unit
        override suspend fun saveCategory(category: CategoryChoice, previous: CategoryChoice?) = false
        override suspend fun deleteCategory(category: CategoryChoice) = false
    }
}
