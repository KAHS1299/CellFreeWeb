package com.example.cellfreeapp

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.base.BaseActivity
import com.example.cellfreeapp.data.model.BlockConfiguration
import com.example.cellfreeapp.data.model.BlockedAppRequest
import com.example.cellfreeapp.data.repository.BlockedAppsRepository
import com.example.cellfreeapp.utils.AppBlockerService
import com.example.cellfreeapp.ui.dialog.BlockConfigurationModal
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    var isBlocked: Boolean = false,
    var blockTimeMinutes: Int? = null,
    var blockCategory: String? = null,
    var dbId: Int? = null,
    var blockStartDate: String? = null,
    var blockEndDate: String? = null,
    var isUpdatingStatus: Boolean = false
)

class AppListAdapter(
    private val allApps: List<AppInfo>,
    private val onAppNeedsConfiguration: (AppInfo) -> Unit,
    private val onAppStatusChanged: (AppInfo, Boolean) -> Unit,
    private val onAppNameClicked: (AppInfo) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>()
{
    
    private var filteredApps = allApps.toMutableList()
    
    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.iv_app_icon)
        val appName: TextView = view.findViewById(R.id.tv_app_name)
        val blockTime: TextView = view.findViewById(R.id.tv_block_time)
        val checkBox: CheckBox = view.findViewById(R.id.cb_app_block)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_list, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.appName.text = app.name
        holder.appIcon.setImageDrawable(app.icon)
        
        // Configurar información de bloqueo
        if (app.isBlocked && app.blockTimeMinutes != null) {
            holder.blockTime.visibility = View.VISIBLE
            val timeText = when {
                app.blockTimeMinutes!! < 60 -> "${app.blockTimeMinutes} min"
                app.blockTimeMinutes!! == 60 -> "1 hora"
                app.blockTimeMinutes!! < 1440 -> "${app.blockTimeMinutes!! / 60}h ${app.blockTimeMinutes!! % 60}min"
                else -> "${app.blockTimeMinutes!! / 1440} días"
            }
            val categoryText = app.blockCategory?.let { " • $it" } ?: ""
            holder.blockTime.text =
                context.getString(R.string.blocked_by_time, timeText, categoryText)
        } else {
            holder.blockTime.visibility = View.GONE
        }
        
        // Remover listener temporalmente para evitar llamadas innecesarias
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = app.isBlocked
        holder.checkBox.isEnabled = !app.isUpdatingStatus
        
        // Log para debugging
        if (app.isBlocked) {
            Log.d("AppListAdapter", "App ${app.name} está marcada como bloqueada en el adapter")
        }
        
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (app.dbId == null && isChecked) {
                app.isBlocked = true
                onAppNeedsConfiguration(app)
            } else if (app.dbId == null) {
                app.isBlocked = false
                notifyAppChanged(app)
            } else {
                app.isBlocked = isChecked
                onAppStatusChanged(app, isChecked)
            }
        }
        
        holder.appName.setOnClickListener {
            onAppNameClicked(app)
        }
    }

    override fun getItemCount(): Int = filteredApps.size
    
    fun filterApps(query: String) {
        val newFilteredApps = if (query.isEmpty()) {
            allApps.toList()
        } else {
            allApps.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        val oldFilteredApps = filteredApps.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldFilteredApps.size

            override fun getNewListSize(): Int = newFilteredApps.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldFilteredApps[oldItemPosition].packageName ==
                    newFilteredApps[newItemPosition].packageName

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldFilteredApps[oldItemPosition] == newFilteredApps[newItemPosition]
        })
        filteredApps.clear()
        filteredApps.addAll(newFilteredApps)
        diff.dispatchUpdatesTo(this)
    }

    fun clearPendingSelection(app: AppInfo) {
        app.isBlocked = false
        app.blockTimeMinutes = null
        app.blockCategory = null
        notifyAppChanged(app)
    }

    fun notifyAppChanged(app: AppInfo) {
        val position = filteredApps.indexOfFirst { it.packageName == app.packageName }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    fun notifyVisibleAppsChanged() {
        if (filteredApps.isNotEmpty()) {
            notifyItemRangeChanged(0, filteredApps.size)
        }
    }
}

class AppBlockerActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appListAdapter: AppListAdapter
    private lateinit var blockedAppsRepository: BlockedAppsRepository
    private lateinit var searchEditText: TextInputEditText
    private val installedApps = mutableListOf<AppInfo>()
    private var currentSelectedApp: AppInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_blocker)

        blockedAppsRepository = BlockedAppsRepository(this)
        
        // Verificar que el usuario esté autenticado
        val token = authRepository.getAccessToken()
        Log.d("AppBlockerActivity", "Token al iniciar: ${if (token != null) "✓ Presente" else "✗ No encontrado"}")
        
        if (token == null) {
            Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        setupBaseViews()
        initViews()
        setupSearchFunction()
        loadInstalledApps()
        loadBlockedApps() // Cargar información de apps ya bloqueadas
        setupClickListeners()
    }
    
    override fun getScreenTitle(): String {
        return "Aplicaciones"
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)
        searchEditText = findViewById(R.id.etSearchApps)
    }
    
    private fun setupSearchFunction() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appListAdapter.filterApps(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadInstalledApps() {
        val packageManager = packageManager
        val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        installedApps.clear()
        
        for (appInfo in installedPackages) {
            // Filtrar solo las aplicaciones que no son del sistema o las populares que sí queremos mostrar
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || 
                isPopularApp(appInfo.packageName)) {
                
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val appIcon = packageManager.getApplicationIcon(appInfo.packageName)
                    if (appInfo.packageName != "com.example.cellfreeapp") {
                        installedApps.add(AppInfo(appName, appInfo.packageName, appIcon))
                    }
                } catch (e: Exception) {
                    // Ignorar aplicaciones que no se pueden cargar
                    Log.e("AppBlockerActivity", "Error al cargar información de la app: ${e.message}")
                }
            }
        }
        
        // Ordenar alfabéticamente
        installedApps.sortBy { it.name }
        
        // Configurar el adapter con los callbacks para mostrar modal y manejar cancelaciones
        appListAdapter = AppListAdapter(
            allApps = installedApps,
            onAppNeedsConfiguration = { selectedApp ->
                currentSelectedApp = selectedApp
                showBlockConfigurationModal(selectedApp)
            },
            onAppStatusChanged = { app, isBlocked ->
                updateAppStatus(app, isBlocked)
            },
            onAppNameClicked = { app ->
                currentSelectedApp = app
                showBlockConfigurationModal(app, isEditing = app.dbId != null)
            },
            context = this.baseContext
        )
        recyclerView.adapter = appListAdapter
    }
    
    private fun loadBlockedApps() {
        findViewById<ProgressBar>(R.id.progress_loading).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.rv_apps).visibility = View.GONE
        lifecycleScope.launch {
            try {
                val result = blockedAppsRepository.getBlockedApps()
                if (result.isSuccess) {
                    val blockedApps = result.getOrNull() ?: emptyList()

                    for (blockedApp in blockedApps) {
                        val installedApp = blockedApp.nombrePaquete
                            ?.let { packageName ->
                                installedApps.find { it.packageName == packageName }
                            }
                            ?: installedApps.find { it.name == blockedApp.nombreApp }
                        installedApp?.let {
                            it.isBlocked = blockedApp.estado
                            it.blockTimeMinutes = blockedApp.tiempoBloqueo
                            it.blockCategory = blockedApp.categoria
                            it.dbId = blockedApp.id
                            it.blockStartDate = blockedApp.fechaInicio
                            it.blockEndDate = blockedApp.fechaFin
                        }
                    }

                    findViewById<ProgressBar>(R.id.progress_loading).visibility = View.GONE
                    findViewById<RecyclerView>(R.id.rv_apps).visibility = View.VISIBLE

                    // Actualizar la vista
                    appListAdapter.notifyVisibleAppsChanged()
                    Log.d("AppBlockerActivity", "Cargadas ${blockedApps.size} apps bloqueadas del servidor")
                } else {
                    Log.e("AppBlockerActivity", "Error al cargar apps bloqueadas: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("AppBlockerActivity", "Excepción al cargar apps bloqueadas: ${e.message}")
            }
        }
    }
    
    private fun isPopularApp(packageName: String): Boolean {
        val popularApps = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.whatsapp",
            "com.twitter.android",
            "com.snapchat.android",
            "com.zhiliaoapp.musically", // TikTok
            "org.telegram.messenger",
            "com.spotify.music",
            "com.netflix.mediaclient"
        )
        return popularApps.contains(packageName)
    }
    
    private fun showBlockConfigurationModal(
        selectedApp: AppInfo,
        isEditing: Boolean = false
    ) {
        val initialConfiguration = if (isEditing) {
            BlockConfiguration(
                appName = selectedApp.name,
                isAllApps = false,
                startDate = selectedApp.blockStartDate,
                endDate = selectedApp.blockEndDate,
                timeInMinutes = selectedApp.blockTimeMinutes ?: 60,
                category = selectedApp.blockCategory
            )
        } else {
            null
        }
        val modal = BlockConfigurationModal(
            context = this,
            appName = selectedApp.name,
            isAllApps = false, // Es una app individual
            onSave = { configuration ->
                saveBlockConfiguration(configuration, selectedApp)
            },
            onCancel = {
                if (!isEditing && selectedApp.dbId == null) {
                    appListAdapter.clearPendingSelection(selectedApp)
                }
                if (currentSelectedApp === selectedApp) {
                    currentSelectedApp = null
                }
            },
            initialConfiguration = initialConfiguration
        )
        modal.show()
    }
    
    private fun showAllAppsConfigurationModal() {
        val modal = BlockConfigurationModal(
            context = this,
            appName = "Todas las aplicaciones",
            isAllApps = true,
            onSave = { configuration ->
                saveAllAppsConfiguration(configuration)
            },
            onCancel = {
                // Si se cancela la configuración de todas las apps,
                // no hay apps específicas que desseleccionar
            }
        )
        modal.show()
    }
    
    private fun saveBlockConfiguration(configuration: BlockConfiguration, selectedApp: AppInfo) {
        lifecycleScope.launch {
            try {
                Log.d("AppBlockerActivity", "Guardando configuración para: ${selectedApp.name}")
                val category = configuration.category ?: detectAppCategory(selectedApp.packageName)
                
                Log.d("AppBlockerActivity", "Fechas - Inicio: ${configuration.startDate}, Fin: ${configuration.endDate}")

                val request = BlockedAppRequest(
                    nombreApp = selectedApp.name,  // Usar el nombre real de la app
                    categoria = category,
                    tiempoBloqueo = configuration.timeInMinutes,
                    estado = selectedApp.dbId == null || selectedApp.isBlocked,
                    fechaInicio = configuration.startDate,
                    fechaFin = configuration.endDate,
                    nombrePaquete = selectedApp.packageName
                )
                
                val result = selectedApp.dbId?.let { appId ->
                    blockedAppsRepository.updateBlockedApp(appId, request)
                } ?: blockedAppsRepository.createBlockedApp(request)
                
                if (result.isSuccess) {
                    refreshAppBlockerService()
                    Toast.makeText(
                        this@AppBlockerActivity,
                        if (selectedApp.dbId == null) {
                            "${selectedApp.name} bloqueada exitosamente"
                        } else {
                            "Configuración de ${selectedApp.name} actualizada"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    val savedApp = result.getOrThrow()
                    selectedApp.dbId = savedApp.id ?: selectedApp.dbId
                    selectedApp.isBlocked = request.estado
                    selectedApp.blockTimeMinutes =
                        savedApp.tiempoBloqueo ?: configuration.timeInMinutes
                    selectedApp.blockCategory = savedApp.categoria ?: category
                    selectedApp.blockStartDate = savedApp.fechaInicio ?: configuration.startDate
                    selectedApp.blockEndDate = savedApp.fechaFin ?: configuration.endDate
                    appListAdapter.notifyAppChanged(selectedApp)
                    currentSelectedApp = null
                } else {
                    Toast.makeText(
                        this@AppBlockerActivity,
                        "Error: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Desmarcar el checkbox si falló y limpiar datos de bloqueo
                    if (selectedApp.dbId == null) {
                        selectedApp.isBlocked = false
                        selectedApp.blockTimeMinutes = null
                        selectedApp.blockCategory = null
                        selectedApp.blockStartDate = null
                        selectedApp.blockEndDate = null
                    }
                    appListAdapter.notifyAppChanged(selectedApp)
                    currentSelectedApp = null
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AppBlockerActivity,
                    "Error al guardar configuración: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                
                // Desmarcar el checkbox si falló y limpiar datos de bloqueo
                if (selectedApp.dbId == null) {
                    selectedApp.isBlocked = false
                    selectedApp.blockTimeMinutes = null
                    selectedApp.blockCategory = null
                    selectedApp.blockStartDate = null
                    selectedApp.blockEndDate = null
                }
                appListAdapter.notifyAppChanged(selectedApp)
                currentSelectedApp = null
            }
        }
    }

    private fun updateAppStatus(app: AppInfo, requestedStatus: Boolean) {
        val appId = app.dbId
        if (appId == null) {
            if (requestedStatus) {
                currentSelectedApp = app
                showBlockConfigurationModal(app)
            } else {
                app.isBlocked = false
                appListAdapter.notifyAppChanged(app)
            }
            return
        }

        app.isUpdatingStatus = true
        appListAdapter.notifyAppChanged(app)
        lifecycleScope.launch {
            try {
                val result = blockedAppsRepository.toggleAppStatus(appId)
                if (result.isSuccess) {
                    val updatedApp = result.getOrThrow()
                    app.isBlocked = updatedApp.estado
                    app.blockTimeMinutes = updatedApp.tiempoBloqueo ?: app.blockTimeMinutes
                    app.blockCategory = updatedApp.categoria ?: app.blockCategory
                    app.blockStartDate = updatedApp.fechaInicio ?: app.blockStartDate
                    app.blockEndDate = updatedApp.fechaFin ?: app.blockEndDate
                    refreshAppBlockerService()
                    Toast.makeText(
                        this@AppBlockerActivity,
                        if (app.isBlocked) "${app.name} activada" else "${app.name} desactivada",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    app.isBlocked = !requestedStatus
                    Toast.makeText(
                        this@AppBlockerActivity,
                        "No se pudo cambiar el estado de ${app.name}: " +
                            (result.exceptionOrNull()?.message ?: "Error desconocido"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                app.isBlocked = !requestedStatus
                Toast.makeText(
                    this@AppBlockerActivity,
                    "No se pudo cambiar el estado de ${app.name}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                app.isUpdatingStatus = false
                appListAdapter.notifyAppChanged(app)
            }
        }
    }
    
    private fun saveAllAppsConfiguration(configuration: BlockConfiguration) {
        lifecycleScope.launch {
            try {
                val selectedApps = installedApps.filter { it.isBlocked }
                var successCount = 0
                var failCount = 0
                
                for (app in selectedApps) {
                    val category = detectAppCategory(app.packageName)
                    
                    Log.d("AppBlockerActivity", "Bloqueo masivo - App: ${app.name}, Fechas - Inicio: ${configuration.startDate}, Fin: ${configuration.endDate}")
                    
                    val request = BlockedAppRequest(
                        nombreApp = app.name,
                        categoria = category,
                        tiempoBloqueo = configuration.timeInMinutes,
                        estado = true,
                        fechaInicio = configuration.startDate,
                        fechaFin = configuration.endDate,
                        nombrePaquete = app.packageName
                    )
                    
                    val result = app.dbId?.let { appId ->
                        blockedAppsRepository.updateBlockedApp(appId, request)
                    } ?: blockedAppsRepository.createBlockedApp(request)

                    if (result.isSuccess) {
                        successCount++
                        val savedApp = result.getOrThrow()
                        app.dbId = savedApp.id ?: app.dbId
                        app.isBlocked = request.estado
                        app.blockTimeMinutes =
                            savedApp.tiempoBloqueo ?: configuration.timeInMinutes
                        app.blockCategory = savedApp.categoria ?: category
                        app.blockStartDate = savedApp.fechaInicio ?: configuration.startDate
                        app.blockEndDate = savedApp.fechaFin ?: configuration.endDate
                    } else {
                        failCount++
                        if (app.dbId == null) {
                            app.isBlocked = false
                        }
                    }
                }
                
                val message = if (failCount == 0) {
                    "$successCount aplicaciones bloqueadas exitosamente"
                } else {
                    "$successCount aplicaciones bloqueadas, $failCount fallaron"
                }
                
                Toast.makeText(this@AppBlockerActivity, message, Toast.LENGTH_LONG).show()
                if (successCount > 0) {
                    refreshAppBlockerService()
                }
                appListAdapter.notifyVisibleAppsChanged()
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@AppBlockerActivity,
                    "Error al guardar configuración: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun detectAppCategory(packageName: String): String {
        return when {
            packageName.contains("facebook") || packageName.contains("instagram") || 
            packageName.contains("twitter") || packageName.contains("snapchat") ||
            packageName.contains("whatsapp") || packageName.contains("telegram") -> "social"
            
            packageName.contains("game") || packageName.contains("play") -> "juegos"
            
            packageName.contains("youtube") || packageName.contains("netflix") || 
            packageName.contains("spotify") || packageName.contains("music") -> "entretenimiento"
            
            packageName.contains("office") || packageName.contains("docs") || 
            packageName.contains("calendar") -> "productividad"
            
            else -> "otros"
        }
    }

    private fun setupClickListeners() {
        // Buscar el botón "Todas" si existe en el layout
        findViewById<View>(R.id.btn_select_all)?.setOnClickListener {
            showAllAppsConfigurationModal()
        }
    }

    private fun refreshAppBlockerService() {
        val intent = Intent(this, AppBlockerService::class.java).apply {
            action = AppBlockerService.ACTION_REFRESH_BLOCKED_APPS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}