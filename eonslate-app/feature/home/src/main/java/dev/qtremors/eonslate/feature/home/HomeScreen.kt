package dev.qtremors.eonslate.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerSummary
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.components.TrackerIcons

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    HomeContent(trackers = trackers, modifier = modifier)
}

/** The home page for a given list: null while loading, then the empty state or the trackers. */
@Composable
fun HomeContent(
    trackers: List<TrackerSummary>?,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    EonslateScaffold(
        title = stringResource(R.string.home_title),
        modifier = modifier,
    ) { padding ->
        when {
            trackers == null -> LoadingState(Modifier.fillMaxSize().padding(padding))
            trackers.isEmpty() -> EmptyState(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
            ) {
                itemsIndexed(trackers, key = { _, summary -> summary.tracker.id }) { index, summary ->
                    ConnectedItem(index = index, count = trackers.size, modifier = Modifier.animateItem()) {
                        TrackerRow(summary = summary, nowMillis = nowMillis)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerRow(summary: TrackerSummary, nowMillis: Long) {
    val tracker = summary.tracker
    val count = pluralStringResource(R.plurals.home_entry_count, summary.entryCount, summary.entryCount)
    val supporting = summary.lastEntryAt?.let { last ->
        val relative = DateUtils.getRelativeTimeSpanString(last, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
        stringResource(R.string.home_entry_summary, count, relative)
    } ?: stringResource(R.string.home_no_entries)

    ConnectedRow(
        title = tracker.name,
        supporting = supporting,
        leading = {
            IconBadge(
                icon = TrackerIcons.resolve(tracker.iconName),
                key = tracker.id,
                colorIndex = tracker.colorIndex,
            )
        },
    )
}
