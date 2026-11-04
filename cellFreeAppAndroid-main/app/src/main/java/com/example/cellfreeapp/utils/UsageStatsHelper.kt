package com.example.cellfreeapp.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.*

/**
 * Helper para manejar estadísticas de uso de aplicaciones
 */
class UsageStatsHelper(private val context: Context) {
    
    companion object {
        const val TAG = "UsageStatsHelper"
    }
    
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    /**
     * Verifica si la app tiene permisos para acceder a estadísticas de uso
     */
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            Log.d(TAG, "Verificando permisos de uso: mode=$mode")
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando permisos: ${e.message}")
            false
        }
    }

    /**
     * Abre la configuración de acceso a estadísticas de uso
     */
    fun openUsageAccessSettings(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Obtiene estadísticas de uso para un período específico
     */
    fun getUsageStatsForPeriod(startTime: Long, endTime: Long): List<android.app.usage.UsageStats> {
        return try {
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            // Filtrar apps con uso significativo (más de 1 minuto)
            usageStats.filter { 
                it.totalTimeInForeground > 60_000 // 1 minuto en ms
            }.sortedByDescending { 
                it.lastTimeUsed 
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas de uso: ${e.message}")
            emptyList()
        }
    }

    /**
     * Devuelve los contadores acumulados de hoy para calcular el incremento a reportar.
     */
    fun getTodayUsageStatsForReporting(
        endTime: Long = System.currentTimeMillis()
    ): List<android.app.usage.UsageStats> {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = endTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            endTime
        ).orEmpty()
            .filter {
                it.packageName != context.packageName &&
                    it.totalTimeInForeground > 0L
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    /**
     * Cuenta inicios de sesiones de primer plano por paquete desde el comienzo del día.
     */
    fun getTodayAppLaunchCounts(endTime: Long = System.currentTimeMillis()): Map<String, Int> {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = endTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val events = usageStatsManager.queryEvents(startOfDay, endTime)
        val event = UsageEvents.Event()
        val launchCounts = mutableMapOf<String, Int>()
        var foregroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            val isResumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }

            if (isResumed && foregroundPackage != packageName) {
                launchCounts[packageName] = (launchCounts[packageName] ?: 0) + 1
                foregroundPackage = packageName
            }
        }

        return launchCounts
    }

    /**
     * Obtiene estadísticas de uso del día actual
     */
    fun getTodayUsageStats(): List<android.app.usage.UsageStats> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        return getUsageStatsForPeriod(startTime, endTime)
    }

    /**
     * Obtiene estadísticas de uso de la última semana
     */
    fun getWeekUsageStats(): List<android.app.usage.UsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (7 * 24 * 60 * 60 * 1000L) // 7 días atrás
        
        return getUsageStatsForPeriod(startTime, endTime)
    }

    /**
     * Obtiene estadísticas de uso del último mes
     */
    fun getMonthUsageStats(): List<android.app.usage.UsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (30 * 24 * 60 * 60 * 1000L) // 30 días atrás
        
        return getUsageStatsForPeriod(startTime, endTime)
    }

    /**
     * Obtiene el nombre amigable de una aplicación
     */
    fun getAppDisplayName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            // Si no se puede obtener el nombre, devolver el package name limpio
            packageName.split(".").lastOrNull() ?: packageName
        }
    }

    /**
     * Verifica si una aplicación está instalada
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene estadísticas agregadas por día
     */
    fun getDailyUsageStats(days: Int = 7): Map<String, List<android.app.usage.UsageStats>> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)
        
        val dailyStats = mutableMapOf<String, MutableList<android.app.usage.UsageStats>>()
        
        try {
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            for (stat in usageStats) {
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(stat.firstTimeStamp))
                
                if (!dailyStats.containsKey(date)) {
                    dailyStats[date] = mutableListOf()
                }
                dailyStats[date]?.add(stat)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas diarias: ${e.message}")
        }
        
        return dailyStats
    }

    /**
     * Formatea tiempo en milisegundos a texto legible
     */
    fun formatUsageTime(timeInMillis: Long): String {
        val seconds = timeInMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}