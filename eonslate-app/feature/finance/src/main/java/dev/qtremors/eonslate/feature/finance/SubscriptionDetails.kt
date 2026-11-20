package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.RecurringPayment
import dev.qtremors.eonslate.core.tracker.domain.finance.SubscriptionSpending
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import dev.qtremors.eonslate.core.ui.components.BarChart
import dev.qtremors.eonslate.core.ui.components.ChartAxis
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.PressableCard
import dev.qtremors.eonslate.core.ui.theme.HeroTextStyle
import dev.qtremors.eonslate.core.ui.theme.LocalSignals
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun SubscriptionDetails(
    payment: RecurringPayment,
    account: Account,
    ledger: Ledger,
    today: LocalDate,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val report = remember(payment, ledger, today) { SubscriptionSpending.of(payment, ledger, today) }
    val income = payment.kind == TransactionKind.INCOME
    val accent = if (income) LocalSignals.current.positive else MaterialTheme.colorScheme.error
    val locale = LocalValueDisplay.current.numberLocale
    val money: (BigInteger) -> String = { value ->
        val prefix = if (value.signum() == 0) "" else if (income) "+" else "−"
        prefix + ValueFormat.money(value, account.currency, locale)
    }
    val name = payment.note.ifBlank { payment.kind.label() }
    EonslateBottomSheet(onDismissRequest = onDismiss, title = name) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SubscriptionIcon(payment.icon, name, size = 48.dp, color = payment.color)
                Column(Modifier.weight(1f)) {
                    Text(payment.interval.label(), style = MaterialTheme.typography.titleMedium)
                    payment.startEpochDay?.let {
                        Text(stringResource(R.string.finance_since, displayDate(it)), style = MaterialTheme.typography.labelLarge)
                    }
                }
                FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = onEdit) {
                    Text(stringResource(R.string.finance_subscription_edit))
                }
            }
            PressableCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(if (income) R.string.finance_subscription_received else R.string.finance_subscription_spent))
                    Text(money(report.recorded), style = HeroTextStyle, color = if (report.recorded.signum() == 0) MaterialTheme.colorScheme.onSurface else accent)
                    if (report.paymentCount == 0) {
                        Text(stringResource(R.string.finance_subscription_no_payments), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    report.estimatedSinceStart?.takeIf { it != report.recorded }?.let {
                        Text(stringResource(R.string.finance_subscription_estimate), style = MaterialTheme.typography.labelLarge)
                        Text(money(it), style = MetricTextStyle, color = accent)
                    }
                }
            }
            if (report.history.values.any { it.signum() > 0 }) {
                SpendingChart(stringResource(R.string.finance_subscription_history), report.history, money, accent)
            }
            SpendingChart(
                stringResource(R.string.finance_subscription_forecast), report.forecast, money,
                MaterialTheme.colorScheme.primary, total = money(report.projected),
                detail = stringResource(
                    when {
                        payment.paused -> R.string.finance_paused
                        payment.nextEpochDay == null -> R.string.finance_subscription_no_schedule
                        else -> R.string.finance_subscription_forecast_basis
                    },
                ),
            )
        }
    }
}

@Composable
private fun SpendingChart(
    title: String,
    months: Map<YearMonth, BigInteger>,
    money: (BigInteger) -> String,
    color: Color,
    total: String? = null,
    detail: String? = null,
) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault()) }
    val labels = months.keys.map(formatter::format)
    val values = months.values.toList()
    PressableCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            total?.let { Text(it, style = MetricTextStyle) }
            detail?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (values.any { it.signum() > 0 }) {
                selected?.let { index ->
                    Text("${labels[index]} · ${money(values[index])}", style = MaterialTheme.typography.labelLarge)
                }
                BarChart(
                    values.map(BigInteger::toFloat), color,
                    contentDescription = labels.zip(values).joinToString { (month, amount) -> "$month ${money(amount)}" },
                    selected = selected,
                    onSelect = { selected = it },
                    peakLabel = if (selected == null) values.maxOrNull()?.let(money) else null,
                )
                ChartAxis(labels.first(), labels.last())
            }
        }
    }
}
