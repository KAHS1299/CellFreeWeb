package com.example.cellfreeapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.example.cellfreeapp.base.BaseActivity
import com.example.cellfreeapp.service.MonitoringConfigManager
import com.example.cellfreeapp.service.UsageMonitoringWorker
import com.example.cellfreeapp.utils.UsageStatsHelper
import com.example.cellfreeapp.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MonitoringConfigActivity : BaseActivity() {

    private lateinit var configManager: MonitoringConfigManager
    private lateinit var usageStatsHelper: UsageStatsHelper
    
    private lateinit var switchMonitoring: Switch
    private lateinit var spinnerInterval: Spinner
    private lateinit var textLastExecution: TextView
    private lateinit var textTotalApps: TextView
    private lateinit var textMonitoringStatus: TextView
    private lateinit var buttonRequestPermission: Button
    private lateinit var buttonCheckPermissions: Button
    private lateinit var buttonResetStats: Button
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    companion object {
        const val TAG = "MonitoringConfigActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring_config)
        setupBaseViews()
        
        // Configurar ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "⚙️ Configuración de Monitoreo"
        }
        
        // Inicializar componentes
        configManager = MonitoringConfigManager(this)
        usageStatsHelper = UsageStatsHelper(this)
        
        initViews()
        setupListeners()
        updateUI()
        checkPermissions()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getScreenTitle(): String {
        return "⚙️ Monitoreo Automático"
    }

    private fun initViews() {
        switchMonitoring = findViewById(R.id.switch_monitoring)
        spinnerInterval = findViewById(R.id.spinner_interval)
        textLastExecution = findViewById(R.id.text_last_execution)
        textTotalApps = findViewById(R.id.text_total_apps)
        textMonitoringStatus = findViewById(R.id.text_monitoring_status)
        buttonRequestPermission = findViewById(R.id.button_request_permission)
        buttonCheckPermissions = findViewById(R.id.button_check_permissions)
        buttonResetStats = findViewById(R.id.button_reset_stats)
        
        setupIntervalSpinner()
    }

    private fun setupIntervalSpinner() {
        val intervals = arrayOf(
            "Cada 15 minutos",
            "Cada 30 minutos", 
            "Cada 1 hora",
            "Cada 2 horas"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = adapter
    }

    private fun setupListeners() {
        switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Switch cambió a: $isChecked")
            
            if (isChecked && !usageStatsHelper.hasUsageStatsPermission()) {
                Log.d(TAG, "Intentando activar sin permisos - mostrando diálogo")
                switchMonitoring.isChecked = false
                showPermissionDialog()
                return@setOnCheckedChangeListener
            }
            
            if (isChecked) {
                Log.d(TAG, "Activando monitoreo...")
                // Crear canal de notificación antes de activar
                NotificationHelper.createNotificationChannel(this@MonitoringConfigActivity)
                Toast.makeText(this, "🔄 Activando monitoreo automático...", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Desactivando monitoreo...")
                Toast.makeText(this, "⏹️ Desactivando monitoreo automático...", Toast.LENGTH_SHORT).show()
            }
            
            configManager.setMonitoringEnabled(isChecked)
            updateMonitoringStatus()
        }
        
        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val interval = when (position) {
                    0 -> MonitoringConfigManager.INTERVAL_15_MINUTES
                    1 -> MonitoringConfigManager.INTERVAL_30_MINUTES
                    2 -> MonitoringConfigManager.INTERVAL_1_HOUR
                    3 -> MonitoringConfigManager.INTERVAL_2_HOURS
                    else -> MonitoringConfigManager.INTERVAL_15_MINUTES
                }
                configManager.setMonitoringInterval(interval)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        buttonRequestPermission.setOnClickListener {
            requestUsageStatsPermission()
        }
        
        buttonCheckPermissions.setOnClickListener {
            recheckPermissions()
        }
        
        buttonResetStats.setOnClickListener {
            showResetStatsDialog()
        }
    }

    private fun updateUI() {
        val summary = configManager.getMonitoringSummary()
        
        // Estado del switch
        switchMonitoring.isChecked = summary.enabled
        
        // Intervalo seleccionado
        val intervalPosition = when (summary.intervalMinutes) {
            MonitoringConfigManager.INTERVAL_15_MINUTES -> 0
            MonitoringConfigManager.INTERVAL_30_MINUTES -> 1
            MonitoringConfigManager.INTERVAL_1_HOUR -> 2
            MonitoringConfigManager.INTERVAL_2_HOURS -> 3
            else -> 0
        }
        spinnerInterval.setSelection(intervalPosition)
        
        // Última ejecución
        if (summary.lastExecutionTime > 0) {
            textLastExecution.text = "Última ejecución: ${dateFormat.format(Date(summary.lastExecutionTime))}"
        } else {
            textLastExecution.text = "Última ejecución: Nunca"
        }
        
        // Total de apps monitoreadas
        textTotalApps.text = "Apps monitoreadas: ${summary.totalAppsMonitored}"
        
        updateMonitoringStatus()
    }

    private fun updateMonitoringStatus() {
        lifecycleScope.launch {
            UsageMonitoringWorker.isMonitoringActive(this@MonitoringConfigActivity) { isActive ->
                runOnUiThread {
                    if (isActive && configManager.isMonitoringEnabled()) {
                        textMonitoringStatus.text = "✅ Monitoreo activo"
                        textMonitoringStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    } else if (configManager.isMonitoringEnabled()) {
                        textMonitoringStatus.text = "⏳ Iniciando monitoreo..."
                        textMonitoringStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                    } else {
                        textMonitoringStatus.text = "❌ Monitoreo inactivo"
                        textMonitoringStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        Log.d(TAG, "Verificando permisos de estadísticas de uso...")
        
        if (usageStatsHelper.hasUsageStatsPermission()) {
            Log.d(TAG, "✅ Permisos concedidos")
            buttonRequestPermission.visibility = View.GONE
            buttonCheckPermissions.visibility = View.GONE
            switchMonitoring.isEnabled = true
            
            // Actualizar estado si los permisos están OK
            if (!configManager.isMonitoringEnabled()) {
                textMonitoringStatus.text = "✅ Listo para activar"
                textMonitoringStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }
        } else {
            Log.d(TAG, "❌ Permisos no concedidos")
            buttonRequestPermission.visibility = View.VISIBLE
            buttonCheckPermissions.visibility = View.VISIBLE
            switchMonitoring.isEnabled = false
            switchMonitoring.isChecked = false
            textMonitoringStatus.text = "⚠️ Se requieren permisos"
            textMonitoringStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("📱 Permisos de Estadísticas de Uso")
            .setMessage("Para el monitoreo automático, CellFree necesita acceso especial a las estadísticas de uso de aplicaciones.\n\n" +
                       "📋 Pasos a seguir:\n" +
                       "1. Toca 'Abrir Configuración'\n" +
                       "2. Busca 'CellFree' en la lista\n" +
                       "3. Activa el interruptor junto a CellFree\n" +
                       "4. Vuelve a esta pantalla\n" +
                       "5. Toca '🔄 Verificar Permisos'\n\n" +
                       "⚠️ Este permiso es necesario para detectar automáticamente el uso de aplicaciones.")
            .setPositiveButton("Abrir Configuración") { _, _ ->
                requestUsageStatsPermission()
            }
            .setNegativeButton("Ahora No", null)
            .setCancelable(true)
            .show()
    }

    private fun requestUsageStatsPermission() {
        try {
            val intent = usageStatsHelper.openUsageAccessSettings()
            startActivity(intent)
            Toast.makeText(this, 
                "🔍 Busca 'CellFree' en la lista y activa el permiso", 
                Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo configuración específica: ${e.message}")
            // Fallback al configuración general si no se puede abrir la específica
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, 
                    "Ve a: Aplicaciones > Acceso especial > Datos de uso de aplicaciones > CellFree", 
                    Toast.LENGTH_LONG).show()
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Error abriendo configuración general: ${fallbackError.message}")
                Toast.makeText(this, 
                    "No se pudo abrir configuración. Ve manualmente a: Configuración > Aplicaciones > Acceso especial", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showResetStatsDialog() {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Resetear Estadísticas")
            .setMessage("¿Estás seguro de que quieres resetear todas las estadísticas de monitoreo?")
            .setPositiveButton("Resetear") { _, _ ->
                configManager.resetMonitoringStats()
                updateUI()
                Toast.makeText(this, "Estadísticas reseteadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - verificando estado actualizado")
        checkPermissions()
        updateUI()
    }

    private fun recheckPermissions() {
        Log.d(TAG, "Reverificando permisos manualmente...")
        checkPermissions()
        
        if (usageStatsHelper.hasUsageStatsPermission()) {
            Toast.makeText(this, "✅ Permisos concedidos correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ Aún se requieren permisos", Toast.LENGTH_SHORT).show()
        }
    }
}