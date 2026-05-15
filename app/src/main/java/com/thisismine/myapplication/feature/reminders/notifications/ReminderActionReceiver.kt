package com.thisismine.myapplication.feature.reminders.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.thisismine.myapplication.data.local.MaintenanceEntryEntity
import com.thisismine.myapplication.data.local.ServiceReminderEntity
import com.thisismine.myapplication.data.local.WagChangeDatabase
import com.thisismine.myapplication.domain.model.ServiceReminder
import java.time.LocalDate
import java.util.UUID

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderNotifications.EXTRA_REMINDER_ID) ?: return
        val actionType = ReminderNotifications.actionToType(intent.action) ?: return

        val pendingResult = goAsync()
        Thread {
            runCatching {
                val appContext = context.applicationContext
                val database = WagChangeDatabase.create(appContext)
                val reminderDao = database.reminderDao()
                val maintenanceDao = database.maintenanceDao()
                val motorcycleDao = database.motorcycleDao()

                val entity = reminderDao.getById(reminderId) ?: return@runCatching
                val currentOdometer = motorcycleDao.getById(entity.motorcycleId)?.currentOdometerKm
                    ?: entity.triggerAtKm
                val motorcycleReminders = reminderDao.getByMotorcycleId(entity.motorcycleId).map { it.toDomain() }

                val result = ReminderActionEngine.apply(
                    action = actionType,
                    reminder = entity.toDomain(),
                    currentOdometerKm = currentOdometer,
                    today = LocalDate.now()
                )

                if (actionType == ReminderActionType.MarkDone) {
                    maintenanceDao.insert(
                        MaintenanceEntryEntity(
                            id = UUID.randomUUID().toString(),
                            motorcycleId = entity.motorcycleId,
                            title = entity.title,
                            dateIso = LocalDate.now().toString(),
                            odometerKm = currentOdometer,
                            totalCostPhp = 0.0,
                            notes = "Auto-logged from reminder completion (${entity.id})",
                            costLevelOverride = "AUTO",
                            imageUris = ""
                        )
                    )
                }

                if (result.shouldArchive) {
                    reminderDao.delete(entity)
                    AlarmReminderScheduler(appContext).cancelReminder(reminderId)
                } else {
                    val updated = result.updatedReminder ?: return@runCatching
                    reminderDao.update(updated.toEntity())
                    AlarmReminderScheduler(appContext).syncReminders(
                        reminders = motorcycleReminders.map { reminder ->
                            if (reminder.id == updated.id) updated else reminder
                        },
                        currentOdometerKm = currentOdometer
                    )
                }

                NotificationManagerCompat.from(appContext).cancel(reminderId.hashCode())
            }
            pendingResult.finish()
        }.start()
    }
}

private fun ServiceReminderEntity.toDomain(): ServiceReminder = ServiceReminder(
    id = id,
    motorcycleId = motorcycleId,
    title = title,
    triggerAtKm = triggerAtKm,
    triggerAtIsoDate = triggerAtIsoDate,
    repeatingIntervalKm = repeatingIntervalKm,
    lastTriggeredAtKm = lastTriggeredAtKm,
    completionCount = completionCount,
    lastCompletedAtIsoDate = lastCompletedAtIsoDate,
    isArchived = isArchived
)

private fun ServiceReminder.toEntity(): ServiceReminderEntity = ServiceReminderEntity(
    id = id,
    motorcycleId = motorcycleId,
    title = title,
    triggerAtKm = triggerAtKm,
    triggerAtIsoDate = triggerAtIsoDate,
    repeatingIntervalKm = repeatingIntervalKm,
    lastTriggeredAtKm = lastTriggeredAtKm,
    completionCount = completionCount,
    lastCompletedAtIsoDate = lastCompletedAtIsoDate,
    isArchived = isArchived
)
