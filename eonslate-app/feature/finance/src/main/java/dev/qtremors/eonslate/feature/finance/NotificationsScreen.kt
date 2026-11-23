package dev.qtremors.eonslate.feature.finance

import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.insights.Alert
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.intervalRemaining
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(center: AlertCenter, val detection: SmsDetection) : ViewModel() {
    val alerts = center.alerts
}

/**
 * Everything waiting, in one place: payments found in SMS, payments that need their amount,
 * budgets gone over, intervals come round, and payments in the next few days.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onReviewSms: () -> Unit,
    onSubscriptions: () -> Unit,
    onTransactions: () -> Unit,
    onTracker: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val result by viewModel.detection.result.collectAsStateWithLifecycle()
    val locale = LocalValueDisplay.current.numberLocale
    EonslateScaffold(title = stringResource(R.string.alerts_title), onBack = onBack, modifier = modifier) { padding ->
        val list = alerts
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@EonslateScaffold
        }
        val found = list.any { it is Alert.FoundPayments }
        if (!found && result == null && list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Rounded.DoneAll, stringResource(R.string.alerts_empty_title), stringResource(R.string.alerts_empty_message))
            }
            return@EonslateScaffold
        }
        val due = list.filterIsInstance<Alert.PaymentDue>()
        val over = list.filterIsInstance<Alert.OverBudget>()
        val floors = list.filterIsInstance<Alert.BelowFloor>()
        val intervals = list.filterIsInstance<Alert.IntervalDue>()
        val soon = list.filterIsInstance<Alert.PaymentSoon>()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (found || result != null) item(key = "sms") { FoundPaymentsBanner(viewModel.detection, onReviewSms, Modifier.padding(top = 8.dp)) }
            if (floors.isNotEmpty()) item(key = "floors") {
                Section(stringResource(R.string.alerts_section_floors)) {
                    floors.forEach { alert ->
                        val breach = alert.breach
                        ConnectedRow(
                            title = breach.account?.label ?: stringResource(R.string.floor_overall),
                            supporting = stringResource(
                                R.string.floor_under,
                                ValueFormat.money(breach.under, breach.currency, locale),
                                ValueFormat.money(breach.floor, breach.currency, locale),
                            ),
                            leading = { IconBadge(Icons.Rounded.VerticalAlignBottom, breach.account?.id ?: "overall") },
                            onClick = onTransactions,
                        )
                    }
                }
            }
            if (due.isNotEmpty()) item(key = "due") {
                Section(stringResource(R.string.alerts_section_due)) {
                    due.forEach { alert ->
                        ConnectedRow(
                            title = alert.payment.note.ifBlank { stringResource(R.string.alerts_payment) },
                            supporting = if (alert.daysLate == 0L) stringResource(R.string.alerts_due_today)
                            else pluralStringResource(R.plurals.alerts_waiting_days, alert.daysLate.toInt(), alert.daysLate.toInt()),
                            leading = { IconBadge(Icons.Rounded.HourglassBottom, alert.payment.id) },
                            onClick = onSubscriptions,
                        )
                    }
                }
            }
            if (over.isNotEmpty()) item(key = "over") {
                Section(stringResource(R.string.alerts_section_budgets)) {
                    over.forEach { alert ->
                        val health = alert.health
                        ConnectedRow(
                            title = health.title,
                            supporting = stringResource(R.string.alerts_over, health.percent, ValueFormat.money(health.limit, health.currency, locale)),
                            leading = { IconBadge(Icons.Rounded.Savings, health.title) },
                            onClick = onTransactions,
                        )
                    }
                }
            }
            if (intervals.isNotEmpty()) item(key = "intervals") {
                Section(stringResource(R.string.alerts_section_trackers)) {
                    intervals.forEach { alert ->
                        ConnectedRow(
                            title = alert.status.label?.let { "${alert.tracker.name} · $it" } ?: alert.tracker.name,
                            supporting = intervalRemaining(alert.status, alert.tracker),
                            leading = { IconBadge(Icons.Rounded.Update, alert.tracker.id) },
                            onClick = { onTracker(alert.tracker.id) },
                        )
                    }
                }
            }
            if (soon.isNotEmpty()) item(key = "soon") {
                Section(stringResource(R.string.alerts_section_soon)) {
                    soon.forEach { alert ->
                        ConnectedRow(
                            title = alert.payment.note.ifBlank { stringResource(R.string.alerts_payment) },
                            supporting = pluralStringResource(R.plurals.alerts_in_days, alert.daysLeft.toInt(), alert.daysLeft.toInt()) +
                                " · " + ValueFormat.money(alert.payment.minor, alert.currency, locale),
                            leading = { IconBadge(Icons.Rounded.EventRepeat, alert.payment.id) },
                            onClick = onSubscriptions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, rows: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        ConnectedGroup { rows() }
    }
}
