package com.example.cellfreeapp.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Gestor de configuración del monitoreo automático
 */
class MonitoringConfigManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "monitoring_config"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_MONITORING_INTERVAL = "monitoring_interval"
        private const val KEY_LAST_MONITORING_TIME = "last_monitoring_time"
        private const val KEY_TOTAL_APPS_MONITORED = "total_apps_monitored"
        private const val KEY_MONITORING_START_TIME = "monitoring_start_time"
        private const val TAG = "MonitoringConfigManager"
        
        // Intervalos predefinidos en minutos
        const val INTERVAL_15_MINUTES = 15L
        const val INTERVAL_30_MINUTES = 30L
        const val INTERVAL_1_HOUR = 60L
        const val INTERVAL_2_HOURS = 120L
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Habilita o deshabilita el monitoreo automático
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
        Log.i(TAG, "Monitoreo ${if (enabled) "habilitado" else "deshabilitado"}")
        
        if (enabled) {
            // Iniciar el monitoreo
            UsageMonitoringWorker.schedulePeriodicMonitoring(context, getMonitoringInterval())
            // Registrar hora de inicio si es la primera vez
            if (getMonitoringStartTime() == 0L) {
                setMonitoringStartTime(System.currentTimeMillis())
            }
        } else {
            // Detener el monitoreo
            UsageMonitoringWorker.cancelMonitoring(context)
        }
    }
    
    /**
     * Verifica si el monitoreo está habilitado
     */
    fun isMonitoringEnabled(): Boolean {
        return prefs.getBoolean(KEY_MONITORING_ENABLED, false)
    }
    
    /**
     * Establece el intervalo de monitoreo en minutos
     */
    fun setMonitoringInterval(intervalMinutes: Long) {
        val safeIntervalMinutes =
            intervalMinutes.coerceAtLeast(UsageMonitoringWorker.MINIMUM_MONITORING_INTERVAL)
        prefs.edit().putLong(KEY_MONITORING_INTERVAL, safeIntervalMinutes).apply()
        Log.i(TAG, "Intervalo de monitoreo establecido a $safeIntervalMinutes minutos")
        
        // Si el monitoreo está activo, reprogramar con el nuevo intervalo
        if (isMonitoringEnabled()) {
            UsageMonitoringWorker.schedulePeriodicMonitoring(context, safeIntervalMinutes)
        }
    }
    
    /**
     * Obtiene el intervalo de monitoreo actual
     */
    fun getMonitoringInterval(): Long {
        return prefs.getLong(KEY_MONITORING_INTERVAL, INTERVAL_15_MINUTES)
            .coerceAtLeast(UsageMonitoringWorker.MINIMUM_MONITORING_INTERVAL)
    }
    
    /**
     * Registra la última vez que se ejecutó el monitoreo
     */
    fun setLastMonitoringTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_MONITORING_TIME, timestamp).apply()
    }
    
    /**
     * Obtiene la última vez que se ejecutó el monitoreo
     */
    fun getLastMonitoringTime(): Long {
        return prefs.getLong(KEY_LAST_MONITORING_TIME, 0L)
    }
    
    /**
     * Incrementa el contador de apps monitoreadas
     */
    fun incrementAppsMonitored(count: Int) {
        val currentTotal = getTotalAppsMonitored()
        prefs.edit().putLong(KEY_TOTAL_APPS_MONITORED, currentTotal + count).apply()
    }
    
    /**
     * Obtiene el total de apps monitoreadas
     */
    fun getTotalAppsMonitored(): Long {
        return prefs.getLong(KEY_TOTAL_APPS_MONITORED, 0L)
    }
    
    /**
     * Establece la hora de inicio del monitoreo
     */
    private fun setMonitoringStartTime(timestamp: Long) {
        prefs.edit().putLong(KEY_MONITORING_START_TIME, timestamp).apply()
    }
    
    /**
     * Obtiene la hora de inicio del monitoreo
     */
    fun getMonitoringStartTime(): Long {
        return prefs.getLong(KEY_MONITORING_START_TIME, 0L)
    }
    
    /**
     * Resetea todas las estadísticas de monitoreo
     */
    fun resetMonitoringStats() {
        prefs.edit()
            .remove(KEY_TOTAL_APPS_MONITORED)
            .remove(KEY_LAST_MONITORING_TIME)
            .remove(KEY_MONITORING_START_TIME)
            .apply()
        Log.i(TAG, "Estadísticas de monitoreo reseteadas")
    }
    
    /**
     * Obtiene un resumen del estado del monitoreo
     */
    fun getMonitoringSummary(): MonitoringSummary {
        return MonitoringSummary(
            enabled = isMonitoringEnabled(),
            intervalMinutes = getMonitoringInterval(),
            lastExecutionTime = getLastMonitoringTime(),
            totalAppsMonitored = getTotalAppsMonitored(),
            startTime = getMonitoringStartTime()
        )
    }
    
    /**
     * Obtiene el texto del intervalo para mostrar en UI
     */
    fun getIntervalDisplayText(intervalMinutes: Long): String {
        return when (intervalMinutes) {
            INTERVAL_15_MINUTES -> "Cada 15 minutos"
            INTERVAL_30_MINUTES -> "Cada 30 minutos"
            INTERVAL_1_HOUR -> "Cada 1 hora"
            INTERVAL_2_HOURS -> "Cada 2 horas"
            else -> "Cada $intervalMinutes minutos"
        }
    }
}

/**
 * Data class para el resumen del monitoreo
 */
data class MonitoringSummary(
    val enabled: Boolean,
    val intervalMinutes: Long,
    val lastExecutionTime: Long,
    val totalAppsMonitored: Long,
    val startTime: Long
)