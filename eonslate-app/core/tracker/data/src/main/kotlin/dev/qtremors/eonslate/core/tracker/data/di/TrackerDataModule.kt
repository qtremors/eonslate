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
import dev.qtremors.eonslate.core.tracker.data.repository.RoomEntryRepository
import dev.qtremors.eonslate.core.tracker.data.repository.RoomTrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.time.SystemAppClock
import dev.qtremors.eonslate.core.tracker.domain.time.UuidIdGenerator
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
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackerRepositoryModule {
    @Binds
    abstract fun bindTrackerRepository(impl: RoomTrackerRepository): TrackerRepository

    @Binds
    abstract fun bindEntryRepository(impl: RoomEntryRepository): EntryRepository
}
