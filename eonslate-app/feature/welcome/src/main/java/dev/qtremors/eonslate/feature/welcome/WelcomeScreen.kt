package dev.qtremors.eonslate.feature.welcome

import dev.qtremors.eonslate.core.ui.components.rememberFocusOnOpen
import dev.qtremors.eonslate.core.ui.components.FormField
import dev.qtremors.eonslate.core.ui.components.ConnectedChoices
import dev.qtremors.eonslate.core.ui.components.Avatar
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.time.UuidIdGenerator
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape


/**
 * The first run: what Eonslate is, your name, your money, and a few trackers to start with.
 * Every page can be skipped, and nothing here is needed to use the app. Someone moving from
 * another phone can restore a backup instead.
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit, onRestore: () -> Unit = onDone, viewModel: WelcomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    LaunchedEffect(state.done) { if (state.done) (if (state.restore) onRestore else onDone)() }
    BackHandler(enabled = state.page != WelcomePage.INTRO) { viewModel.back() }

    val defaultAccount = stringResource(R.string.welcome_account_default)
    val finish: () -> Unit = {
        haptics.confirm()
        viewModel.finish(defaultAccount)
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = state.page, label = "welcomePage") { page ->
            when (page) {
                WelcomePage.INTRO -> IntroPage(onNext = viewModel::next, onSkip = viewModel::skip, onRestore = viewModel::restore)
                WelcomePage.YOU -> Step(
                    page, onBack = viewModel::back, onSkip = viewModel::skip,
                    nextLabel = stringResource(R.string.welcome_continue), onNext = viewModel::next,
                ) { YouPage(state.name, viewModel::setName) }
                WelcomePage.MONEY -> Step(
                    page, onBack = viewModel::back, onSkip = viewModel::skip,
                    nextLabel = stringResource(R.string.welcome_start), onNext = finish, nextEnabled = state.moneyReady && !state.saving,
                ) { MoneyPage(state, viewModel) }
            }
        }
    }
}

/**
 * One step after the intro: a back arrow and how far along it is at the top, the page, then
 * the button that moves on. Skip leaves the rest to the app's defaults.
 */
@Composable
private fun Step(
    page: WelcomePage,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    scrolls: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val steps = WelcomePage.entries.size - 1
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.welcome_back))
            }
            LinearWavyProgressIndicator(
                progress = { page.ordinal.toFloat() / steps },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onSkip) { Text(stringResource(R.string.welcome_skip)) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .then(if (scrolls) Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp) else Modifier),
            content = content,
        )
        Button(
            shapes = ButtonDefaults.shapes(),
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        ) { Text(nextLabel) }
    }
}

/** What to call you, shown in your profile. */
@Composable
private fun ColumnScope.YouPage(name: String, onName: (String) -> Unit) {
    val focus = rememberFocusOnOpen(enabled = true)
    PageTitle(stringResource(R.string.welcome_you_title), stringResource(R.string.welcome_you_subtitle))
    Avatar(name, size = 88.dp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 24.dp))
    FormField(
        value = name,
        onValueChange = onName,
        label = stringResource(R.string.welcome_you_name),
        supporting = stringResource(R.string.welcome_you_hint),
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Done,
        modifier = focus,
    )
}

/** The currency money is kept in, and the first account with what it holds today. */
@Composable
private fun MoneyPage(state: WelcomeState, viewModel: WelcomeViewModel) {
    val numbered = state.accountKind == AccountKind.BANK || state.accountKind == AccountKind.CARD
    PageTitle(stringResource(R.string.welcome_money_title), stringResource(R.string.welcome_money_subtitle))
    Column(Modifier.padding(top = 24.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField(
            value = state.currency,
            onValueChange = viewModel::setCurrency,
            label = stringResource(R.string.welcome_money_currency),
            supporting = stringResource(R.string.welcome_money_currency_hint),
            error = if (state.currency.length == 3 && !state.currencyKnown) stringResource(R.string.welcome_money_currency_unknown) else null,
            capitalization = KeyboardCapitalization.Characters,
            maxLength = 3,
        )
        Text(
            stringResource(R.string.welcome_money_account),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
        ConnectedChoices(AccountKind.EVERYDAY, state.accountKind, { kindLabel(it) }, viewModel::setAccountKind)
        FormField(
            value = state.accountName,
            onValueChange = viewModel::setAccountName,
            label = stringResource(R.string.welcome_money_account_name),
            placeholder = stringResource(R.string.welcome_account_default),
        )
        FormField(
            value = state.opening,
            onValueChange = viewModel::setOpening,
            label = stringResource(R.string.welcome_money_opening),
            supporting = stringResource(R.string.welcome_money_opening_hint),
            error = if (state.openingMinor == null) stringResource(R.string.welcome_money_opening_invalid) else null,
            keyboardType = KeyboardType.Decimal,
            suffix = state.currency.takeIf { state.currencyKnown },
        )
        if (numbered) FormField(
            value = state.digits,
            onValueChange = viewModel::setDigits,
            label = stringResource(R.string.welcome_money_digits),
            supporting = stringResource(R.string.welcome_money_digits_hint),
            error = if (state.digitsValid) null else stringResource(R.string.welcome_money_digits_invalid),
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            maxLength = 4,
        )
    }
}

@Composable
private fun kindLabel(kind: AccountKind): String = stringResource(
    when (kind) {
        AccountKind.BANK, AccountKind.DEPOSIT -> R.string.welcome_kind_bank
        AccountKind.CARD -> R.string.welcome_kind_card
        AccountKind.WALLET -> R.string.welcome_kind_wallet
        AccountKind.CASH -> R.string.welcome_kind_cash
    },
)

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 16.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun IntroPage(onNext: () -> Unit, onSkip: () -> Unit, onRestore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        SlateStack()
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displayMedium, color = colors.onSurface)
        Text(
            stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Point(Icons.Rounded.Tune, stringResource(R.string.welcome_point_any_title), stringResource(R.string.welcome_point_any), 2)
            Point(Icons.Rounded.Insights, stringResource(R.string.welcome_point_see_title), stringResource(R.string.welcome_point_see), 3)
            Point(Icons.Rounded.Lock, stringResource(R.string.welcome_point_private_title), stringResource(R.string.welcome_point_private), 0)
        }
        Button(
            shapes = ButtonDefaults.shapes(),
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) { Text(stringResource(R.string.welcome_next)) }
        TextButton(shapes = ButtonDefaults.shapes(), onClick = onRestore) { Text(stringResource(R.string.welcome_restore)) }
        TextButton(shapes = ButtonDefaults.shapes(), onClick = onSkip) { Text(stringResource(R.string.welcome_skip)) }
    }
}

/** Three tracker slates leaning on each other, in pigments, as the hero. */
@Composable
private fun SlateStack() {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tiles = listOf(Triple("Mood", 5, -10f), Triple("LocalDrink", 3, 0f), Triple("FitnessCenter", 8, 10f))
    Box(Modifier.size(width = 240.dp, height = 120.dp), contentAlignment = Alignment.Center) {
        tiles.forEachIndexed { index, (icon, color, angle) ->
            val badge = remember(color, dark) { BadgePalette.forIndex(color, dark) }
            Box(
                modifier = Modifier
                    .offset(x = ((index - 1) * 68).dp, y = if (index == 1) (-8).dp else 8.dp)
                    .rotate(angle)
                    .size(96.dp)
                    .clip(SlateCardShape)
                    .background(badge.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(TrackerIcons.resolve(icon), contentDescription = null, tint = badge.content, modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, text: String, colorIndex: Int) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = SlateCardShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, key = title, colorIndex = colorIndex)
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
