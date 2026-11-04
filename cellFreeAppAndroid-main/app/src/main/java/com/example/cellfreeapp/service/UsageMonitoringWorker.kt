package com.example.cellfreeapp.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.cellfreeapp.data.repository.ReportsRepository
import com.example.cellfreeapp.utils.UsageStatsHelper
import com.example.cellfreeapp.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Worker para monitoreo automático del uso de aplicaciones
 * Registra los incrementos de uso diario de las aplicaciones.
 */
class UsageMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "UsageMonitoringWorker"
        const val WORK_NAME = "usage_monitoring_work"
        const val MINIMUM_MONITORING_INTERVAL = 15L
        const val NOTIFICATION_ID = 1001
        private const val REPORT_PROGRESS_PREFERENCES = "usage_report_progress"
        private const val KEY_REPORT_DATE = "report_date"
        
        /**
         * Programa el monitoreo automático con el intervalo elegido.
         */
        fun schedulePeriodicMonitoring(context: Context, intervalMinutes: Long) {
            val safeIntervalMinutes = intervalMinutes.coerceAtLeast(MINIMUM_MONITORING_INTERVAL)
            if (safeIntervalMinutes != intervalMinutes) {
                Log.w(
                    TAG,
                    "Intervalo $intervalMinutes inválido; se usará el mínimo de " +
                        "$MINIMUM_MONITORING_INTERVAL minutos"
                )
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val monitoringRequest = PeriodicWorkRequestBuilder<UsageMonitoringWorker>(
                safeIntervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    MINIMUM_MONITORING_INTERVAL, TimeUnit.MINUTES
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                monitoringRequest
            )
            
            Log.i(TAG, "Monitoreo automático programado cada $safeIntervalMinutes minutos")
        }

        /**
         * Cancela el monitoreo automático
         */
        fun cancelMonitoring(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Monitoreo automático cancelado")
        }

        /**
         * Verifica si el monitoreo está activo
         */
        fun isMonitoringActive(context: Context, callback: (Boolean) -> Unit) {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(WORK_NAME)
                .observeForever { workInfos ->
                    val isActive = workInfos?.any { 
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED 
                    } ?: false
                    callback(isActive)
                }
        }
    }

    private lateinit var reportsRepository: ReportsRepository
    private lateinit var usageStatsHelper: UsageStatsHelper
    private val reportPreferences by lazy {
        applicationContext.getSharedPreferences(
            REPORT_PROGRESS_PREFERENCES,
            Context.MODE_PRIVATE
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val configManager = MonitoringConfigManager(applicationContext)
            if (!configManager.isMonitoringEnabled()) {
                Log.i(TAG, "Monitoreo deshabilitado; se omite el registro")
                return@withContext Result.success()
            }

            val currentTime = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(currentTime))
            prepareReportProgressForDate(today)
            Log.i(
                TAG,
                "Iniciando reporte de uso: intervaloConfiguradoMin=" +
                    "${configManager.getMonitoringInterval()}, fecha=$today"
            )
            
            // Inicializar componentes
            reportsRepository = ReportsRepository(applicationContext)
            usageStatsHelper = UsageStatsHelper(applicationContext)
            
            // Verificar permisos de uso de aplicaciones
            if (!usageStatsHelper.hasUsageStatsPermission()) {
                Log.w(TAG, "Sin permisos de estadísticas de uso")
                return@withContext Result.failure()
            }

            val usageData = usageStatsHelper.getTodayUsageStatsForReporting(currentTime)
            val launchCounts = usageStatsHelper.getTodayAppLaunchCounts(currentTime)
            val usageByPackage = usageData.associateBy { it.packageName }
            val packagesToReport = (usageByPackage.keys + launchCounts.keys)
                .filter { it != applicationContext.packageName }
            var registeredCount = 0
            var hasRegistrationFailure = false

            for (packageName in packagesToReport) {
                val appUsage = usageByPackage[packageName]
                val currentUsageMinutes =
                    ((appUsage?.totalTimeInForeground ?: 0L) /
                        TimeUnit.MINUTES.toMillis(1)).toInt()
                val usageProgressKey = "usage_minutes:$packageName"
                val launchesProgressKey = "launch_count:$packageName"
                val lastReportedUsageMinutes =
                    reportPreferences.getInt(usageProgressKey, 0)
                val lastReportedLaunchCount =
                    reportPreferences.getInt(launchesProgressKey, 0)
                val currentLaunchCount = launchCounts[packageName] ?: 0

                if (
                    currentUsageMinutes < lastReportedUsageMinutes ||
                    currentLaunchCount < lastReportedLaunchCount
                ) {
                    Log.w(
                        TAG,
                        "Contador diario retrocedió para $packageName; " +
                            "se conserva el último avance enviado"
                    )
                    continue
                }

                val usageDeltaMinutes = currentUsageMinutes - lastReportedUsageMinutes
                val launchDelta = currentLaunchCount - lastReportedLaunchCount
                if (usageDeltaMinutes == 0 && launchDelta == 0) continue

                val result = reportsRepository.registerUsage(
                    nombreApp = usageStatsHelper.getAppDisplayName(packageName),
                    categoria = determineAppCategory(packageName),
                    tiempoUsoMinutos = usageDeltaMinutes,
                    numeroAperturas = launchDelta
                )

                if (result.isSuccess) {
                    val progressSaved = reportPreferences.edit()
                        .putInt(usageProgressKey, currentUsageMinutes)
                        .putInt(launchesProgressKey, currentLaunchCount)
                        .commit()
                    if (!progressSaved) {
                        hasRegistrationFailure = true
                        Log.e(
                            TAG,
                            "El servidor registró el uso de $packageName, pero no se pudo " +
                                "guardar el avance local"
                        )
                        continue
                    }
                    registeredCount++
                    configManager.incrementAppsMonitored(1)
                    Log.d(
                        TAG,
                        "Uso registrado: paquete=$packageName, " +
                            "incrementoMin=$usageDeltaMinutes, aperturas=$launchDelta"
                    )
                } else {
                    hasRegistrationFailure = true
                    Log.e(
                        TAG,
                        "Error registrando uso de $packageName; se reintentará sin avanzar " +
                            "el contador local",
                        result.exceptionOrNull()
                    )
                }
            }

            if (hasRegistrationFailure) {
                return@withContext Result.retry()
            }
            configManager.setLastMonitoringTime(currentTime)
            Log.i(
                TAG,
                "Reporte completado: $registeredCount registros enviados de " +
                    "${packagesToReport.size} aplicaciones con actividad"
            )
            if (registeredCount > 0) sendProgressNotification(registeredCount)
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en monitoreo automático: ${e.message}", e)
            Result.retry()
        }
    }

    private fun prepareReportProgressForDate(today: String) {
        val savedDate = reportPreferences.getString(KEY_REPORT_DATE, null)
        if (savedDate == today) return

        val saved = reportPreferences.edit()
            .clear()
            .putString(KEY_REPORT_DATE, today)
            .commit()
        if (!saved) {
            throw IllegalStateException("No se pudo guardar la fecha de avance del reporte")
        }
        Log.i(TAG, "Avance diario de reportes inicializado para $today")
    }

    /**
     * Determina la categoría de una aplicación basada en su package name
     */
    private fun determineAppCategory(packageName: String): String {
        return when {
            packageName.contains("whatsapp") || 
            packageName.contains("telegram") || 
            packageName.contains("messenger") -> "Comunicación"
            
            packageName.contains("youtube") || 
            packageName.contains("netflix") || 
            packageName.contains("spotify") -> "Entretenimiento"
            
            packageName.contains("chrome") || 
            packageName.contains("firefox") || 
            packageName.contains("browser") -> "Navegación"
            
            packageName.contains("game") || 
            packageName.contains("play.games") -> "Juegos"
            
            packageName.contains("camera") || 
            packageName.contains("gallery") || 
            packageName.contains("photos") -> "Fotografía"
            
            packageName.contains("office") || 
            packageName.contains("docs") || 
            packageName.contains("sheets") -> "Productividad"
            
            else -> "Otros"
        }
    }

    /**
     * Envía notificación de progreso del monitoreo
     */
    private suspend fun sendProgressNotification(registeredCount: Int) {
        val notification = NotificationHelper.createMonitoringNotification(
            applicationContext, 
            "$registeredCount apps monitoreadas automáticamente"
        )
        setForeground(ForegroundInfo(NOTIFICATION_ID, notification))
    }

}