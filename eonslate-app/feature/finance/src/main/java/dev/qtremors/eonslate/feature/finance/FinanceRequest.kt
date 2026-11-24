package dev.qtremors.eonslate.feature.finance

import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind

/**
 * Something another screen asks Transactions to open once it is shown, such as a tap on an
 * account card on Home. [FinanceScreen] acts on it, then reports it handled so it is not repeated.
 */
sealed interface FinanceRequest {
    /** The payment SMS found in the inbox, for review. */
    data object Inbox : FinanceRequest

    /** A new transaction of this kind. */
    data class NewTransaction(val kind: TransactionKind) : FinanceRequest

    /** Only this account's transactions; a deposit opens the deposit instead. */
    data class Account(val id: String) : FinanceRequest

    /** The new account editor. */
    data object NewAccount : FinanceRequest

    /** One piggy bank. */
    data class Piggy(val id: String) : FinanceRequest
}
