package dev.qtremors.eonslate.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    EonslateScaffold(
        title = stringResource(R.string.home_title),
        modifier = modifier,
    ) { padding ->
        EmptyState(
            icon = Icons.Rounded.AutoAwesome,
            title = stringResource(R.string.home_empty_title),
            message = stringResource(R.string.home_empty_message),
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
