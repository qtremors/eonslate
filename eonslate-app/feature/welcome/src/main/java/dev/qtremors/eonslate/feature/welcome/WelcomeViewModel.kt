package dev.qtremors.eonslate.feature.welcome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.settings.DisplayPreferences
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** The first run's pages, in order. */
enum class WelcomePage { INTRO, YOU, MONEY }

data class WelcomeState(
    val page: WelcomePage = WelcomePage.INTRO,
    val name: String = "",
    /** The currency money starts in; the device's until changed. */
    val currency: String = "",
    val accountName: String = "",
    val accountKind: AccountKind = AccountKind.BANK,
    val opening: String = "",
    val digits: String = "",
    val saving: Boolean = false,
    val done: Boolean = false,
    /** True when the person chose to restore a backup instead of setting up. */
    val restore: Boolean = false,
) {
    val currencyKnown: Boolean get() = MoneyAmount.isKnownCurrency(currency)
    val openingMinor: Long? get() = if (opening.isBlank()) 0L else MoneyAmount.parse(opening.trim().replace(',', '.'), currency)
    val digitsValid: Boolean get() = digits.isEmpty() || Account.DIGITS.matches(digits)

    /** The money page can move on once what is typed there makes sense. */
    val moneyReady: Boolean get() = currencyKnown && openingMinor != null && digitsValid
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val trackers: TrackerRepository,
    private val settings: SettingsRepository,
    private val clock: AppClock,
    private val finance: FinanceRepository,
    private val ids: IdGenerator,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WelcomeState(
            page = WelcomePage.entries.getOrNull(savedState[KEY_PAGE] ?: 0) ?: WelcomePage.INTRO,
            name = savedState[KEY_NAME] ?: "",
            currency = savedState[KEY_CURRENCY] ?: "",
            accountName = savedState[KEY_ACCOUNT] ?: "",
            accountKind = AccountKind.fromKey(savedState[KEY_KIND]),
            opening = savedState[KEY_OPENING] ?: "",
            digits = savedState[KEY_DIGITS] ?: "",
        )
    )
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    init {
        // Money starts in the currency already set, or the device's.
        if (_state.value.currency.isBlank()) viewModelScope.launch {
            val code = settings.tracking.first().currencyCode()
            _state.update { if (it.currency.isBlank()) it.copy(currency = code) else it }
        }
    }

    private fun change(change: (WelcomeState) -> WelcomeState) {
        _state.update(change)
        val s = _state.value
        savedState[KEY_PAGE] = s.page.ordinal
        savedState[KEY_NAME] = s.name
        savedState[KEY_CURRENCY] = s.currency
        savedState[KEY_ACCOUNT] = s.accountName
        savedState[KEY_KIND] = s.accountKind.storageKey
        savedState[KEY_OPENING] = s.opening
        savedState[KEY_DIGITS] = s.digits
    }

    fun showPage(page: WelcomePage) = change { it.copy(page = page) }

    fun next() = change { s -> s.copy(page = WelcomePage.entries.getOrElse(s.page.ordinal + 1) { s.page }) }

    fun back() = change { s -> s.copy(page = WelcomePage.entries.getOrElse(s.page.ordinal - 1) { s.page }) }

    fun setName(name: String) = change { it.copy(name = name.take(DisplayPreferences.NAME_LIMIT)) }

    fun setCurrency(code: String) = change { it.copy(currency = code.trim().uppercase(Locale.ROOT).take(3)) }

    fun setAccountName(name: String) = change { it.copy(accountName = name.take(Account.NAME_LIMIT)) }

    fun setAccountKind(kind: AccountKind) = change { it.copy(accountKind = kind) }

    fun setOpening(opening: String) = change { it.copy(opening = opening.take(24)) }

    fun setDigits(digits: String) = change { it.copy(digits = digits.filter(Char::isDigit).take(4)) }

    /**
     * Saves what was set up: the name, the currency, the first account (named [defaultAccount]
     * when left blank, and only when there is none yet). Marks the welcome as seen. Trackers are
     * left for people to build themselves.
     */
    fun finish(defaultAccount: String = "") = save(setUp = true, defaultAccount)

    /** Leaves without setting anything up. */
    fun skip() = save(setUp = false, "")

    /** Leaves to restore a backup, which brings its own name, money and trackers. */
    fun restore() {
        _state.update { it.copy(restore = true) }
        skip()
    }

    private fun save(setUp: Boolean, defaultAccount: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val s = _state.value
            val code = if (setUp && s.currencyKnown) s.currency else settings.tracking.first().currencyCode()
            if (setUp) {
                s.name.trim().takeIf { it.isNotEmpty() }?.let { name -> settings.updateDisplay { it.copy(name = name) } }
                if (s.currencyKnown) settings.updateTracking { it.copy(currency = s.currency) }
                val opening = s.openingMinor
                if (opening != null && finance.observeLedger().first().accounts.isEmpty()) {
                    val numbered = s.accountKind == AccountKind.BANK || s.accountKind == AccountKind.CARD
                    finance.saveAccount(
                        Account(
                            id = ids.newId(),
                            name = s.accountName.trim().ifEmpty { defaultAccount }.ifEmpty { s.accountKind.name },
                            currency = code,
                            openingMinor = opening,
                            kind = s.accountKind,
                            digits = s.digits.takeIf { numbered && s.digitsValid && it.isNotEmpty() },
                        ),
                    )
                }
            }
            settings.setWelcomeSeen(true)
            _state.update { it.copy(saving = false, done = true) }
        }
    }

    private companion object {
        const val KEY_PAGE = "welcome.page"
        const val KEY_NAME = "welcome.name"
        const val KEY_CURRENCY = "welcome.currency"
        const val KEY_ACCOUNT = "welcome.account"
        const val KEY_KIND = "welcome.kind"
        const val KEY_OPENING = "welcome.opening"
        const val KEY_DIGITS = "welcome.digits"
    }
}
