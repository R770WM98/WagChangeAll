package com.thisismine.myapplication.feature.reminders.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.thisismine.myapplication.domain.model.ServiceReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmReminderScheduler(
    context: Context
) : ReminderNotificationScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dedupeStore = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override fun syncReminders(reminders: List<ServiceReminder>, currentOdometerKm: Int) {
        ReminderNotifications.ensureChannel(appContext)

        reminders.forEach { reminder ->
            cancelAlarm(reminder.id)
            scheduleDateAlarm(reminder)
            notifyMileageIfNeeded(reminder, currentOdometerKm)
        }
    }

    override fun cancelReminder(reminderId: String) {
        cancelAlarm(reminderId)
    }

    private fun scheduleDateAlarm(reminder: ServiceReminder) {
        val dueDate = reminder.triggerAtIsoDate.toLocalDateOrNull() ?: return
        val triggerAt = dueDate.atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerAt <= System.currentTimeMillis()) {
            notifyDateIfNeeded(reminder)
            return
        }

        val pendingIntent = reminderPendingIntent(
            reminderId = reminder.id,
            title = reminder.title,
            kind = ReminderNotifications.Kind.DateDue
        )

        // Use inexact while-idle alarm to avoid exact alarm permission requirements.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    private fun notifyMileageIfNeeded(reminder: ServiceReminder, currentOdometerKm: Int) {
        // Snoozed reminders carry a future trigger date; skip km notifications until that date.
        val triggerDate = reminder.triggerAtIsoDate.toLocalDateOrNull()
        if (triggerDate != null && triggerDate.isAfter(LocalDate.now())) return

        val nextDueKm = nextTriggerKm(reminder, currentOdometerKm)
        val kmRemaining = nextDueKm - currentOdometerKm
        if (kmRemaining > 300) return

        val level = if (kmRemaining <= 0) "overdue" else "due-soon"
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dedupeKey = "km-${reminder.id}"
        val last = dedupeStore.getString(dedupeKey, null)
        if (last == "$level-$todayKey") return

        val detail = if (kmRemaining <= 0) {
            "Overdue by ${-kmRemaining} km"
        } else {
            "Due in $kmRemaining km"
        }

        ReminderNotifications.showReminderNotification(
            context = appContext,
            reminderId = reminder.id,
            title = reminder.title,
            body = detail,
            kind = ReminderNotifications.Kind.MileageDue
        )
        dedupeStore.edit { putString(dedupeKey, "$level-$todayKey") }
    }

    private fun notifyDateIfNeeded(reminder: ServiceReminder) {
        val todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dedupeKey = "date-${reminder.id}"
        if (dedupeStore.getString(dedupeKey, null) == todayKey) return

        val dueDate = reminder.triggerAtIsoDate.toLocalDateOrNull()
        val body = when {
            dueDate == null -> "Service date reminder"
            dueDate.isAfter(LocalDate.now()) -> "Service date reminder is coming soon"
            dueDate.isEqual(LocalDate.now()) -> "Service date reminder: due today"
            else -> {
                val overdueDays = LocalDate.now().toEpochDay() - dueDate.toEpochDay()
                "Service date overdue by $overdueDays days"
            }
        }

        ReminderNotifications.showReminderNotification(
            context = appContext,
            reminderId = reminder.id,
            title = reminder.title,
            body = body,
            kind = ReminderNotifications.Kind.DateDue
        )
        dedupeStore.edit { putString(dedupeKey, todayKey) }
    }

    private fun cancelAlarm(reminderId: String) {
        val intent = Intent(appContext, ReminderNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun reminderPendingIntent(
        reminderId: String,
        title: String,
        kind: ReminderNotifications.Kind
    ): PendingIntent {
        val intent = Intent(appContext, ReminderNotificationReceiver::class.java).apply {
            putExtra(ReminderNotifications.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderNotifications.EXTRA_TITLE, title)
            putExtra(ReminderNotifications.EXTRA_BODY, "Service date reminder: due today")
            putExtra(ReminderNotifications.EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerKm(reminder: ServiceReminder, currentKm: Int): Int {
        val interval = reminder.repeatingIntervalKm ?: return reminder.triggerAtKm
        if (interval <= 0) return reminder.triggerAtKm

        val base = reminder.lastTriggeredAtKm ?: reminder.triggerAtKm
        val nextIfNoCycle = base.toLong() + interval.toLong()
        if (currentKm.toLong() < nextIfNoCycle) return nextIfNoCycle.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        val delta = (currentKm.toLong() - base.toLong()).coerceAtLeast(0L)
        val cycles = (delta / interval.toLong()) + 1L
        val next = base.toLong() + (cycles * interval.toLong())
        return next.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun String?.toLocalDateOrNull(): LocalDate? {
        if (this.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    private companion object {
        const val PREFS_FILE = "reminder-notification-dedupe"
    }
}
