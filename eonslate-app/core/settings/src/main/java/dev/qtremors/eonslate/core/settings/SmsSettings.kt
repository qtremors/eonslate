package dev.qtremors.eonslate.core.settings

/**
 * Payment SMS detection. Off until the person turns it on, which also asks for SMS access.
 *
 * @property autoApprove found payments are added without review.
 * @property discarded keys of payments the person dismissed, so they never come back.
 */
data class SmsSettings(
    val detect: Boolean = false,
    val autoApprove: Boolean = false,
    val discarded: Set<String> = emptySet(),
)
