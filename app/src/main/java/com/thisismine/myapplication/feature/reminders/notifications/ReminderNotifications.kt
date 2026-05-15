package com.thisismine.myapplication.feature.reminders.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thisismine.myapplication.MainActivity
import com.thisismine.myapplication.R

object ReminderNotifications {
    const val CHANNEL_ID = "service_reminders"
    const val CHANNEL_NAME = "Service reminders"
    const val CHANNEL_DESCRIPTION = "Due and upcoming service reminder alerts"

    const val ACTION_SNOOZE = "com.thisismine.myapplication.feature.reminders.notifications.ACTION_SNOOZE"
    const val ACTION_MARK_DONE = "com.thisismine.myapplication.feature.reminders.notifications.ACTION_MARK_DONE"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    @Suppress("unused")
    const val EXTRA_KIND = "extra_kind"

    enum class Kind {
        DateDue,
        MileageDue,
        Snoozed,
        Completed
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        body: String,
        kind: Kind = inferKind(body)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(kind.smallIconRes())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(actionButton(context, ACTION_SNOOZE, R.drawable.ic_reminder_snooze, R.string.reminder_action_snooze, reminderId, title, body, Kind.Snoozed))
            .addAction(actionButton(context, ACTION_MARK_DONE, R.drawable.ic_reminder_done, R.string.reminder_action_done, reminderId, title, body, Kind.Completed))
            .addAction(openAction(context, reminderId))
            .build()

        manager.notify(reminderId.hashCode(), notification)
    }

    fun actionToType(action: String?): ReminderActionType? = when (action) {
        ACTION_SNOOZE -> ReminderActionType.Snooze
        ACTION_MARK_DONE -> ReminderActionType.MarkDone
        else -> null
    }

    private fun inferKind(body: String): Kind = when {
        body.contains("km", ignoreCase = true) -> Kind.MileageDue
        body.contains("overdue", ignoreCase = true) || body.contains("due today", ignoreCase = true) -> Kind.DateDue
        body.contains("snooz", ignoreCase = true) -> Kind.Snoozed
        body.contains("completed", ignoreCase = true) -> Kind.Completed
        else -> Kind.DateDue
    }

    private fun Kind.smallIconRes(): Int = when (this) {
        Kind.DateDue -> android.R.drawable.ic_menu_my_calendar
        Kind.MileageDue -> android.R.drawable.ic_menu_compass
        Kind.Snoozed -> android.R.drawable.ic_lock_idle_alarm
        Kind.Completed -> android.R.drawable.checkbox_on_background
    }

    private fun actionButton(
        context: Context,
        action: String,
        iconRes: Int,
        titleRes: Int,
        reminderId: String,
        reminderTitle: String,
        body: String,
        kind: Kind
    ): Action {
        val intent = actionIntent(
            context = context,
            action = action,
            reminderId = reminderId,
            title = reminderTitle,
            body = body,
            kind = kind
        )
        return Action(iconRes, context.getString(titleRes), intent)
    }

    private fun openAction(context: Context, reminderId: String): Action {
        val intent = PendingIntent.getActivity(
            context,
            reminderId.hashCode() + 1000,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Action(
            R.drawable.ic_reminder_open,
            context.getString(R.string.reminder_action_open),
            intent
        )
    }

    private fun actionIntent(
        context: Context,
        action: String,
        reminderId: String,
        title: String,
        body: String,
        kind: Kind
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_KIND, kind.name)
        }
        val requestCode = reminderId.hashCode() + action.hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
