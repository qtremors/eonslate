package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.finance.Categories
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryChoice
import dev.qtremors.eonslate.core.tracker.domain.finance.CategoryKind
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.MainActionButton

@Composable
fun ManageCategoriesScreen(onBack: () -> Unit, viewModel: FinanceViewModel = hiltViewModel()) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val categories = remember(ledger) { ledger?.let(Categories::all)?.sortedBy(CategoryChoice::key) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var newId by rememberSaveable { mutableStateOf<String?>(null) }

    EonslateScaffold(
        title = stringResource(R.string.finance_manage_categories),
        onBack = onBack,
        floatingActionButton = {
            if (categories != null) MainActionButton(
                text = stringResource(R.string.finance_add_category),
                icon = Icons.Rounded.Add,
                onClick = { viewModel.clearError(); newId = viewModel.newId() },
            )
        },
    ) { padding ->
        if (categories == null) LoadingState(Modifier.fillMaxSize().padding(padding))
        else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                ConnectedItem(index, categories.size) {
                    ConnectedRow(
                        title = category.name,
                        supporting = category.kind.label(),
                        leading = { FinanceBadge(category.icon, category.color, category.name) },
                        trailing = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                        onClick = { viewModel.clearError(); editing = category.id },
                    )
                }
            }
        }
    }

    val previous = categories?.firstOrNull { it.id == editing }
    val id = previous?.id ?: newId ?: return
    key(id) {
        val original = remember { previous }
        var name by rememberSaveable { mutableStateOf(original?.name.orEmpty()) }
        var kind by rememberSaveable { mutableStateOf(original?.kind ?: CategoryKind.BOTH) }
        var icon by rememberSaveable { mutableStateOf(original?.icon) }
        var color by rememberSaveable { mutableStateOf(original?.color) }
        val draft = CategoryChoice(name.trim(), kind, id, icon, color)
        val duplicate = categories?.any { it.id != id && it.key == draft.key } == true
        val close = { editing = null; newId = null }
        EditorSheet(
            stringResource(if (original == null) R.string.finance_add_category else R.string.finance_edit_category),
            busy, failed, close,
        ) {
            FormField(
                name, { name = it }, stringResource(R.string.finance_category),
                error = if (duplicate) stringResource(R.string.finance_category_exists) else null,
                maxLength = Transaction.CATEGORY_LIMIT, enabled = !busy,
            )
            ConnectedChoices(CategoryKind.entries, kind, { it.label() }, { if (!busy) kind = it })
            FinanceStyleField(icon, color, name, original?.name.orEmpty(), "Tag", busy, { icon = it }, { color = it })
            SaveRow(!busy && draft.isValid() && !duplicate, { viewModel.save(draft, original, close) }, null, busy)
        }
    }
}

@Composable
private fun CategoryKind.label() = stringResource(when (this) {
    CategoryKind.EXPENSE -> R.string.finance_expense
    CategoryKind.INCOME -> R.string.finance_income
    CategoryKind.BOTH -> R.string.finance_category_both
})
