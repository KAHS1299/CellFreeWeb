package com.example.cellfreeapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AppOpsManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import com.example.cellfreeapp.R
import com.example.cellfreeapp.data.model.BlockedApp
import com.example.cellfreeapp.data.repository.BlockedAppsRepository
import kotlinx.coroutines.*
import java.util.Calendar
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppBlockerService : Service() {

    private val blockedAppsRepository by lazy { BlockedAppsRepository(this) }
    private val usagePreferences by lazy {
        getSharedPreferences("AppBlockerUsage", Context.MODE_PRIVATE)
    }
    private val refreshMutex = Mutex()
    private var blockedAppsList = emptyList<BlockedApp>()
    private var monitoringJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val liveUsageMillisByPackage = mutableMapOf<String, Long>()
    private var lastBlockedPackage: String? = null
    private var lastObservedForegroundPackage: String? = null
    private var hasObservedForegroundPackage = false
    private var lastLoggedMatchingPackage: String? = null
    private var lastLoggedUnmatchedPackage: String? = null
    private val resumedActivities = linkedMapOf<String, String>()
    private var usageDayKey = ""
    private var usageEventQueryTime = 0L
    private var foregroundStateInitialized = false
    private var lastCheckElapsedRealtime = 0L
    private var lastUsageLogElapsedRealtime = 0L
    private var encouragementOverlay: View? = null
    private val invalidScheduleWarnings = mutableSetOf<String>()
    private var usagePermissionWarningLogged = false

    companion object {
        const val ACTION_REFRESH_BLOCKED_APPS = "com.example.cellfreeapp.REFRESH_BLOCKED_APPS"
        private const val TAG = "BLOCKINGAPP"
        private const val CHECK_INTERVAL_MS = 1_000L
        private const val REFRESH_INTERVAL_MS = 60_000L
        private const val USAGE_LOG_INTERVAL_MS = 15_000L
        private const val ENCOURAGEMENT_CHANNEL_ID = "app_blocker_encouragement"
        private const val ENCOURAGEMENT_NOTIFICATION_ID = 2
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio creado")
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}, monitoringActive=${monitoringJob?.isActive == true}")
        if (monitoringJob?.isActive != true) {
            if (!hasUsageStatsPermission()) {
                Log.e(TAG, "Servicio no iniciado: falta permiso de acceso a estadísticas de uso")
                stopSelf(startId)
                return START_NOT_STICKY
            }
            monitoringJob = serviceScope.launch(Dispatchers.IO) {
                try {
                    refreshBlockedApps()
                } catch (e: Exception) {
                    Log.e(TAG, "Error inesperado en la carga inicial de bloqueos", e)
                }
                var nextRefreshTime = android.os.SystemClock.elapsedRealtime() + REFRESH_INTERVAL_MS
                while (isActive) {
                    try {
                        if (!hasUsageStatsPermission()) {
                            if (!usagePermissionWarningLogged) {
                                Log.e(TAG, "Permiso de acceso a estadísticas de uso revocado; deteniendo monitoreo")
                                usagePermissionWarningLogged = true
                            }
                            stopSelf()
                            break
                        }
                        usagePermissionWarningLogged = false

                        val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
                        if (elapsedRealtime >= nextRefreshTime) {
                            refreshBlockedApps()
                            nextRefreshTime =
                                android.os.SystemClock.elapsedRealtime() + REFRESH_INTERVAL_MS
                        }
                        checkForegroundApp()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al revisar la aplicación en primer plano", e)
                    }
                    delay(CHECK_INTERVAL_MS)
                }
            }
        } else if (intent?.action == ACTION_REFRESH_BLOCKED_APPS) {
            serviceScope.launch {
                refreshBlockedApps()
            }
        }
        return START_STICKY
    }

    private suspend fun refreshBlockedApps() {
        refreshMutex.withLock {
            val result = blockedAppsRepository.getEnabledBlockedApps()
            if (result.isSuccess) {
                blockedAppsList = result.getOrThrow()
                Log.d(TAG, "Configuraciones habilitadas cargadas: ${blockedAppsList.size}")
                Log.d(
                    TAG,
                    "Paquetes activos: ${blockedAppsList.mapNotNull { it.nombrePaquete }}"
                )
            } else {
                Log.e(
                    TAG,
                    "No se pudieron cargar los bloqueos activos; se conserva la última lista válida",
                    result.exceptionOrNull()
                )
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun checkForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        val currentAppPackage = getForegroundPackage(usageStatsManager, currentTime)
        val previousForegroundPackage = lastObservedForegroundPackage
        val foregroundPackageChanged =
            !hasObservedForegroundPackage || currentAppPackage != lastObservedForegroundPackage
        if (foregroundPackageChanged) {
            Log.d(
                TAG,
                "App detectada en primer plano por UsageEvents: ${currentAppPackage ?: "<ninguna>"}"
            )
            hasObservedForegroundPackage = true
            lastObservedForegroundPackage = currentAppPackage
        }

        ensureUsageDay(currentTime)
        val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
        if (
            lastCheckElapsedRealtime > 0L &&
            previousForegroundPackage != null &&
            currentAppPackage == previousForegroundPackage
        ) {
            liveUsageMillisByPackage[previousForegroundPackage] =
                (liveUsageMillisByPackage[previousForegroundPackage] ?: 0L) +
                    (elapsedRealtime - lastCheckElapsedRealtime).coerceAtLeast(0L)
        }
        lastCheckElapsedRealtime = elapsedRealtime

        if (currentAppPackage != lastBlockedPackage) {
            lastBlockedPackage = null
        }

        val configuredApp = blockedAppsList.firstOrNull {
            it.nombrePaquete == currentAppPackage && it.estado
        }
        val blockedApp = configuredApp?.takeIf {
            isWithinConfiguredDates(it, currentTime)
        }
        if (blockedApp == null) {
            lastBlockedPackage = null
            if (currentAppPackage != null && currentAppPackage != lastLoggedUnmatchedPackage) {
                val reason = if (configuredApp == null) {
                    "sin configuración habilitada"
                } else {
                    "fuera de su periodo configurado"
                }
                Log.d(
                    TAG,
                    "No se aplica bloqueo a $currentAppPackage: $reason"
                )
                lastLoggedUnmatchedPackage = currentAppPackage
            }
            lastLoggedMatchingPackage = null
            return
        }
        val packageName = currentAppPackage ?: return
        lastLoggedUnmatchedPackage = null

        val limitMillis = blockedApp.tiempoBloqueo?.toLong()?.times(60_000L)
        val reportedUsageMillis = getTodayUsageMillis(usageStatsManager, packageName, currentTime)
        val usagePreferenceKey = usagePreferenceKey(packageName)
        val storedUsageMillis = usagePreferences.getLong(usagePreferenceKey, 0L)
        val usageMillis = maxOf(
            reportedUsageMillis,
            storedUsageMillis,
            liveUsageMillisByPackage.getOrPut(packageName) {
                maxOf(reportedUsageMillis, storedUsageMillis)
            }
        )
        liveUsageMillisByPackage[packageName] = usageMillis
        if (usageMillis - storedUsageMillis >= USAGE_LOG_INTERVAL_MS) {
            usagePreferences.edit().putLong(usagePreferenceKey, usageMillis).apply()
        }

        if (
            packageName != lastLoggedMatchingPackage ||
            elapsedRealtime - lastUsageLogElapsedRealtime >= USAGE_LOG_INTERVAL_MS
        ) {
            Log.d(
                TAG,
                "Bloqueo coincidente: paquete=$packageName, tiempoBloqueoMin=${blockedApp.tiempoBloqueo}, " +
                    "usoHoyMs=$usageMillis"
            )
            lastLoggedMatchingPackage = packageName
            lastUsageLogElapsedRealtime = elapsedRealtime
        }

        if (limitMillis != null && usageMillis < limitMillis) {
            return
        }

        if (lastBlockedPackage != packageName) {
            if (limitMillis == null) {
                Log.d(TAG, "Sin tiempoBloqueo; se intentará bloquear de inmediato: paquete=$packageName")
            } else {
                Log.d(
                    TAG,
                    "Límite alcanzado: paquete=$packageName, " +
                        "usoHoyMs=$usageMillis, limiteMs=$limitMillis"
                )
            }
            lastBlockedPackage = packageName
            Log.d(TAG, "Ejecutando acción de bloqueo para $packageName")
            withContext(Dispatchers.Main.immediate) {
                blockApp(blockedApp.nombreApp)
            }
        }
    }

    private fun isWithinConfiguredDates(app: BlockedApp, currentTime: Long): Boolean {
        val startText = app.fechaInicio
        if (startText != null) {
            val startTime = parseConfiguredDate(startText)
            if (startTime == null) {
                logInvalidScheduleOnce(app, "fecha_inicio", startText)
                return false
            }
            if (currentTime < startTime) return false
        }

        val endText = app.fechaFin
        if (endText != null) {
            val endTime = parseConfiguredDate(endText)
            if (endTime == null) {
                logInvalidScheduleOnce(app, "fecha_fin", endText)
                return false
            }
            if (currentTime > endTime) return false
        }
        return true
    }

    private fun parseConfiguredDate(value: String): Long? {
        val trimmedValue = value.trim()
        val offsetMatch = Regex("([+-]\\d{2}):(\\d{2})$").find(trimmedValue)
        val normalizedValue = when {
            trimmedValue.endsWith("Z", ignoreCase = true) ->
                trimmedValue.dropLast(1) + "+0000"
            offsetMatch != null ->
                trimmedValue.dropLast(offsetMatch.value.length) +
                    offsetMatch.groupValues[1] + offsetMatch.groupValues[2]
            else -> trimmedValue
        }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (pattern in patterns) {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }
            val position = ParsePosition(0)
            val date = formatter.parse(normalizedValue, position)
            if (date != null && position.index == normalizedValue.length) {
                return date.time
            }
        }
        return null
    }

    private fun logInvalidScheduleOnce(app: BlockedApp, field: String, value: String) {
        val key = "${app.id ?: app.nombrePaquete}:$field"
        if (invalidScheduleWarnings.add(key)) {
            Log.e(TAG, "No se aplicará el bloqueo ${app.nombreApp}: $field inválida: $value")
        }
    }

    private fun getForegroundPackage(
        usageStatsManager: UsageStatsManager,
        currentTime: Long
    ): String? {
        val startTime = if (foregroundStateInitialized) {
            usageEventQueryTime
        } else {
            startOfTodayMillis()
        }
        val events = usageStatsManager.queryEvents(startTime, currentTime)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            val activityName = event.className ?: packageName
            val activityKey = "$packageName/$activityName"

            when {
                isActivityResumedEvent(event.eventType) -> {
                    resumedActivities.remove(activityKey)
                    resumedActivities[activityKey] = packageName
                }
                isActivityPausedEvent(event.eventType) -> {
                    resumedActivities.remove(activityKey)
                }
                isActivityBackgroundEvent(event.eventType) -> {
                    resumedActivities.entries.removeAll { it.value == packageName }
                }
                isScreenOffEvent(event.eventType) -> {
                    resumedActivities.clear()
                }
            }
        }

        usageEventQueryTime = currentTime
        foregroundStateInitialized = true
        return resumedActivities.values.lastOrNull()
    }

    private fun isActivityResumedEvent(eventType: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }

    private fun isActivityPausedEvent(eventType: Int): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                eventType == UsageEvents.Event.ACTIVITY_STOPPED)

    private fun isActivityBackgroundEvent(eventType: Int): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            eventType == UsageEvents.Event.MOVE_TO_BACKGROUND

    private fun isScreenOffEvent(eventType: Int): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE ||
                eventType == UsageEvents.Event.KEYGUARD_SHOWN)

    private fun getTodayUsageMillis(
        usageStatsManager: UsageStatsManager,
        packageName: String,
        currentTime: Long
    ): Long {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfTodayMillis(),
            currentTime
        )
        return stats.orEmpty()
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
    }

    private fun ensureUsageDay(currentTime: Long) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(currentTime))
        if (usageDayKey != today) {
            usageDayKey = today
            liveUsageMillisByPackage.clear()
            lastBlockedPackage = null
            lastCheckElapsedRealtime = 0L
            lastUsageLogElapsedRealtime = 0L
            Log.d(TAG, "Nuevo día de uso ($today): progreso diario y estado de bloqueo reiniciados")
        }
    }

    private fun usagePreferenceKey(packageName: String): String =
        "$usageDayKey:$packageName"

    private fun startOfTodayMillis(): Long =
        Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

    private fun blockApp(appName: String) {
        if (!showEncouragementOverlay(appName)) {
            showEncouragementNotification(appName)
        }
        val startMain = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        Log.d(TAG, "Solicitando abrir la pantalla de inicio (ACTION_MAIN/CATEGORY_HOME)")
        startActivity(startMain)
        Log.d(TAG, "La solicitud para abrir Inicio fue aceptada por startActivity")
    }

    private fun showEncouragementOverlay(appName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No se mostró la alerta modal: falta permiso de superposición")
            return false
        }

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        encouragementOverlay?.let { existingOverlay ->
            windowManager.removeView(existingOverlay)
            encouragementOverlay = null
        }

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val cardBackground = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(20).toFloat()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(20))
            background = cardBackground
            elevation = dp(12).toFloat()
        }
        content.addView(
            TextView(this).apply {
                text = "¡Buen trabajo!"
                textSize = 23f
                setTextColor(Color.rgb(34, 96, 65))
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        content.addView(
            TextView(this).apply {
                text = context.getString(R.string.message_encouragement, appName)
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(20))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        content.addView(
            Button(this).apply {
                text = "Entendido"
                setOnClickListener { removeEncouragementOverlay() }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val layoutParams = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dp(420)),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.45f
        }

        return try {
            windowManager.addView(content, layoutParams)
            encouragementOverlay = content
            Log.d(TAG, "Alerta modal de ánimo mostrada para $appName")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Android rechazó la alerta modal de ánimo", e)
            false
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "No se pudo agregar la alerta modal de ánimo a la ventana", e)
            false
        }
    }

    private fun removeEncouragementOverlay() {
        val overlay = encouragementOverlay ?: return
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(overlay)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "La alerta modal ya no estaba adjunta a la ventana", e)
        } finally {
            encouragementOverlay = null
        }
    }

    private fun showEncouragementNotification(appName: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "No se mostró el mensaje de ánimo: falta permiso de notificaciones")
            return
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Log.w(TAG, "No se mostró el mensaje de ánimo: notificaciones desactivadas")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ENCOURAGEMENT_CHANNEL_ID,
                "Mensajes de ánimo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mensajes positivos al alcanzar el límite de uso de una aplicación"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, ENCOURAGEMENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("¡Buen trabajo!")
            .setContentText("Alcanzaste el límite de $appName. Mantén el bloqueo y sigue con tus objetivos.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Alcanzaste el límite de uso de $appName. " +
                        "Mantén el bloqueo activo y sigue con tus objetivos. ¡Tú puedes!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(ENCOURAGEMENT_NOTIFICATION_ID, notification)
            Log.d(TAG, "Notificación de ánimo mostrada para $appName")
        } catch (e: SecurityException) {
            Log.e(TAG, "Android rechazó la notificación de ánimo", e)
        }
    }

    private fun startInForeground() {
        val channelId = "app_blocker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Modo Enfoque Activo")
            .setContentText("Estamos protegiendo tu productividad.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Recomendado
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }
        Log.d(TAG, "Servicio iniciado en primer plano")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Servicio destruido")
        removeEncouragementOverlay()
        super.onDestroy()
        serviceScope.cancel()
    }
}
