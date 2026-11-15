package dev.qtremors.eonslate.feature.welcome

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
import dev.qtremors.eonslate.core.tracker.ui.Templates
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.TrackerIcons
import dev.qtremors.eonslate.core.ui.components.rememberAppHaptics
import dev.qtremors.eonslate.core.ui.theme.SlateCardShape
import dev.qtremors.eonslate.core.ui.theme.SlateTileShape

/** Templates offered on the second page: the ones most people start with. */
internal val STARTERS = listOf("habit", "mood", "water", "expenses", "sleep", "workout", "reading", "journal")

/**
 * The first run, two pages at most: what Eonslate is, then a few trackers to start with. Both
 * can be skipped, and nothing here is needed to use the app.
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit, viewModel: WelcomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    BackHandler(enabled = state.page == 1) { viewModel.showPage(0) }

    val finish: (List<String>) -> Unit = { ids ->
        viewModel.finish { currency ->
            ids.mapNotNull(Templates::byId).map { spec ->
                Templates.instantiate(spec, context::getString, UuidIdGenerator, currency, now = 0)
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = state.page, label = "welcomePage") { page ->
            when (page) {
                0 -> IntroPage(
                    onNext = { viewModel.showPage(1) },
                    onSkip = { finish(emptyList()) },
                )
                else -> StartersPage(
                    picked = state.picked,
                    saving = state.saving,
                    onToggle = { id ->
                        haptics.toggle(id !in state.picked)
                        viewModel.toggle(id)
                    },
                    onStart = {
                        haptics.confirm()
                        finish(state.picked)
                    },
                    onSkip = { finish(emptyList()) },
                )
            }
        }
    }
}

@Composable
private fun IntroPage(onNext: () -> Unit, onSkip: () -> Unit) {
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
            colors = ButtonDefaults.buttonColors(containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) { Text(stringResource(R.string.welcome_next)) }
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

@Composable
private fun StartersPage(
    picked: List<String>,
    saving: Boolean,
    onToggle: (String) -> Unit,
    onStart: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 32.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.welcome_starters_title), style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
                    Text(stringResource(R.string.welcome_starters_subtitle), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
            }
            items(STARTERS.mapNotNull(Templates::byId), key = { it.id }) { spec ->
                val isPicked = spec.id in picked
                Box(
                    Modifier
                        .then(if (isPicked) Modifier.border(3.dp, colors.tertiary, SlateCardShape) else Modifier)
                        .semantics {
                            selected = isPicked
                            role = Role.Checkbox
                        }
                ) {
                    TrackerSlate(
                        name = stringResource(spec.name),
                        iconName = spec.iconName,
                        colorIndex = spec.colorIndex,
                        key = spec.id,
                        supporting = stringResource(spec.description),
                        onClick = { onToggle(spec.id) },
                        action = {
                            if (isPicked) {
                                Box(
                                    Modifier.size(32.dp).clip(SlateTileShape).background(colors.tertiaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onTertiaryContainer)
                                }
                            }
                        },
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = onStart,
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (picked.isEmpty()) stringResource(R.string.welcome_start_empty)
                    else pluralStringResource(R.plurals.welcome_start_with, picked.size, picked.size)
                )
            }
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onSkip, enabled = !saving) { Text(stringResource(R.string.welcome_skip)) }
        }
    }
}
