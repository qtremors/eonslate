package dev.qtremors.eonslate.quicklog

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import dev.qtremors.eonslate.R
import dev.qtremors.eonslate.applyScreenshotPolicy
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.ui.components.ConnectedItem
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EmptyState
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.LoadingState
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import dev.qtremors.eonslate.toThemeOptions
import javax.inject.Inject

@AndroidEntryPoint
class TrackerPickerActivity : ComponentActivity() {
    @Inject lateinit var trackers: TrackerRepository
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyScreenshotPolicy(settings)
        val configuring = intent.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
        val manager = AppWidgetManager.getInstance(this)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        setResult(RESULT_CANCELED, result)
        val provider = ComponentName(this, LogWidgetProvider::class.java)
        if ((configuring && manager.getAppWidgetInfo(id)?.provider != provider) || (!configuring && !QuickLog.isPicker(intent))) {
            finish()
            return
        }

        setContent {
            val appearance by settings.appearance.collectAsState(initial = Appearance())
            val source = remember { trackers.observeTrackers() }
            val list by source.collectAsState(initial = null as List<Tracker>?)
            EonslateTheme(options = appearance.toThemeOptions()) {
                EonslateScaffold(title = stringResource(R.string.quick_log_choose), onBack = ::finish) { padding ->
                    val available = list
                    when {
                        available == null -> LoadingState(Modifier.fillMaxSize().padding(padding))
                        available.isEmpty() -> EmptyState(
                            icon = Icons.Rounded.Add,
                            title = stringResource(R.string.quick_log_empty),
                            message = "",
                            actionLabel = stringResource(R.string.quick_log_open),
                            onAction = {
                                startActivity(QuickLog.log(this))
                                finish()
                            },
                            modifier = Modifier.fillMaxSize().padding(padding),
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = padding,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            itemsIndexed(available, key = { _, tracker -> tracker.id }) { index, tracker ->
                                ConnectedItem(index, available.size) {
                                    ConnectedRow(tracker.name, onClick = {
                                        if (configuring) {
                                            LogWidgetProvider.select(this@TrackerPickerActivity, manager, id, tracker.id)
                                            setResult(RESULT_OK, result)
                                        } else startActivity(QuickLog.log(this@TrackerPickerActivity, tracker.id))
                                        finish()
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
