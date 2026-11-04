package com.example.cellfreeapp.service

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cellfreeapp.models.CalendarEvent
import java.util.concurrent.TimeUnit

object ReminderNotificationScheduler {

    private const val TAG = "ReminderNotification"
    private const val WORK_PREFIX = "reminder_notification_"

    fun schedule(context: Context, event: CalendarEvent) {
        val eventId = event.id
        if (eventId == null) {
            Log.w(TAG, "No se puede programar el recordatorio sin ID del servidor")
            return
        }

        if (event.completed) {
            cancel(context, eventId)
            return
        }

        val delayMillis = event.date.time - System.currentTimeMillis()
        if (delayMillis <= 0L) {
            cancel(context, eventId)
            Log.i(TAG, "No se programa un recordatorio vencido: id=$eventId")
            return
        }

        val input = Data.Builder()
            .putInt(ReminderNotificationWorker.KEY_REMINDER_ID, eventId)
            .putString(ReminderNotificationWorker.KEY_TITLE, event.title)
            .putString(ReminderNotificationWorker.KEY_DESCRIPTION, event.description)
            .putString(ReminderNotificationWorker.KEY_TYPE, event.type)
            .putLong(ReminderNotificationWorker.KEY_DATE_TIME, event.date.time)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInputData(input)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(eventId),
            ExistingWorkPolicy.REPLACE,
            request
        )
        Log.i(TAG, "Recordatorio programado: id=$eventId, demoraMs=$delayMillis")
    }

    fun cancel(context: Context, eventId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(eventId))
    }

    private fun workName(eventId: Int): String = "$WORK_PREFIX$eventId"
}
