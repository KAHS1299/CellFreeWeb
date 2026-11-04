package com.example.cellfreeapp

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cellfreeapp.base.BaseActivity
import com.google.android.material.button.MaterialButton
import androidx.core.net.toUri
import com.example.cellfreeapp.utils.AppBlockerService

class MainActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        if (!authRepository.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        setupBaseViews()
        
        // Mostrar botón de logout en MainActivity  
        findViewById<ImageView>(R.id.iv_logout)?.visibility = android.view.View.VISIBLE
        
        setupClickListeners()
    }
    
    override fun getScreenTitle(): String {
        return "Menú Principal"
    }

    override fun onResume() {
        super.onResume()
        // Es recomendable verificar cada vez que el usuario vuelve a la app
        checkRequiredPermissions()
    }

    private fun setupClickListeners() {
        // Botón Bloquear aplicaciones
        findViewById<MaterialButton>(R.id.btn_bloquear_apps).setOnClickListener {
            val intent = Intent(this, AppBlockerActivity::class.java)
            startActivity(intent)
        }

        // Botón Calendario
        findViewById<MaterialButton>(R.id.btn_calendario).setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // Botón Reporte de uso
        findViewById<MaterialButton>(R.id.btn_reporte_uso).setOnClickListener {
            val intent = Intent(this, UsageReportActivity::class.java)
            startActivity(intent)
        }

        // Botón Monitoreo Automático
        findViewById<MaterialButton>(R.id.btn_monitoreo_automatico).setOnClickListener {
            val intent = Intent(this, MonitoringConfigActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkRequiredPermissions() {
        // 1. Permiso de Notificaciones (Android 13+)
        // Usamos android.Manifest explícitamente para evitar confusiones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                return // Salimos para esperar a que el usuario responda al diálogo nativo
            }
        }

        // 2. Permisos Especiales (Uso y Superposición)
        val needsUsageStats = !hasUsageStatsPermission()
        val needsOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !Settings.canDrawOverlays(this)
        } else {
            false
        }

        if (needsUsageStats || needsOverlay) {
            showPermissionDialog(needsUsageStats, needsOverlay)
        } else {
            startBlockingService()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showPermissionDialog(needsUsageStats: Boolean, needsOverlay: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Permisos Necesarios")
            .setMessage("Para que CellFree pueda monitorear y bloquear aplicaciones, debes habilitar los permisos de 'Acceso a uso' y 'Mostrar sobre otras apps'.")
            .setPositiveButton("Configurar") { _, _ ->
                if (needsUsageStats) {
                    // Mejor abrir la lista general para evitar crashes en algunas versiones
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } else if (needsOverlay) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    fun startBlockingService() {
        val intent = Intent(this, AppBlockerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}