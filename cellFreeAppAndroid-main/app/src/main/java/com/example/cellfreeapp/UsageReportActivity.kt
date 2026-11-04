package com.example.cellfreeapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.adapters.ReportsTableAdapter
import com.example.cellfreeapp.base.BaseActivity
import com.example.cellfreeapp.data.model.ReportTableItem
import com.example.cellfreeapp.data.repository.ReportsRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UsageReportActivity : BaseActivity() {

    private lateinit var etFechaInicio: TextInputEditText
    private lateinit var etFechaFin: TextInputEditText
    private lateinit var ivDownload: ImageView
    private lateinit var rvReports: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var llSummary: CardView
    private lateinit var tvTotalApps: TextView
    private lateinit var tvTotalDaily: TextView
    private lateinit var tvTotalWeekly: TextView
    private lateinit var tvTotalMonthly: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var reportsRepository: ReportsRepository
    private lateinit var reportsAdapter: ReportsTableAdapter

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private var customStartDate: String? = null
    private var customEndDate: String? = null
    private var isFiltering = false

    companion object {
        private const val TAG = "UsageReportActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manejar compatibilidad con dispositivos Motorola
        try {
            super.onCreate(savedInstanceState)
        } catch (e: Exception) {
            // Si hay error con BaseActivity, usar AppCompatActivity directamente
            Log.w(TAG, "Error con BaseActivity, usando fallback: ${e.message}")
            if (this !is androidx.appcompat.app.AppCompatActivity) {
                // Intentar con configuración mínima
                setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            }
        }
        
        try {
            setContentView(R.layout.activity_usage_report)
            setupBaseViews()
            
            // Configurar ActionBar si está disponible
            supportActionBar?.let { actionBar ->
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.title = "📊 Reportes de Uso"
            }
            
            initRepository()
            initViews()
            setupClickListeners()
            loadInitialData()
            
        } catch (e: Exception) {
            Log.w(TAG, "Error con layout principal, usando fallback: ${e.message}")
            
            // Usar layout simplificado para dispositivos problemáticos
            try {
                setContentView(R.layout.activity_usage_report_fallback)
                setupBaseViews()
                supportActionBar?.title = "📊 Reportes"
                
                initRepository()
                initViews()
                setupClickListeners()
                loadInitialData()
                
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Error crítico al inicializar actividad: ${fallbackError.message}")
                Toast.makeText(this, "Error al cargar reportes: ${fallbackError.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
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
        return "📊 Reportes"
    }

    private fun initRepository() {
        try {
            reportsRepository = ReportsRepository(this)
            Log.d(TAG, "ReportsRepository inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar repository: ${e.message}")
            Toast.makeText(this, "Error de configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        try {
            // Inicializar vistas básicas una por una con verificación
            try {
                etFechaInicio = findViewById(R.id.et_fecha_inicio)
                Log.d(TAG, "etFechaInicio inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando etFechaInicio: ${e.message}")
            }
            
            try {
                etFechaFin = findViewById(R.id.et_fecha_fin)
                Log.d(TAG, "etFechaFin inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando etFechaFin: ${e.message}")
            }
            
            try {
                ivDownload = findViewById(R.id.iv_download)
                Log.d(TAG, "ivDownload inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando ivDownload: ${e.message}")
            }
            
            try {
                rvReports = findViewById(R.id.rv_reports)
                Log.d(TAG, "rvReports inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando rvReports: ${e.message}")
            }
            
            try {
                tvNoData = findViewById(R.id.tv_no_data)
                Log.d(TAG, "tvNoData inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando tvNoData: ${e.message}")
            }
            
            try {
                llSummary = findViewById(R.id.ll_summary)
                Log.d(TAG, "llSummary inicializado como CardView")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando llSummary: ${e.message}")
            }
            
            try {
                tvTotalApps = findViewById(R.id.tv_total_apps)
                Log.d(TAG, "tvTotalApps inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando tvTotalApps: ${e.message}")
            }
            
            try {
                tvTotalDaily = findViewById(R.id.tv_total_daily)
                Log.d(TAG, "tvTotalDaily inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando tvTotalDaily: ${e.message}")
            }
            
            try {
                tvTotalWeekly = findViewById(R.id.tv_total_weekly)
                Log.d(TAG, "tvTotalWeekly inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando tvTotalWeekly: ${e.message}")
            }
            
            try {
                tvTotalMonthly = findViewById(R.id.tv_total_monthly)
                Log.d(TAG, "tvTotalMonthly inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando tvTotalMonthly: ${e.message}")
            }
            
            try {
                progressBar = findViewById(R.id.progress_bar)
                Log.d(TAG, "progressBar inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando progressBar: ${e.message}")
            }

            // Configurar RecyclerView con compatibilidad para Motorola
            if (::rvReports.isInitialized) {
                setupRecyclerView()
            } else {
                Log.e(TAG, "No se pudo inicializar RecyclerView")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico al inicializar vistas: ${e.message}")
            Toast.makeText(this, "Error al cargar interfaz: ${e.message}", Toast.LENGTH_LONG).show()
            // No terminar la actividad inmediatamente, dar oportunidad de usar fallback
        }
    }
    
    private fun setupRecyclerView() {
        try {
            // Intentar usar el adaptador principal
            reportsAdapter = ReportsTableAdapter()
            rvReports.apply {
                layoutManager = LinearLayoutManager(this@UsageReportActivity)
                adapter = reportsAdapter
                itemAnimator = null // Desactivar para compatibilidad Motorola
                
                // Asegurar altura mínima después de configurar el adaptador
                post {
                    ensureMinimumHeight()
                }
            }
            Log.d(TAG, "RecyclerView configurado con adaptador principal")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error con adaptador principal, usando fallback: ${e.message}")
            
            try {
                // Usar adaptador fallback para dispositivos problemáticos
                val fallbackAdapter = ReportsTableAdapterFallback(emptyList())
                rvReports.apply {
                    layoutManager = LinearLayoutManager(this@UsageReportActivity).apply {
                        isItemPrefetchEnabled = false
                    }
                    adapter = fallbackAdapter
                    itemAnimator = null
                    
                    // Asegurar altura mínima después de configurar el adaptador
                    post {
                        ensureMinimumHeight()
                    }
                }
                Log.d(TAG, "RecyclerView configurado con adaptador fallback")
                
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Error crítico con RecyclerView: ${fallbackError.message}")
                throw fallbackError
            }
        }
    }
    
    private fun ensureMinimumHeight() {
        try {
            if (::rvReports.isInitialized) {
                // Convertir dp a px
                val minHeightDp = 320
                val scale = resources.displayMetrics.density
                val minHeightPx = (minHeightDp * scale + 0.5f).toInt()
                
                // Asegurar que el RecyclerView tenga la altura mínima
                val layoutParams = rvReports.layoutParams
                if (layoutParams != null) {
                    layoutParams.height = Math.max(layoutParams.height, minHeightPx)
                    rvReports.layoutParams = layoutParams
                    Log.d(TAG, "Altura mínima del RecyclerView asegurada: ${minHeightPx}px")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error asegurando altura mínima: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        // Filtro por fecha
        setupDatePickers()

        // Botón de descarga
        ivDownload.setOnClickListener {
            downloadReport()
        }
    }

    private fun loadInitialData() {
        Log.d(TAG, "Cargando datos iniciales de reportes")
        
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                val reportData: List<ReportTableItem> = if (isFiltering && customStartDate != null && customEndDate != null) {
                    // Para reportes personalizados, usar endpoint específico
                    val result = reportsRepository.getCustomReport(customStartDate!!, customEndDate!!)
                    if (result.isSuccess) {
                        val aggregatedData = result.getOrNull()
                        aggregatedData?.aplicaciones?.map { appData ->
                            ReportTableItem(
                                nombreApp = appData.nombreApp,
                                categoria = appData.categoria ?: "general",
                                diario = formatMinutesToText(appData.tiempoTotalMinutos / 7), // Promedio diario
                                semanal = formatMinutesToText(appData.tiempoTotalMinutos),
                                mensual = formatMinutesToText(appData.tiempoTotalMinutos * 4), // Estimación mensual
                                personalizado = formatMinutesToText(appData.tiempoTotalMinutos)
                            )
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                } else {
                    // Para vista general, usar reportes combinados
                    val result = reportsRepository.getCombinedReports()
                    if (result.isSuccess) {
                        result.getOrNull() ?: emptyList()
                    } else {
                        emptyList()
                    }
                }

                if (reportData.isNotEmpty()) {
                    reportsAdapter.updateData(reportData)
                    updateSummary(reportData)
                    showNoData(false)
                    
                    // Asegurar altura mínima después de cargar datos
                    rvReports.post {
                        ensureMinimumHeight()
                    }
                } else {
                    showNoData(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al cargar reportes: ${e.message}")
                Toast.makeText(this@UsageReportActivity, 
                    "Error al cargar reportes: ${e.message}", Toast.LENGTH_LONG).show()
                showNoData(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setupDatePickers() {
        // Configurar campo de fecha inicio
        etFechaInicio.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val startDate = Calendar.getInstance()
                    startDate.set(year, month, dayOfMonth)
                    
                    customStartDate = apiDateFormat.format(startDate.time)
                    etFechaInicio.setText(dateFormat.format(startDate.time))
                    
                    // Filtrar datos cuando se cambie la fecha
                    if (customEndDate?.isNotEmpty() == true) {
                        isFiltering = true
                        loadInitialData()
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        // Configurar campo de fecha fin
        etFechaFin.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val endDate = Calendar.getInstance()
                    endDate.set(year, month, dayOfMonth)
                    
                    // Validar que tenga fecha de inicio
                    if (customStartDate?.isEmpty() != false) {
                        Toast.makeText(this, "Primero selecciona la fecha de inicio", 
                            Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    
                    // Validar que fecha fin sea posterior a fecha inicio
                    val startDate = apiDateFormat.parse(customStartDate!!)
                    if (startDate != null && endDate.time.before(startDate)) {
                        Toast.makeText(this, "La fecha de fin debe ser posterior a la fecha de inicio", 
                            Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    
                    customEndDate = apiDateFormat.format(endDate.time)
                    etFechaFin.setText(dateFormat.format(endDate.time))
                    
                    // Filtrar datos
                    isFiltering = true
                    loadInitialData()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        
        // Picker para fecha de inicio
        val startDatePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val startDate = Calendar.getInstance()
                startDate.set(year, month, dayOfMonth)
                
                // Picker para fecha de fin
                val endDatePicker = DatePickerDialog(
                    this,
                    { _, endYear, endMonth, endDayOfMonth ->
                        val endDate = Calendar.getInstance()
                        endDate.set(endYear, endMonth, endDayOfMonth)
                        
                        // Validar que fecha fin sea posterior a fecha inicio
                        if (endDate.before(startDate)) {
                            Toast.makeText(this, "La fecha de fin debe ser posterior a la fecha de inicio", 
                                Toast.LENGTH_SHORT).show()
                            return@DatePickerDialog
                        }
                        
                        // Establecer fechas y actualizar UI
                        customStartDate = apiDateFormat.format(startDate.time)
                        customEndDate = apiDateFormat.format(endDate.time)
                        
                        val displayText = "${dateFormat.format(startDate.time)} - ${dateFormat.format(endDate.time)}"
                        etFechaInicio.setText(displayText)
                        
                        isFiltering = true
                        loadInitialData()
                        
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                
                // Configurar fecha mínima para el end picker
                endDatePicker.datePicker.minDate = startDate.timeInMillis
                endDatePicker.show()
                
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // No permitir fechas futuras
        startDatePicker.datePicker.maxDate = System.currentTimeMillis()
        startDatePicker.show()
    }

    private fun downloadReport() {
        if (!isFiltering || customStartDate == null || customEndDate == null) {
            Toast.makeText(this, "Selecciona un rango de fechas para descargar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                showLoading(true)
                
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "reporte_uso_${timestamp}.csv"
                val destinationFile = File(downloadsDir, fileName)
                
                val result = reportsRepository.downloadReport(
                    tipo = "personalizado",
                    fechaInicio = customStartDate,
                    fechaFin = customEndDate,
                    destinationFile = destinationFile
                )
                
                if (result.isSuccess) {
                    Toast.makeText(this@UsageReportActivity, 
                        "Reporte descargado en: ${destinationFile.absolutePath}", 
                        Toast.LENGTH_LONG).show()
                        
                    // Optionally, open file manager to show the file
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                        androidx.core.content.FileProvider.getUriForFile(
                            this@UsageReportActivity,
                            "${packageName}.provider",
                            destinationFile
                        ),
                        "text/csv"
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(intent, "Abrir reporte"))
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error al descargar"
                    Toast.makeText(this@UsageReportActivity, 
                        "Error al descargar reporte: $error", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al descargar reporte: ${e.message}")
                Toast.makeText(this@UsageReportActivity, 
                    "Error al descargar reporte: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateSummary(reportData: List<ReportTableItem>) {
        val totalApps = reportData.size
        var totalDaily = 0
        var totalWeekly = 0
        var totalMonthly = 0
        
        // Calcular totales de cada período
        reportData.forEach { item ->
            totalDaily += parseTimeToMinutes(item.diario)
            totalWeekly += parseTimeToMinutes(item.semanal)
            totalMonthly += parseTimeToMinutes(item.mensual)
        }
        
        // Actualizar textos
        tvTotalApps.text = totalApps.toString()
        tvTotalDaily.text = formatMinutesToText(totalDaily)
        tvTotalWeekly.text = formatMinutesToText(totalWeekly)
        tvTotalMonthly.text = formatMinutesToText(totalMonthly)
        
        llSummary.visibility = if (totalApps > 0) View.VISIBLE else View.GONE
    }
    
    private fun formatMinutesToText(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val regex = """(\d+)h\s*(\d+)m|(\d+)m""".toRegex()
            val match = regex.find(timeStr)
            when {
                match?.groups?.get(1) != null -> {
                    val hours = match.groups[1]?.value?.toInt() ?: 0
                    val minutes = match.groups[2]?.value?.toInt() ?: 0
                    hours * 60 + minutes
                }
                match?.groups?.get(3) != null -> {
                    match.groups[3]?.value?.toInt() ?: 0
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun showLoading(isLoading: Boolean) {
        try {
            if (::progressBar.isInitialized) {
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
            if (::ivDownload.isInitialized) {
                ivDownload.isEnabled = !isLoading
            }
            if (::etFechaInicio.isInitialized) {
                etFechaInicio.isEnabled = !isLoading
            }
            if (::etFechaFin.isInitialized) {
                etFechaFin.isEnabled = !isLoading
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en showLoading: ${e.message}")
        }
    }

    private fun showNoData(show: Boolean) {
        try {
            if (::tvNoData.isInitialized) {
                tvNoData.visibility = if (show) View.VISIBLE else View.GONE
            }
            if (::rvReports.isInitialized) {
                rvReports.visibility = if (show) View.GONE else View.VISIBLE
            }
            if (::llSummary.isInitialized) {
                llSummary.visibility = if (show) View.GONE else View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en showNoData: ${e.message}")
        }
    }
}