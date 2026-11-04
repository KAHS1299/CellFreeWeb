package com.example.cellfreeapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Helper para gestión de notificaciones del sistema
 */
object NotificationHelper {
    
    const val CHANNEL_ID = "usage_monitoring_channel"
    const val CHANNEL_NAME = "Monitoreo de Uso"
    const val CHANNEL_DESCRIPTION = "Notificaciones del sistema de monitoreo automático de aplicaciones"
    
    /**
     * Crea el canal de notificación necesario para el servicio foreground
     * Debe llamarse antes de usar notificaciones
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Crea una notificación básica para el servicio de monitoreo
     */
    fun createMonitoringNotification(context: Context, message: String = "Monitoreando aplicaciones...") =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("📊 CellFree Monitor")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
}