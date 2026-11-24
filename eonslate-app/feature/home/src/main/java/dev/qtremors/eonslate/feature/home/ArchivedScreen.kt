package dev.qtremors.eonslate.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.ui.TrackerSlate
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.LoadingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ArchivedViewModel @Inject constructor(repository: TrackerRepository) : ViewModel() {
    /** Archived trackers in home order; null until the first read. */
    val trackers: StateFlow<List<Tracker>?> = repository.observeTrackers(includeArchived = true)
        .map { all -> all.filter { it.archived } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Trackers put away from Home. Each opens its detail page, where it can be brought back. */
@Composable
fun ArchivedScreen(
    onBack: () -> Unit,
    onOpenTracker: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedViewModel = hiltViewModel(),
) {
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()
    ArchivedContent(trackers = trackers, onBack = onBack, onOpenTracker = onOpenTracker, modifier = modifier)
}

/** The archived page for a given list: null while loading, then the empty state or the slates. */
@Composable
fun ArchivedContent(
    trackers: List<Tracker>?,
    onBack: () -> Unit,
    onOpenTracker: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    EonslateScaffold(title = stringResource(R.string.archived_title), onBack = onBack, modifier = modifier) { padding ->
        val list = trackers
        when {
            list == null -> LoadingState(Modifier.fillMaxSize().padding(padding))
            list.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Inventory2,
                title = stringResource(R.string.archived_empty_title),
                message = stringResource(R.string.archived_empty_message),
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { tracker ->
                    TrackerSlate(
                        name = tracker.name,
                        iconName = tracker.iconName,
                        colorIndex = tracker.colorIndex,
                        key = tracker.id,
                        supporting = stringResource(R.string.archived_supporting),
                        onClick = { onOpenTracker(tracker.id) },
                    )
                }
            }
        }
    }
}
