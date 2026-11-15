package dev.qtremors.eonslate.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.settings.AccentChoice
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.ThemeChoice
import dev.qtremors.eonslate.core.settings.Tracking
import dev.qtremors.eonslate.core.settings.WeekStart
import dev.qtremors.eonslate.core.ui.components.ChoiceRow
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.EonslateBottomSheet
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.NavigationRow
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.components.ToggleRow
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape
import dev.qtremors.eonslate.core.ui.theme.ThemePreset
import dev.qtremors.eonslate.core.ui.theme.eonslateColorScheme

/** What the settings page changes, so it can be shown without a view model in previews and tests. */
interface SettingsActions {
    fun setTheme(theme: ThemeChoice)
    fun setPureBlack(on: Boolean)
    fun setAccent(accent: AccentChoice)
    fun setPreset(name: String)
    fun setHaptics(on: Boolean)
    fun setReduceMotion(on: Boolean)
    fun setWeekStart(start: WeekStart) {}
    fun setCurrency(code: String?) {}
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : SettingsActions {
            override fun setTheme(theme: ThemeChoice) = viewModel.setTheme(theme)
            override fun setPureBlack(on: Boolean) = viewModel.setPureBlack(on)
            override fun setAccent(accent: AccentChoice) = viewModel.setAccent(accent)
            override fun setPreset(name: String) = viewModel.setPreset(name)
            override fun setHaptics(on: Boolean) = viewModel.setHaptics(on)
            override fun setReduceMotion(on: Boolean) = viewModel.setReduceMotion(on)
            override fun setWeekStart(start: WeekStart) = viewModel.setWeekStart(start)
            override fun setCurrency(code: String?) = viewModel.setCurrency(code)
        }
    }
    SettingsContent(appearance, actions, onBack, onAbout, modifier, tracking = tracking)
}

/**
 * The settings page in the order the design system sets: appearance, palette, behavior, about.
 * Every row shows its current value, and every change applies at once.
 */
@Composable
fun SettingsContent(
    appearance: Appearance?,
    actions: SettingsActions,
    onBack: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    wallpaperAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    tracking: Tracking? = Tracking(),
) {
    var editCurrency by rememberSaveable { mutableStateOf(false) }
    if (editCurrency && tracking != null) {
        CurrencySheet(
            current = tracking.currency,
            onSave = {
                actions.setCurrency(it)
                editCurrency = false
            },
            onDismiss = { editCurrency = false },
        )
    }
    EonslateScaffold(title = stringResource(R.string.settings_title), onBack = onBack, modifier = modifier) { padding ->
        if (appearance == null) {
            LoadingState(Modifier.fillMaxSize().padding(padding))
            return@EonslateScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "appearance") {
                SectionHeader(stringResource(R.string.settings_section_appearance))
                ConnectedGroup {
                    ChoiceRow(
                        title = stringResource(R.string.settings_theme),
                        icon = Icons.Rounded.DarkMode,
                        options = ThemeChoice.entries,
                        selected = appearance.theme,
                        label = {
                            stringResource(
                                when (it) {
                                    ThemeChoice.SYSTEM -> R.string.settings_theme_system
                                    ThemeChoice.LIGHT -> R.string.settings_theme_light
                                    ThemeChoice.DARK -> R.string.settings_theme_dark
                                }
                            )
                        },
                        onSelect = actions::setTheme,
                    )
                    ToggleRow(
                        title = stringResource(R.string.settings_pure_black),
                        supporting = stringResource(
                            if (appearance.theme == ThemeChoice.LIGHT) R.string.settings_pure_black_off else R.string.settings_pure_black_hint
                        ),
                        checked = appearance.pureBlack,
                        onCheckedChange = actions::setPureBlack,
                        icon = Icons.Rounded.Contrast,
                        enabled = appearance.theme != ThemeChoice.LIGHT,
                    )
                    if (wallpaperAvailable) {
                        ChoiceRow(
                            title = stringResource(R.string.settings_colors),
                            supporting = stringResource(
                                if (appearance.accent == AccentChoice.WALLPAPER) R.string.settings_colors_wallpaper_hint
                                else R.string.settings_colors_preset_hint
                            ),
                            icon = Icons.Rounded.Palette,
                            options = AccentChoice.entries,
                            selected = appearance.accent,
                            label = {
                                stringResource(if (it == AccentChoice.PRESET) R.string.settings_colors_eonslate else R.string.settings_colors_wallpaper)
                            },
                            onSelect = actions::setAccent,
                        )
                    }
                }
            }
            item(key = "presets") {
                SectionHeader(stringResource(R.string.settings_section_palette))
                PresetPicker(
                    selected = appearance.preset.takeIf { appearance.accent == AccentChoice.PRESET },
                    onSelect = actions::setPreset,
                )
            }
            item(key = "behavior") {
                SectionHeader(stringResource(R.string.settings_section_behavior))
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.settings_haptics),
                        supporting = stringResource(R.string.settings_haptics_hint),
                        checked = appearance.haptics,
                        onCheckedChange = actions::setHaptics,
                        icon = Icons.Rounded.Vibration,
                    )
                    ToggleRow(
                        title = stringResource(R.string.settings_reduce_motion),
                        supporting = stringResource(R.string.settings_reduce_motion_hint),
                        checked = appearance.reduceMotion,
                        onCheckedChange = actions::setReduceMotion,
                        icon = Icons.Rounded.Animation,
                    )
                }
            }
            if (tracking != null) {
                item(key = "tracking") {
                    SectionHeader(stringResource(R.string.settings_section_tracking))
                    ConnectedGroup {
                        ChoiceRow(
                            title = stringResource(R.string.settings_week_start),
                            supporting = stringResource(R.string.settings_week_start_hint),
                            icon = Icons.Rounded.CalendarMonth,
                            options = WeekStart.entries,
                            selected = tracking.weekStart,
                            label = {
                                stringResource(
                                    when (it) {
                                        WeekStart.LOCALE -> R.string.settings_week_auto
                                        WeekStart.MONDAY -> R.string.settings_week_monday
                                        WeekStart.SUNDAY -> R.string.settings_week_sunday
                                        WeekStart.SATURDAY -> R.string.settings_week_saturday
                                    }
                                )
                            },
                            onSelect = actions::setWeekStart,
                        )
                        NavigationRow(
                            title = stringResource(R.string.settings_currency),
                            supporting = tracking.currency?.let { stringResource(R.string.settings_currency_chosen, it) }
                                ?: stringResource(R.string.settings_currency_device, Tracking.deviceCurrency()),
                            icon = Icons.Rounded.Payments,
                            badgeKey = "currency",
                            onClick = { editCurrency = true },
                        )
                    }
                }
            }
            item(key = "about") {
                SectionHeader(stringResource(R.string.settings_section_about))
                ConnectedGroup {
                    NavigationRow(
                        title = stringResource(R.string.about_title),
                        supporting = stringResource(R.string.settings_about_hint),
                        icon = Icons.Rounded.Info,
                        badgeKey = "about",
                        onClick = onAbout,
                    )
                }
            }
        }
    }
}

/** Choose the currency new money fields start with, or go back to the device's. */
@Composable
private fun CurrencySheet(current: String?, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var code by rememberSaveable { mutableStateOf(current ?: Tracking.deviceCurrency()) }
    val valid = Tracking.isCurrency(code)
    EonslateBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.settings_currency)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.settings_currency_sheet_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            FormField(
                value = code,
                onValueChange = { code = it.uppercase().filter(Char::isLetter) },
                label = stringResource(R.string.settings_currency_code),
                error = if (code.length == 3 && !valid) stringResource(R.string.settings_currency_unknown) else null,
                capitalization = KeyboardCapitalization.Characters,
                maxLength = 3,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { onSave(null) }) {
                    Text(stringResource(R.string.settings_currency_use_device))
                }
                Spacer(Modifier.weight(1f))
                Button(shapes = ButtonDefaults.shapes(), onClick = { onSave(code) }, enabled = valid) {
                    Text(stringResource(R.string.settings_currency_save))
                }
            }
        }
    }
}

/** Eonslate's palettes as slate tiles, each showing its primary container with its ember accent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetPicker(selected: String?, onSelect: (String) -> Unit) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemePreset.entries.forEach { preset ->
                val scheme = remember(preset, dark) { eonslateColorScheme(preset.seed, dark) }
                val isSelected = preset.name == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.selectable(selected = isSelected, role = Role.RadioButton) {
                        haptics.tick()
                        onSelect(preset.name)
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(SlateTileShape)
                            .background(scheme.primaryContainer)
                            .then(if (isSelected) Modifier.border(3.dp, scheme.primary, SlateTileShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(18.dp).clip(SlateTileShape).background(scheme.tertiary))
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                            )
                        }
                    }
                    Text(presetName(preset), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun presetName(preset: ThemePreset): String = stringResource(
    when (preset) {
        ThemePreset.SLATE -> R.string.preset_slate
        ThemePreset.TIDE -> R.string.preset_tide
        ThemePreset.MOSS -> R.string.preset_moss
        ThemePreset.CLAY -> R.string.preset_clay
        ThemePreset.DUSK -> R.string.preset_dusk
        ThemePreset.GRAPHITE -> R.string.preset_graphite
    }
)
