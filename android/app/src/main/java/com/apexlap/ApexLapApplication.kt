package com.apexlap

import android.app.Application
import com.apexlap.reminders.ReminderReceiver
import com.apexlap.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApexLapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.ensureChannel(this)
        SyncWorker.schedulePeriodic(this)
        SyncWorker.syncNow(this)
    }
}
