package com.apexlap.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apexlap.domain.model.SessionType
import com.apexlap.i18n.I18n
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.apexlap.MainActivity
import com.apexlap.R
import com.apexlap.domain.usecase.PlannedReminder
import com.apexlap.ui.components.flagOf
import com.apexlap.ui.components.localTime
import com.apexlap.ui.components.relativeStart
import java.time.Instant

/** Fired by AlarmManager a few minutes before a session; posts the reminder notification. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val startsAt = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_STARTS_AT, 0))
        val season = intent.getIntExtra(EXTRA_SEASON, 0)
        val round = intent.getIntExtra(EXTRA_ROUND, 0)
        val strings = I18n.strings
        // Stored as the SessionType name; reminders planned by older versions hold a ready-made label.
        val session = intent.getStringExtra(EXTRA_SESSION).orEmpty()
            .let { raw -> runCatching { strings.session(SessionType.valueOf(raw)) }.getOrDefault(raw) }
        val race = intent.getStringExtra(EXTRA_RACE).orEmpty()
        val country = intent.getStringExtra(EXTRA_COUNTRY).orEmpty()

        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, id, MainActivity.raceIntent(context, season, round),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${flagOf(country)} $session · $race")
            .setContentText(strings.startsIn(relativeStart(Instant.now(), startsAt), startsAt.localTime()))
            .setColor(ContextCompat.getColor(context, R.color.racing_red))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "session_reminders"
        private const val EXTRA_ID = "id"
        private const val EXTRA_STARTS_AT = "starts_at"
        private const val EXTRA_SEASON = "season"
        private const val EXTRA_ROUND = "round"
        private const val EXTRA_SESSION = "session"
        private const val EXTRA_RACE = "race"
        private const val EXTRA_COUNTRY = "country"

        fun intent(context: Context, reminder: PlannedReminder): Intent =
            Intent(context, ReminderReceiver::class.java)
                .putExtra(EXTRA_ID, reminder.id)
                .putExtra(EXTRA_STARTS_AT, reminder.session.startsAt?.toEpochMilli() ?: 0L)
                .putExtra(EXTRA_SEASON, reminder.race.season)
                .putExtra(EXTRA_ROUND, reminder.race.round)
                .putExtra(EXTRA_SESSION, reminder.session.type.name)
                .putExtra(EXTRA_RACE, reminder.race.name)
                .putExtra(EXTRA_COUNTRY, reminder.race.circuit.country)

        fun ensureChannel(context: Context) {
            val strings = I18n.strings
            val channel = NotificationChannel(CHANNEL_ID, strings.sessionReminders, NotificationManager.IMPORTANCE_HIGH)
                .apply { description = strings.reminderChannelDescription }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
