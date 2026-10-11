package com.apexlap.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.apexlap.domain.repository.F1Repository
import com.apexlap.domain.usecase.Clock
import com.apexlap.domain.usecase.PlanRemindersUseCase
import com.apexlap.domain.usecase.PlannedReminder
import com.apexlap.domain.usecase.currentSeason
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the cached calendar + user settings into AlarmManager alarms. Idempotent: call it whenever the
 * calendar, the settings or the device clock change (app start, settings screen, boot, periodic sync).
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: F1Repository,
    private val store: ReminderSettingsStore,
    private val clock: Clock,
    private val plan: PlanRemindersUseCase,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    /** Exact alarms need a user grant on Android 12+; without it reminders may fire a few minutes late. */
    val canScheduleExact: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun upcoming(): List<PlannedReminder> {
        val calendar = repository.observeCalendar(clock.currentSeason()).first()
        return plan(calendar, clock.now(), store.settings.first())
    }

    suspend fun reschedule() = mutex.withLock {
        val planned = upcoming()
        val plannedIds = planned.map { it.id }.toSet()
        (store.scheduledIds() - plannedIds).forEach(::cancel)
        planned.forEach(::schedule)
        store.setScheduledIds(plannedIds)
    }

    private fun schedule(reminder: PlannedReminder) {
        val trigger = reminder.triggerAt.toEpochMilli()
        val intent = pendingIntent(reminder.id, ReminderReceiver.intent(context, reminder))
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
    }

    private fun cancel(id: Int) {
        val existing = PendingIntent.getBroadcast(
            context, id, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    private fun pendingIntent(id: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
