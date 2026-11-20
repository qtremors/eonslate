package dev.qtremors.eonslate.feature.settings

import dev.qtremors.eonslate.core.settings.SmsSettings
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Sms
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.pm.PackageManager
import android.Manifest
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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import dev.qtremors.eonslate.core.ui.theme.presetColorScheme

/** What the settings page changes, so it can be shown without a view model in previews and tests. */
interface SettingsActions {
    fun setTheme(theme: ThemeChoice)
    fun setPureBlack(on: Boolean)
    fun setAccent(accent: AccentChoice)
    fun setPreset(name: String)
    fun setHaptics(on: Boolean)
    fun setReduceMotion(on: Boolean)
    fun setBlockScreenshots(on: Boolean) {}
    fun setWeekStart(start: WeekStart) {}
    fun setCurrency(code: String?) {}
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit,
    onBackup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onCategories: (() -> Unit)? = null,
    onDisplay: (() -> Unit)? = null,
    /** Opens the payment SMS scan on Transactions. */
    onScanSms: (() -> Unit)? = null,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val sms by viewModel.sms.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : SettingsActions {
            override fun setTheme(theme: ThemeChoice) = viewModel.setTheme(theme)
            override fun setPureBlack(on: Boolean) = viewModel.setPureBlack(on)
            override fun setAccent(accent: AccentChoice) = viewModel.setAccent(accent)
            override fun setPreset(name: String) = viewModel.setPreset(name)
            override fun setHaptics(on: Boolean) = viewModel.setHaptics(on)
            override fun setReduceMotion(on: Boolean) = viewModel.setReduceMotion(on)
            override fun setBlockScreenshots(on: Boolean) = viewModel.setBlockScreenshots(on)
            override fun setWeekStart(start: WeekStart) = viewModel.setWeekStart(start)
            override fun setCurrency(code: String?) = viewModel.setCurrency(code)
        }
    }
    SettingsContent(
        appearance, actions, onBack, onAbout, modifier,
        tracking = tracking, onBackup = onBackup, sms = sms, onSms = viewModel::updateSms,
        onCategories = onCategories, onDisplay = onDisplay, onScanSms = onScanSms,
    )
}

/**
 * The settings page in the order the design system sets: appearance, accent, behavior, tracking,
 * data, about.
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
    onBackup: () -> Unit = {},
    sms: SmsSettings? = null,
    onSms: ((SmsSettings) -> SmsSettings) -> Unit = {},
    onCategories: (() -> Unit)? = null,
    onDisplay: (() -> Unit)? = null,
    onScanSms: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val askSms = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onSms { it.copy(detect = true) }
    }
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
                        supporting = if (appearance.theme == ThemeChoice.LIGHT) stringResource(R.string.settings_pure_black_off) else null,
                        checked = appearance.pureBlack,
                        onCheckedChange = actions::setPureBlack,
                        icon = Icons.Rounded.Contrast,
                        enabled = appearance.theme != ThemeChoice.LIGHT,
                    )
                }
            }
            item(key = "presets") {
                SectionHeader(stringResource(R.string.settings_section_accent))
                AccentPicker(
                    wallpaper = appearance.accent == AccentChoice.WALLPAPER,
                    preset = appearance.preset,
                    wallpaperAvailable = wallpaperAvailable,
                    onWallpaper = { actions.setAccent(AccentChoice.WALLPAPER) },
                    onPreset = actions::setPreset,
                )
            }
            item(key = "behavior") {
                SectionHeader(stringResource(R.string.settings_section_behavior))
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.settings_haptics),
                        checked = appearance.haptics,
                        onCheckedChange = actions::setHaptics,
                        icon = Icons.Rounded.Vibration,
                    )
                    ToggleRow(
                        title = stringResource(R.string.settings_reduce_motion),
                        checked = appearance.reduceMotion,
                        onCheckedChange = actions::setReduceMotion,
                        icon = Icons.Rounded.Animation,
                    )
                }
            }
            item(key = "privacy") {
                SectionHeader(stringResource(R.string.privacy_title))
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.settings_block_screenshots),
                        checked = appearance.blockScreenshots,
                        onCheckedChange = actions::setBlockScreenshots,
                    )
                }
            }
            if (tracking != null) {
                item(key = "tracking") {
                    SectionHeader(stringResource(R.string.settings_section_tracking))
                    ConnectedGroup {
                        ChoiceRow(
                            title = stringResource(R.string.settings_week_start),
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
                        if (onCategories != null) NavigationRow(
                            title = stringResource(R.string.settings_categories),
                            icon = Icons.Rounded.Label,
                            badgeKey = "categories",
                            onClick = onCategories,
                        )
                    }
                }
            }
            if (onDisplay != null) item(key = "display") {
                ConnectedGroup {
                    NavigationRow(
                        title = stringResource(R.string.settings_display),
                        icon = Icons.Rounded.Person,
                        badgeKey = "display",
                        onClick = onDisplay,
                    )
                }
            }
            if (sms != null) item(key = "sms") {
                SectionHeader(stringResource(R.string.settings_section_sms))
                ConnectedGroup {
                    ToggleRow(
                        title = stringResource(R.string.settings_sms_detect),
                        checked = sms.detect,
                        onCheckedChange = { on ->
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                            if (!on) onSms { it.copy(detect = false) }
                            else if (granted) onSms { it.copy(detect = true) }
                            else askSms.launch(Manifest.permission.READ_SMS)
                        },
                        icon = Icons.Rounded.Sms,
                    )
                    ToggleRow(
                        title = stringResource(R.string.settings_sms_auto),
                        checked = sms.autoApprove,
                        onCheckedChange = { on -> onSms { it.copy(autoApprove = on) } },
                        icon = Icons.Rounded.DoneAll,
                        enabled = sms.detect,
                    )
                    if (onScanSms != null) NavigationRow(
                        title = stringResource(R.string.settings_sms_scan),
                        icon = Icons.Rounded.Sms,
                        badgeKey = "sms-scan",
                        onClick = onScanSms,
                    )
                }
            }
            item(key = "data") {
                SectionHeader(stringResource(R.string.settings_section_data))
                ConnectedGroup {
                    NavigationRow(
                        title = stringResource(R.string.settings_backup),
                        icon = Icons.Rounded.SettingsBackupRestore,
                        badgeKey = "backup",
                        onClick = onBackup,
                    )
                }
            }
            item(key = "about") {
                SectionHeader(stringResource(R.string.settings_section_about))
                ConnectedGroup {
                    NavigationRow(
                        title = stringResource(R.string.about_title),
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

/**
 * Every accent as a slate tile showing the color itself on its container: the wallpaper's (on
 * Android 12 and up), then Mono and Eonslate's presets. Exactly one is chosen at a time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(
    wallpaper: Boolean,
    preset: String,
    wallpaperAvailable: Boolean,
    onWallpaper: () -> Unit,
    onPreset: (String) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val dark = colors.surface.luminance() < 0.5f
    Surface(color = colors.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (wallpaperAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val scheme = remember(dark) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
                    AccentTile(
                        name = stringResource(R.string.settings_colors_wallpaper),
                        container = scheme.primaryContainer,
                        accent = scheme.primary,
                        mark = scheme.onPrimaryContainer,
                        selected = wallpaper,
                        icon = Icons.Rounded.Wallpaper,
                    ) {
                        haptics.tick()
                        onWallpaper()
                    }
                }
                ThemePreset.entries.forEach { option ->
                    val scheme = remember(option, dark) { presetColorScheme(option, dark) }
                    AccentTile(
                        name = presetName(option),
                        container = if (option == ThemePreset.MONO) scheme.surfaceContainerHighest else scheme.primaryContainer,
                        accent = scheme.primary,
                        mark = if (option == ThemePreset.MONO) scheme.onSurface else scheme.onPrimaryContainer,
                        selected = !wallpaper && option.name == preset,
                    ) {
                        haptics.tick()
                        onPreset(option.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentTile(
    name: String,
    container: Color,
    accent: Color,
    mark: Color,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(SlateTileShape)
                .background(container)
                .then(if (selected) Modifier.border(3.dp, accent, SlateTileShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            } else {
                Box(Modifier.size(18.dp).clip(SlateTileShape).background(accent))
            }
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = mark,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                )
            }
        }
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun presetName(preset: ThemePreset): String = stringResource(
    when (preset) {
        ThemePreset.MONO -> R.string.preset_mono
        ThemePreset.SLATE -> R.string.preset_slate
        ThemePreset.TIDE -> R.string.preset_tide
        ThemePreset.MOSS -> R.string.preset_moss
        ThemePreset.OCHRE -> R.string.preset_ochre
        ThemePreset.CLAY -> R.string.preset_clay
        ThemePreset.ROSE -> R.string.preset_rose
        ThemePreset.DUSK -> R.string.preset_dusk
        ThemePreset.GRAPHITE -> R.string.preset_graphite
    }
)
