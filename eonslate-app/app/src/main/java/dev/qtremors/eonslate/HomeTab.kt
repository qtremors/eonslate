package dev.qtremors.eonslate

import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.feature.finance.MoneyOverview
import dev.qtremors.eonslate.feature.home.TodayStrip

/** The start screen: money first, then today's trackers. Each card leads to the tab that manages it. */
@Composable
fun HomeTab(
    onTransactions: () -> Unit,
    onSubscriptions: () -> Unit,
    onTrackers: () -> Unit,
    onLog: (String) -> Unit,
    onSettings: () -> Unit,
    onReviewSms: () -> Unit,
    onAdd: (TransactionKind) -> Unit,
    onAddBudget: () -> Unit,
    displayName: String = "",
) {
    EonslateScaffold(
        title = displayName.ifBlank { stringResource(R.string.app_title) },
        actions = {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.tab_settings))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "money") { MoneyOverview(onTransactions = onTransactions, onReviewSms = onReviewSms, onSubscriptions = onSubscriptions, onAdd = onAdd, onAddBudget = onAddBudget) }
            item(key = "today") { TodayStrip(onLog = onLog, onAll = onTrackers) }
        }
    }
}
