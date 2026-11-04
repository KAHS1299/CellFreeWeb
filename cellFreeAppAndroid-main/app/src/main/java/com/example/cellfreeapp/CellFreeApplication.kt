package com.example.cellfreeapp

import android.app.Application
import com.example.cellfreeapp.utils.NotificationHelper

/**
 * Clase Application personalizada para inicialización global
 */
class CellFreeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Crear canal de notificación al iniciar la app
        NotificationHelper.createNotificationChannel(this)
    }
}