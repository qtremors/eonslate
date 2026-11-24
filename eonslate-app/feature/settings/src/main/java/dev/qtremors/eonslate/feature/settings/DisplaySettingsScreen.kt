package dev.qtremors.eonslate.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.settings.DateDisplay
import dev.qtremors.eonslate.core.settings.DisplayPreferences
import dev.qtremors.eonslate.core.settings.NumberLocale
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.NavigationRow
import dev.qtremors.eonslate.core.ui.components.PressableCard
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@Composable
fun DisplaySettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val display by viewModel.display.collectAsStateWithLifecycle()
    val settings = display
    var editName by rememberSaveable { mutableStateOf(false) }
    EonslateScaffold(title = stringResource(R.string.settings_display), onBack = onBack) { padding ->
        if (settings == null) LoadingState(Modifier.fillMaxSize().padding(padding))
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ConnectedGroup {
                    NavigationRow(
                        title = stringResource(R.string.settings_display_name),
                        supporting = settings.name.ifBlank { null },
                        icon = Icons.Rounded.Person,
                        badgeKey = "display-name",
                        onClick = { editName = true },
                    )
                }
            }
            item {
                SectionHeader(stringResource(R.string.settings_number_format))
                PressableCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(ValueFormat.decimal(BigDecimal("1234567.89"), 2, settings.numbers.locale()), style = MetricTextStyle)
                        ConnectedChoices(NumberLocale.entries, settings.numbers, { it.tag ?: stringResource(R.string.settings_format_device) }, onSelect = { choice ->
                            viewModel.updateDisplay { it.copy(numbers = choice) }
                        })
                    }
                }
            }
            item {
                SectionHeader(stringResource(R.string.settings_date_format))
                PressableCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val sampleDay = LocalDate.of(2025, 12, 31).toEpochDay()
                        Text(ValueFormat.date(sampleDay, Locale.getDefault(), settings.dates.pattern), style = MetricTextStyle)
                        ConnectedChoices(DateDisplay.entries, settings.dates, { it.label() }, onSelect = { choice ->
                            viewModel.updateDisplay { it.copy(dates = choice) }
                        })
                    }
                }
            }
        }
    }
    if (editName && settings != null) {
        var name by rememberSaveable { mutableStateOf(settings.name) }
        EonslateBottomSheet(onDismissRequest = { editName = false }, title = stringResource(R.string.settings_display_name)) {
            FormField(
                name, { name = it }, stringResource(R.string.settings_display_name),
                maxLength = DisplayPreferences.NAME_LIMIT, capitalization = KeyboardCapitalization.Words,
            )
            Button(shapes = ButtonDefaults.shapes(), onClick = {
                viewModel.updateDisplay { it.copy(name = name) }
                editName = false
            }) { Text(stringResource(R.string.settings_currency_save)) }
        }
    }
}

@Composable
private fun DateDisplay.label(): String = stringResource(when (this) {
    DateDisplay.DEVICE -> R.string.settings_format_device
    DateDisplay.DAY_FIRST -> R.string.settings_date_day_first
    DateDisplay.MONTH_FIRST -> R.string.settings_date_month_first
    DateDisplay.YEAR_FIRST -> R.string.settings_date_year_first
})
