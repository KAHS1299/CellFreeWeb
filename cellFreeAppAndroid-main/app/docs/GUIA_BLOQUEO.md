
# Guía de Implementación: Bloqueador de Aplicaciones (Anti-Procrastinación)
Esta guía detalla los pasos para crear un servicio en Android que detecte aplicaciones abiertas y bloquee aquellas seleccionadas por el usuario.

**Table of Contents**

- [Paso 1: Configuración del AndroidManifest.xml](#paso-1-configuración-del-androidmanifestxml)
- [Paso 2: Verificar y Solicitar Permisos](#paso-2-verificar-y-solicitar-permisos)
- [Paso 3: Obtener la Lista de Aplicaciones Instaladas](#paso-3-obtener-la-lista-de-aplicaciones-instaladas)
- [Paso 4: El Servicio de Monitoreo (Foreground Service)](#paso-4-el-servicio-de-monitoreo-foreground-service)
- [Próximos Pasos Recomendados](#próximos-pasos-recomendados)


## Paso 1: Configuración del AndroidManifest.xml

Para poder monitorear otras apps y mostrar una pantalla de bloqueo, necesitamos permisos especiales.
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:tools="http://schemas.android.com/tools">

    <!-- Permiso para consultar qué apps están instaladas -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <!-- Permiso para detectar qué app está en primer plano -->
    <uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />

    <!-- Permiso para mostrar notificaciones (Necesario para Foreground Services en Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

    <application>
        <!-- Registro del Servicio de Monitoreo -->
        <service
            android:name=".service.AppBlockerService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse" />
    </application>

</manifest>
```
## Paso 2: Verificar y Solicitar Permisos

El permiso PACKAGE_USAGE_STATS no se concede en tiempo de ejecución como la cámara; el usuario debe habilitarlo manualmente en los ajustes.

```kotlin
// Dentro de tu Activity o ViewModel
fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageStatsPermission(context: Context) {
    if (!checkUsageStatsPermission(context)) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }
}
```

## Paso 3: Obtener la Lista de Aplicaciones Instaladas
Necesitas mostrarle al usuario una lista para que elija cuáles bloquear.

```kotlin
fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return apps.map {
        AppInfo(
            name = it.loadLabel(pm).toString(),
            packageName = it.packageName,
            icon = it.loadIcon(pm)
        )
    }.filter {
        // Filtrar apps del sistema si se desea
        (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }
}

data class AppInfo(val name: String, val packageName: String, val icon: Drawable)
```

## Paso 4: El Servicio de Monitoreo (Foreground Service)

Este servicio se encarga de revisar constantemente qué app está abierta.

```kotlin
class AppBlockerService : Service() {

    private val blockedApps = setOf("com.instagram.android", "com.facebook.katana") // Esto vendría de tu DB (Room)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("Modo Enfoque Activo")
            .setSmallIcon(R.drawable.ic_lock)
            .build()

        startForeground(1, notification)

        // Hilo de monitoreo (Polling)
        Thread {
            while (true) {
                checkForegroundApp()
                Thread.sleep(1000) // Revisar cada segundo
            }
        }.start()

        return START_STICKY
    }

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

        if (stats != null) {
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            if (sortedStats.isNotEmpty()) {
                val currentApp = sortedStats[0].packageName

                if (blockedApps.contains(currentApp)) {
                    blockApp()
                }
            }
        }
    }

    private fun blockApp() {
        // Opción simple: Mandar al Home
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel("CHANNEL_ID", "App Blocker Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
```

## Próximos Pasos Recomendados

1. Persistencia con Room: Guarda los nombres de los paquetes de las apps seleccionadas en una base de datos local para que el servicio sepa qué bloquear incluso después de reiniciar el teléfono.
2. Interfaz de Selección: Usa un RecyclerView con ListAdapter para mostrar las apps instaladas con un Switch al lado de cada una.
3. Mejorar el Bloqueo: En lugar de enviar al Home, crea una Activity personalizada con un mensaje motivador ("¡Vuelve a trabajar!") y lánzala sobre la app bloqueada.
4. Optimización: El uso de Thread.sleep es básico. Para una app real, considera usar Corrutinas o investigar AccessibilityService, que es más eficiente pero más difícil de configurar en la Google Play Store.

