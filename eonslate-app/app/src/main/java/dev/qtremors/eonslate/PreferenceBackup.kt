package dev.qtremors.eonslate

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qtremors.eonslate.core.settings.SettingsRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.PreferenceStore
import javax.inject.Inject

/** Lets backups carry the settings, which live apart from the database. */
class SettingsPreferenceStore @Inject constructor(private val settings: SettingsRepository) : PreferenceStore {
    override suspend fun export(): Map<String, String> = settings.export()

    override suspend fun replace(values: Map<String, String>) = settings.replace(values)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferenceBackupModule {
    @Binds
    abstract fun bindPreferenceStore(store: SettingsPreferenceStore): PreferenceStore
}
