package com.apexlap

import android.app.Application
import android.content.res.Configuration
import androidx.glance.appwidget.updateAll
import com.apexlap.i18n.I18n
import com.apexlap.reminders.ReminderReceiver
import com.apexlap.settings.SettingsStore
import com.apexlap.sync.SyncWorker
import com.apexlap.widget.NextSessionWidget
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class ApexLapApplication : Application() {

    @Inject lateinit var settings: SettingsStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // The language must be known before the first screen, widget or notification is drawn.
        I18n.apply(runBlocking { settings.settings.first().language })
        scope.launch {
            // Language or favourites changed: the widget and the notification channel follow.
            settings.settings.drop(1).collect { current ->
                I18n.apply(current.language)
                ReminderReceiver.ensureChannel(this@ApexLapApplication)
                NextSessionWidget().updateAll(this@ApexLapApplication)
            }
        }
        ReminderReceiver.ensureChannel(this)
        SyncWorker.schedulePeriodic(this)
        SyncWorker.syncNow(this)
    }

    /** "Phone language" follows the device when it changes while the app runs. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        scope.launch { I18n.apply(settings.settings.first().language) }
    }
}
