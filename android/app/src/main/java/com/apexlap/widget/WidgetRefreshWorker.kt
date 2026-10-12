package com.apexlap.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Redraws the widget at the moment chosen by [nextRefreshDelay]; each redraw plans the following one. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val placed = GlanceAppWidgetManager(applicationContext).getGlanceIds(NextSessionWidget::class.java)
        if (placed.isNotEmpty()) NextSessionWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "widget-refresh"

        fun schedule(context: Context, delay: Duration) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
