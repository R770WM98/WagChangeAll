package com.thisismine.myapplication.feature.reminders.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderNotifications.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(ReminderNotifications.EXTRA_BODY)
            ?: "Service reminder is due."
        val reminderId = intent.getStringExtra(ReminderNotifications.EXTRA_REMINDER_ID) ?: return
        val kind = intent.getStringExtra(ReminderNotifications.EXTRA_KIND)
            ?.let { runCatching { ReminderNotifications.Kind.valueOf(it) }.getOrNull() }
            ?: ReminderNotifications.Kind.DateDue

        ReminderNotifications.showReminderNotification(
            context = context,
            reminderId = reminderId,
            title = title,
            body = body,
            kind = kind
        )
    }
}

