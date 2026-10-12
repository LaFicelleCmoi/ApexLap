package com.apexlap.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.currentSeason
import com.apexlap.reminders.ReminderScheduler
import com.apexlap.settings.SettingsStore
import com.apexlap.widget.NextSessionWidget
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Background housekeeping: refresh the calendar (and standings when something is followed), re-plan reminders, redraw the widget. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, BackgroundEntryPoint::class.java)
        try {
            val season = deps.clock().currentSeason()
            deps.repository().refreshCalendar(season)
            // The widget shows the followed driver and team's standings: keep them fresh too.
            val settings = deps.settings().settings.first()
            if (settings.favoriteDriverId != null || settings.favoriteConstructorId != null) deps.repository().refreshStandings(season)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline: keep working from the cached calendar.
        }
        deps.reminderScheduler().reschedule()
        NextSessionWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "calendar-sync"
        private const val NOW = "calendar-sync-now"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun syncNow(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().build())
        }
    }
}

/** Hilt access for components the framework instantiates itself (workers, receivers, widgets). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackgroundEntryPoint {
    fun repository(): F1Repository
    fun reminderScheduler(): ReminderScheduler
    fun clock(): Clock
    fun settings(): SettingsStore
}
