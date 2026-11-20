package dev.qtremors.eonslate

import android.app.Application
import android.os.StrictMode
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
        // Debug builds report disk and network work on the main thread and leaked objects to Logcat.
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
        super.onCreate()
        // Alarms follow the database: any reminder, tracker or restore change reschedules.
        reminders.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
}
