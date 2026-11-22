package dev.qtremors.eonslate.feature.welcome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.validation.TrackerValidator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeTest {
    @get:Rule
    val compose = createComposeRule()

    private val repo = Repo()
    private val settings = Settings()
    private val clock = object : AppClock {
        override fun nowMillis() = 1_000L
        override fun zoneId() = "UTC"
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val finance = Finance()
    private fun vm(handle: SavedStateHandle = SavedStateHandle()) =
        WelcomeViewModel(handle, repo, settings, clock, finance, dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator { "acc" })

    private fun tracker(id: String) = Tracker(id = id, name = id, fields = listOf(FieldDefinition("$id-f", id, "F", FieldConfig.YesNo)), createdAt = 0)

    @Test
    fun startersArePlacedAfterExistingTrackersAndWelcomeIsSeen() = runTest {
        repo.next = 3
        val vm = vm()
        vm.finish { listOf(tracker("a"), tracker("b")) }
        assertEquals(listOf(3, 4), repo.saved.map { it.position })
        assertEquals(listOf(1_000L, 1_001L), repo.saved.map { it.createdAt })
        assertTrue(settings.seen.value)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun startersUseTheChosenCurrency() = runTest {
        var seen = ""
        vm().finish { currency -> seen = currency; emptyList() }
        assertEquals("JPY", seen)
    }

    @Test
    fun skippingSavesNothingButStillCounts() = runTest {
        vm().skip()
        assertTrue(repo.saved.isEmpty())
        assertTrue(finance.accounts.isEmpty())
        assertTrue(settings.seen.value)
    }

    @Test
    fun settingUpSavesTheNameTheCurrencyAndTheFirstAccount() = runTest {
        val vm = vm()
        vm.setName("Asha")
        vm.setCurrency("inr")
        vm.setAccountKind(dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind.CARD)
        vm.setAccountName("HDFC Millennia")
        vm.setOpening("1500.50")
        vm.setDigits("12a34")
        assertTrue(vm.state.value.moneyReady)
        vm.finish("Main account") { emptyList() }
        val account = finance.accounts.single()
        assertEquals("HDFC Millennia", account.name)
        assertEquals("INR", account.currency)
        assertEquals(150_050L, account.openingMinor)
        assertEquals("1234", account.digits)
        assertEquals("Asha", settings.display.first().name)
    }

    @Test
    fun aBlankAccountNameTakesTheDefaultAndABadAmountHoldsTheMoneyPage() = runTest {
        val vm = vm()
        vm.setOpening("12,5,0")
        assertTrue(!vm.state.value.moneyReady)
        vm.setOpening("")
        vm.finish("Main account") { emptyList() }
        assertEquals("Main account", finance.accounts.single().name)
        assertEquals(0L, finance.accounts.single().openingMinor)
    }

    @Test
    fun restoringLeavesWithoutSettingAnythingUp() = runTest {
        val vm = vm()
        vm.setName("Asha")
        vm.restore()
        assertTrue(vm.state.value.restore)
        assertTrue(vm.state.value.done)
        assertTrue(finance.accounts.isEmpty())
        assertEquals("", settings.display.first().name)
    }

    @Test
    fun picksAndPageSurviveProcessDeath() = runTest {
        val handle = SavedStateHandle()
        val first = vm(handle)
        first.showPage(WelcomePage.MONEY)
        first.toggle("water")
        first.toggle("mood")
        first.toggle("water")
        first.setDigits("4821")
        val restored = vm(handle).state.value
        assertEquals(WelcomePage.MONEY, restored.page)
        assertEquals(listOf("mood"), restored.picked)
        assertEquals("4821", restored.digits)
    }

    @Test
    fun thePagesWalkFromIntroToStarters() {
        val vm = vm()
        compose.setContent { EonslateTheme { WelcomeScreen(onDone = {}, viewModel = vm) } }
        compose.onNodeWithText("Private by design").assertIsDisplayed()
        compose.onNodeWithText("Get started").performClick()
        compose.onNodeWithText("What should we call you?").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Your money").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Pick a few to start").assertIsDisplayed()
        compose.onNodeWithText("Start with nothing").assertIsDisplayed()
        compose.onNodeWithText("Mood").performClick()
        compose.onNodeWithText("Start with 1 tracker").performClick()
        compose.waitForIdle()
        val saved = repo.saved.single()
        assertEquals("mood", saved.templateId)
        assertEquals("Mood", saved.name)
        assertEquals(ValidationResult.Valid, TrackerValidator.validate(saved))
    }

    @Test
    fun skippingFromTheFirstPageCreatesNothing() {
        val vm = vm()
        var done = 0
        compose.setContent { EonslateTheme { WelcomeScreen(onDone = { done++ }, viewModel = vm) } }
        compose.onNodeWithText("Skip").performClick()
        compose.waitForIdle()
        assertTrue(repo.saved.isEmpty())
        assertTrue(settings.seen.value)
        assertEquals(1, done)
    }

    @Test
    fun backOnTheSecondPageReturnsToTheFirst() = runTest {
        val vm = vm()
        vm.next()
        vm.back()
        assertEquals(WelcomePage.INTRO, vm.state.value.page)
        vm.back()
        assertEquals(WelcomePage.INTRO, vm.state.value.page)
        repeat(5) { vm.next() }
        assertEquals(WelcomePage.TRACKERS, vm.state.value.page)
    }

    private class Repo : TrackerRepository {
        val saved = mutableListOf<Tracker>()
        var next = 0
        override fun observeTrackers(includeArchived: Boolean): Flow<List<Tracker>> = emptyFlow()
        override fun observeSummaries(): Flow<List<TrackerSummary>> = emptyFlow()
        override fun observeTracker(id: String): Flow<Tracker?> = emptyFlow()
        override suspend fun getTracker(id: String): Tracker? = null
        override suspend fun nextPosition() = next
        override suspend fun saveTracker(tracker: Tracker): ValidationResult = ValidationResult.Valid.also { saved += tracker }
        override suspend fun setArchived(id: String, archived: Boolean) = Unit
        override suspend fun reorder(orderedIds: List<String>) = Unit
        override suspend fun deleteTracker(id: String) = Unit
    }

    private class Settings : SettingsRepository by dev.qtremors.eonslate.core.settings.InMemorySettingsRepository() {
        override val homeTag: Flow<String?> = MutableStateFlow(null)
        override suspend fun setHomeTag(tag: String?) = Unit

        val seen = MutableStateFlow(false)
        override val welcomeSeen: Flow<Boolean> = seen
        override suspend fun setWelcomeSeen(seen: Boolean) {
            this.seen.value = seen
        }
        override val appearance: Flow<dev.qtremors.eonslate.core.settings.Appearance> = MutableStateFlow(dev.qtremors.eonslate.core.settings.Appearance())
        override suspend fun updateAppearance(change: (dev.qtremors.eonslate.core.settings.Appearance) -> dev.qtremors.eonslate.core.settings.Appearance) = Unit
        override val tracking: Flow<dev.qtremors.eonslate.core.settings.Tracking> = MutableStateFlow(dev.qtremors.eonslate.core.settings.Tracking(currency = "JPY"))
        override suspend fun updateTracking(change: (dev.qtremors.eonslate.core.settings.Tracking) -> dev.qtremors.eonslate.core.settings.Tracking) = Unit
    }

    private class Finance : dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository {
        val accounts = mutableListOf<dev.qtremors.eonslate.core.tracker.domain.finance.Account>()
        override fun observeLedger(): Flow<dev.qtremors.eonslate.core.tracker.domain.finance.Ledger> =
            MutableStateFlow(dev.qtremors.eonslate.core.tracker.domain.finance.Ledger(accounts = accounts.toList()))
        override suspend fun saveAccount(account: dev.qtremors.eonslate.core.tracker.domain.finance.Account) = account.isValid().also { if (it) accounts += account }
        override suspend fun deleteAccount(id: String) = false
        override suspend fun saveTransaction(transaction: dev.qtremors.eonslate.core.tracker.domain.finance.Transaction) = false
        override suspend fun appendTransactions(
            transactions: List<dev.qtremors.eonslate.core.tracker.domain.finance.Transaction>,
            parties: List<dev.qtremors.eonslate.core.tracker.domain.finance.Party>,
        ) = false
        override suspend fun saveParty(party: dev.qtremors.eonslate.core.tracker.domain.finance.Party) = false
        override suspend fun deleteParty(id: String) = false
        override suspend fun deleteTransaction(id: String) = Unit
        override suspend fun deleteTransactions(ids: Collection<String>) = Unit
        override suspend fun addRecurring(payment: dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment, throughDay: Long, throughMinute: Int) = false
        override suspend fun updateRecurring(
            expected: dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment,
            payment: dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment,
            throughDay: Long,
            throughMinute: Int,
        ) = false
        override suspend fun deleteRecurring(id: String) = Unit
        override suspend fun recordRecurring(expected: dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment, minor: Long, day: Long) = false
        override suspend fun postDue(throughDay: Long, throughMinute: Int) = Unit
        override suspend fun saveBudget(budget: dev.qtremors.eonslate.core.tracker.domain.finance.CategoryBudget) = false
        override suspend fun deleteBudget(id: String) = Unit
        override suspend fun saveCategory(category: dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice, previous: dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice?) = false
        override suspend fun deleteCategory(category: dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice) = false
    }
}
