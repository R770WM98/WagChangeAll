package com.thisismine.myapplication.feature.reminders.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.thisismine.myapplication.data.local.WagChangeDatabase
import com.thisismine.myapplication.data.repository.RoomWagChangeRepository
import com.thisismine.myapplication.feature.reminders.data.WagChangeReminderRepository

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in supportedActions) return

        val userManager = context.getSystemService(UserManager::class.java)
        val isUserUnlocked = userManager?.isUserUnlocked ?: true
        if (!shouldReschedule(action, isUserUnlocked)) return

        val pendingResult = goAsync()
        Thread {
            runCatching {
                val appContext = context.applicationContext
                val repository = WagChangeReminderRepository(
                    wagChangeRepository = RoomWagChangeRepository(WagChangeDatabase.create(appContext))
                )
                val scheduler = AlarmReminderScheduler(appContext)
                ReminderRescheduler(repository, scheduler).rescheduleAll()
            }
            pendingResult.finish()
        }.start()
    }

    internal fun shouldReschedule(action: String?, isUserUnlocked: Boolean): Boolean {
        if (action !in supportedActions) return false
        return when (action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> false
            Intent.ACTION_USER_UNLOCKED -> true
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> isUserUnlocked
            else -> false
        }
    }

    private companion object {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
