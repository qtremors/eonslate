package dev.qtremors.eonslate.core.tracker.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase
import dev.qtremors.eonslate.core.tracker.data.db.TrackerDatabase.Companion.configure
import dev.qtremors.eonslate.core.tracker.data.repository.BackupManager
import dev.qtremors.eonslate.core.tracker.data.repository.RoomBackupRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomFinanceRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomReminderRepository
import dev.qtremors.eonslate.core.tracker.domain.reminders.ReminderRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerCsvRepository
import dev.qtremors.eonslate.core.tracker.domain.backup.Backups
import dev.qtremors.eonslate.core.tracker.domain.csv.TrackerCsvRepository
import dev.qtremors.eonslate.core.tracker.domain.finance.FinanceRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.time.SystemAppClock
import dev.qtremors.eonslate.core.tracker.domain.time.UuidIdGenerator
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackerDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrackerDatabase =
        Room.databaseBuilder(context, TrackerDatabase::class.java, TrackerDatabase.NAME)
            .configure()
            .build()

    @Provides
    fun provideClock(): AppClock = SystemAppClock

    @Provides
    @Singleton
    fun provideBackups(@ApplicationContext context: Context, repository: RoomBackupRepository): Backups =
        BackupManager(repository, File(context.filesDir, "backups"), Dispatchers.Default, icons = File(context.filesDir, "icons"))

    @Provides
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackerRepositoryModule {
    @Binds
    abstract fun bindTrackerRepository(impl: RoomTrackerRepository): TrackerRepository

    @Binds
    abstract fun bindEntryRepository(impl: RoomEntryRepository): EntryRepository

    @Binds
    abstract fun bindTrackerCsvRepository(impl: RoomTrackerCsvRepository): TrackerCsvRepository

    @Binds
    abstract fun bindFinanceRepository(impl: RoomFinanceRepository): FinanceRepository

    @Binds
    abstract fun bindReminderRepository(impl: RoomReminderRepository): ReminderRepository
}
