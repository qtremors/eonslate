package dev.qtremors.eonslate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.qtremors.eonslate.core.reminders.ReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class EonslateApplication : Application() {
    @Inject
    lateinit var reminders: ReminderSync

    override fun onCreate() {
        super.onCreate()
        // Alarms follow the database: any reminder, tracker or restore change reschedules.
        reminders.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}
