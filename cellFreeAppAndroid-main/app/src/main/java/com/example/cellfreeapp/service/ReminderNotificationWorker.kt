package com.example.cellfreeapp.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cellfreeapp.CalendarActivity
import com.example.cellfreeapp.R

class ReminderNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_TYPE = "type"
        const val KEY_DATE_TIME = "date_time"

        private const val TAG = "ReminderNotification"
        private const val CHANNEL_ID = "calendar_reminders"
        private const val CHANNEL_NAME = "Recordatorios"
    }

    override suspend fun doWork(): Result {
        val reminderId = inputData.getInt(KEY_REMINDER_ID, -1)
        val title = inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() }
        if (reminderId < 0 || title == null) {
            Log.e(TAG, "Datos inválidos; no se puede mostrar el recordatorio")
            return Result.failure()
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "No se mostró el recordatorio $reminderId: falta permiso de notificaciones")
            return Result.success()
        }

        createNotificationChannel()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            Log.w(TAG, "No se mostró el recordatorio $reminderId: notificaciones deshabilitadas")
            return Result.success()
        }

        val dateTime = inputData.getLong(KEY_DATE_TIME, 0L)
        val openCalendarIntent = Intent(applicationContext, CalendarActivity::class.java).apply {
            putExtra(CalendarActivity.EXTRA_SELECTED_DATE, dateTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId,
            openCalendarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val description = inputData.getString(KEY_DESCRIPTION).orEmpty()
        val type = inputData.getString(KEY_TYPE).orEmpty()
        val message = description.ifBlank { "Recordatorio de ${type.ifBlank { "evento" }}" }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(reminderId, notification)
        Log.i(TAG, "Notificación mostrada para recordatorio $reminderId")
        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos para tareas, exámenes y sesiones de estudio"
        }
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
