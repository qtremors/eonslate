package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.tracker.domain.insights.IntervalStatus
import dev.qtremors.eonslate.core.tracker.domain.insights.Rhythm
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
private fun reading(field: FieldDefinition?, amount: Long): String =
    if (field == null) amount.toString()
    else ValueFormat.format(field, FieldValue.Number(amount), rememberValueWords(), LocalValueDisplay.current.numberLocale)

@Composable
private fun shortDate(day: LocalDate): String {
    val locale = Locale.getDefault()
    return day.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM"), locale))
}

/** "in 12 days", "today", "3 days ago". */
@Composable
private fun relativeDays(days: Long): String = when {
    days == 0L -> stringResource(R.string.interval_today)
    days > 0 -> pluralStringResource(R.plurals.interval_in_days, days.toInt(), days.toInt())
    else -> pluralStringResource(R.plurals.interval_days_ago, (-days).toInt(), (-days).toInt())
}

/** A gap read the way people say it: "5 weeks", "4 months", "10 days". */
@Composable
fun gapText(days: Long): String = when {
    days >= 330 && days % 365 in 0..30 -> pluralStringResource(R.plurals.interval_gap_years, (days / 365).toInt(), (days / 365).toInt())
    days >= 56 -> pluralStringResource(R.plurals.interval_gap_months, ((days + 15) / 30).toInt(), ((days + 15) / 30).toInt())
    days >= 14 && days % 7 == 0L -> pluralStringResource(R.plurals.interval_gap_weeks, (days / 7).toInt(), (days / 7).toInt())
    else -> pluralStringResource(R.plurals.interval_gap_days, days.toInt(), days.toInt())
}

/**
 * When a set interval falls due: "Oil change due at 30,000 km · 1,240 km left", "Servicing due in
 * 12 days", or "Due 3 days ago" without a job. The tighter of time and amount leads.
 */
@Composable
fun intervalText(status: IntervalStatus, tracker: Tracker): String {
    val field = status.interval.amountFieldId?.let(tracker::field)
    val amountLeft = status.amountLeft
    val byAmount = status.dueAt != null && amountLeft != null && status.leadsByAmount
    val due = if (byAmount) {
        val at = reading(field, status.dueAt!!)
        val left = if (amountLeft!! >= 0) stringResource(R.string.interval_left, reading(field, amountLeft))
        else stringResource(R.string.interval_over, reading(field, abs(amountLeft)))
        stringResource(R.string.interval_due_at, at) + " · " + left
    } else {
        stringResource(R.string.interval_due, relativeDays(status.daysLeft ?: 0))
    }
    return status.label?.let { "$it $due" } ?: due.replaceFirstChar { it.uppercase() }
}

/** When it was last done: "Last 12 Aug at 27,000 km", or "Not logged yet". */
@Composable
fun intervalLastText(status: IntervalStatus, tracker: Tracker): String {
    val last = status.last ?: return stringResource(R.string.interval_never)
    val field = status.interval.amountFieldId?.let(tracker::field)
    return status.lastAmount?.let { stringResource(R.string.interval_last_at, shortDate(last), reading(field, it)) }
        ?: stringResource(R.string.interval_last, shortDate(last))
}

/** A learned rhythm: "Usually every 5 weeks · next around 12 Dec", or by a growing number. */
@Composable
fun rhythmText(rhythm: Rhythm, tracker: Tracker): String {
    val field = rhythm.meterFieldId?.let(tracker::field)
    val text = if (rhythm.growth != null && rhythm.nextAt != null) {
        stringResource(R.string.rhythm_every, reading(field, rhythm.growth!!)) + " · " + stringResource(R.string.rhythm_next_at, reading(field, rhythm.nextAt!!))
    } else {
        stringResource(R.string.rhythm_every, gapText(rhythm.days)) + " · " + stringResource(R.string.rhythm_next_on, shortDate(rhythm.nextOn))
    }
    return rhythm.label?.let { "$it: ${text.replaceFirstChar { c -> c.lowercase() }}" } ?: text
}

/** What is left, for a tile's big line: "1,240 km left", "50 km over", "In 121 days", "Today". */
@Composable
fun intervalRemaining(status: IntervalStatus, tracker: Tracker): String {
    val field = status.interval.amountFieldId?.let(tracker::field)
    val left = status.amountLeft
    return if (status.dueAt != null && left != null && status.leadsByAmount) {
        if (left >= 0) stringResource(R.string.interval_left, reading(field, left)) else stringResource(R.string.interval_over, reading(field, abs(left)))
    } else {
        relativeDays(status.daysLeft ?: 0).replaceFirstChar { it.uppercase() }
    }
}

/** When it falls due: "Due at 30,000 km", "Due 14 Mar". */
@Composable
fun intervalDuePoint(status: IntervalStatus, tracker: Tracker): String? {
    val field = status.interval.amountFieldId?.let(tracker::field)
    return when {
        status.dueAt != null && status.amountLeft != null && status.leadsByAmount -> stringResource(R.string.interval_due_at, reading(field, status.dueAt!!)).replaceFirstChar { it.uppercase() }
        status.dueOn != null -> stringResource(R.string.interval_due, shortDate(status.dueOn!!)).replaceFirstChar { it.uppercase() }
        else -> null
    }
}
