package com.example.cellfreeapp

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cellfreeapp.adapters.CalendarDayAdapter
import com.example.cellfreeapp.adapters.CalendarDayCell
import com.example.cellfreeapp.adapters.CalendarEventAdapter
import com.example.cellfreeapp.base.BaseActivity
import com.example.cellfreeapp.data.api.ApiClient
import com.example.cellfreeapp.data.repository.CalendarRepository
import com.example.cellfreeapp.data.repository.CalendarSyncRepository
import com.example.cellfreeapp.data.model.CalendarSync
import com.example.cellfreeapp.service.ReminderNotificationScheduler
import com.example.cellfreeapp.models.CalendarEvent
import com.example.cellfreeapp.ui.dialogs.EventDialogFragment
import com.example.cellfreeapp.utils.CalendarIcsExporter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : BaseActivity(), EventDialogFragment.EventDialogListener {

    private lateinit var tvCalendarMonth: TextView
    private lateinit var btnPreviousMonth: MaterialButton
    private lateinit var btnNextMonth: MaterialButton
    private lateinit var rvCalendarDays: RecyclerView
    private lateinit var tvEventsTitle: TextView
    private lateinit var tvNoEvents: TextView
    private lateinit var rvEventsDay: RecyclerView
    private lateinit var fabAddEvent: FloatingActionButton
    private lateinit var btnImportCalendar: MaterialButton
    private lateinit var btnToday: MaterialButton
    private lateinit var btnExportCalendar: MaterialButton
    private lateinit var progressLoading: ProgressBar
    
    private lateinit var eventAdapter: CalendarEventAdapter
    private lateinit var calendarDayAdapter: CalendarDayAdapter
    private val eventsList = mutableListOf<CalendarEvent>()
    private var selectedDate: Long = System.currentTimeMillis()
    private var displayedMonth: Calendar = Calendar.getInstance()
    
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var calendarSyncRepository: CalendarSyncRepository
    private var eventsToExport: List<CalendarEvent> = emptyList()

    private val createCalendarDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
            if (uri != null) {
                val events = eventsToExport
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val output = contentResolver.openOutputStream(uri)
                                ?: throw IOException("No se pudo abrir el archivo de destino")
                            output.bufferedWriter(Charsets.UTF_8).use {
                                it.write(CalendarIcsExporter.export(events))
                            }
                        }
                        Toast.makeText(
                            this@CalendarActivity,
                            "Calendario exportado correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (error: IOException) {
                        showError("No se pudo guardar el calendario: ${error.message}")
                    } catch (error: SecurityException) {
                        showError("No se pudo acceder al archivo seleccionado: ${error.message}")
                    } catch (error: IllegalArgumentException) {
                        showError("No se pudo exportar un evento: ${error.message}")
                    }
                }
            }
            eventsToExport = emptyList()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = intent.getLongExtra(EXTRA_SELECTED_DATE, selectedDate)
        setContentView(R.layout.activity_calendar)

        setupBaseViews()
        initRepository()
        initViews()
        setupRecyclerView()
        setupClickListeners()
        setupCalendar()
        
        // Cargar eventos del día actual
        updateEventsForDate(selectedDate)
        loadEventsFromApi()
    }
    
    override fun getScreenTitle(): String {
        return "Calendario"
    }

    private fun initRepository() {
        val sharedPreferences = getSharedPreferences("CellFreeAuth", MODE_PRIVATE)
        calendarRepository = CalendarRepository(ApiClient.remindersApiService, sharedPreferences)
        calendarSyncRepository = CalendarSyncRepository(this)
    }

    private fun initViews() {
        tvCalendarMonth = findViewById(R.id.tv_calendar_month)
        btnPreviousMonth = findViewById(R.id.btn_previous_month)
        btnNextMonth = findViewById(R.id.btn_next_month)
        rvCalendarDays = findViewById(R.id.rv_calendar_days)
        tvEventsTitle = findViewById(R.id.tv_events_title)
        tvNoEvents = findViewById(R.id.tv_no_events)
        rvEventsDay = findViewById(R.id.rv_events_day)
        fabAddEvent = findViewById(R.id.fab_add_event)
        btnImportCalendar = findViewById(R.id.btn_import_calendar)
        btnToday = findViewById(R.id.btn_today)
        btnExportCalendar = findViewById(R.id.btn_export_calendar)
        progressLoading = findViewById(R.id.progress_loading)
    }

    private fun setupRecyclerView() {
        eventAdapter = CalendarEventAdapter(eventsList) { event ->
            showEventOptions(event)
        }
        rvEventsDay.layoutManager = LinearLayoutManager(this)
        rvEventsDay.adapter = eventAdapter
    }

    private fun setupCalendar() {
        displayedMonth = Calendar.getInstance().apply {
            timeInMillis = selectedDate
        }
        calendarDayAdapter = CalendarDayAdapter { dateInMillis ->
            selectedDate = dateInMillis
            displayedMonth.timeInMillis = dateInMillis
            updateEventsForDate(selectedDate)
        }
        rvCalendarDays.layoutManager = GridLayoutManager(this, 7)
        rvCalendarDays.adapter = calendarDayAdapter
        btnPreviousMonth.setOnClickListener { changeDisplayedMonth(-1) }
        btnNextMonth.setOnClickListener { changeDisplayedMonth(1) }
        updateCalendarGrid()
    }

    private fun changeDisplayedMonth(monthOffset: Int) {
        displayedMonth.add(Calendar.MONTH, monthOffset)
        selectedDate = displayedMonth.timeInMillis
        updateEventsForDate(selectedDate)
    }

    private fun updateCalendarGrid() {
        val monthStart = (displayedMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        tvCalendarMonth.text = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            .format(monthStart.time)
            .replaceFirstChar { it.titlecase(Locale("es", "ES")) }

        val firstWeekdayOffset =
            (monthStart.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val firstVisibleDay = (monthStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -firstWeekdayOffset)
        }
        val eventDays = eventsList.map { calendarDayKey(it.date.time) }.toSet()
        val selectedDay = calendarDayKey(selectedDate)
        val today = calendarDayKey(System.currentTimeMillis())
        val days = (0 until 42).map {
            val day = (firstVisibleDay.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, it)
            }
            val dayInMillis = day.timeInMillis
            CalendarDayCell(
                dateInMillis = dayInMillis,
                dayOfMonth = day.get(Calendar.DAY_OF_MONTH),
                belongsToDisplayedMonth =
                    day.get(Calendar.MONTH) == monthStart.get(Calendar.MONTH) &&
                        day.get(Calendar.YEAR) == monthStart.get(Calendar.YEAR),
                hasEvents = calendarDayKey(dayInMillis) in eventDays,
                isSelected = calendarDayKey(dayInMillis) == selectedDay,
                isToday = calendarDayKey(dayInMillis) == today
            )
        }
        calendarDayAdapter.submitDays(days)
    }

    private fun calendarDayKey(dateInMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = dateInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun setupClickListeners() {
        // Agregar nuevo evento
        fabAddEvent.setOnClickListener {
            showAddEventDialog(selectedDate)
        }

        // Botón "Hoy"
        btnToday.setOnClickListener {
            val today = System.currentTimeMillis()
            selectedDate = today
            displayedMonth.timeInMillis = today
            updateEventsForDate(today)
        }

        // Importar calendario
        btnImportCalendar.setOnClickListener {
            showImportOptions()
        }

        // Exportar calendario
        btnExportCalendar.setOnClickListener {
            exportCalendar()
        }
    }

    private fun updateEventsForDate(dateInMillis: Long) {
        updateCalendarGrid()
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val date = Date(dateInMillis)
        tvEventsTitle.text = "Eventos del ${dateFormat.format(date)}"
        
        // Filtrar eventos por fecha seleccionada
        val eventsForDate = getEventsForDate(dateInMillis)
        
        if (eventsForDate.isEmpty()) {
            tvNoEvents.visibility = View.VISIBLE
            rvEventsDay.visibility = View.GONE
        } else {
            tvNoEvents.visibility = View.GONE
            rvEventsDay.visibility = View.VISIBLE
        }
        eventAdapter.updateEvents(eventsForDate)
    }

    private fun getEventsForDate(dateInMillis: Long): List<CalendarEvent> {
        val selectedCal = Calendar.getInstance()
        selectedCal.timeInMillis = dateInMillis
        val selectedYear = selectedCal.get(Calendar.YEAR)
        val selectedDay = selectedCal.get(Calendar.DAY_OF_YEAR)
        
        return eventsList.filter { event ->
            val eventCal = Calendar.getInstance()
            eventCal.time = event.date
            
            eventCal.get(Calendar.DAY_OF_YEAR) == selectedDay &&
                eventCal.get(Calendar.YEAR) == selectedYear
        }
    }

    private fun loadEventsFromApi() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = calendarRepository.getReminders()
                result.fold(
                    onSuccess = { reminders ->
                        val currentIds = reminders.mapNotNull { it.id }.toSet()
                        eventsList.mapNotNull { it.id }
                            .filterNot(currentIds::contains)
                            .forEach { ReminderNotificationScheduler.cancel(this@CalendarActivity, it) }
                        eventsList.clear()
                        eventsList.addAll(reminders)
                        reminders.forEach {
                            ReminderNotificationScheduler.schedule(this@CalendarActivity, it)
                        }
                        updateEventsForDate(selectedDate)
                        showLoading(false)
                    },
                    onFailure = { error ->
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Error al cargar eventos: ${error.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@CalendarActivity, 
                    "Error de conexión: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showAddEventDialog(dateInMillis: Long) {
        val date = Date(dateInMillis)
        val dialog = EventDialogFragment.newInstance(date)
        dialog.show(supportFragmentManager, "EventDialog")
    }

    private fun showEditEventDialog(event: CalendarEvent) {
        val dialog = EventDialogFragment.newInstance(event)
        dialog.show(supportFragmentManager, "EventDialog")
    }

    private fun showEventOptions(event: CalendarEvent) {
        val completionOption = if (event.completed) {
            "Marcar como pendiente"
        } else {
            "Marcar como completado"
        }
        val options = arrayOf("Ver detalles", "Editar", completionOption, "Eliminar")
        
        AlertDialog.Builder(this)
            .setTitle(event.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEventDetails(event)
                    1 -> showEditEventDialog(event)
                    2 -> toggleEventComplete(event)
                    3 -> confirmDeleteEvent(event)
                }
            }
            .show()
    }

    private fun showEventDetails(event: CalendarEvent) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val completedText = if (event.completed) "Completado" else "Pendiente"
        val typeText = when (event.type) {
            "tarea" -> "Tarea"
            "examen" -> "Examen"
            "estudio" -> "Estudio"
            else -> "Otro"
        }
        
        AlertDialog.Builder(this)
            .setTitle(event.title)
            .setMessage("${event.description}\n\nFecha: ${dateFormat.format(event.date)}\nHora: ${event.startTime}\nTipo: $typeText\nEstado: $completedText")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun toggleEventComplete(event: CalendarEvent) {
        val eventId = event.id
        if (eventId == null) {
            Toast.makeText(this, "El recordatorio no tiene un ID válido", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = calendarRepository.toggleReminderComplete(eventId)
                result.fold(
                    onSuccess = { updatedEvent ->
                        // Actualizar el evento en la lista
                        val index = eventsList.indexOfFirst { it.id == eventId }
                        if (index != -1) {
                            eventsList[index] = updatedEvent
                            updateEventsForDate(selectedDate)
                        }
                        if (updatedEvent.completed) {
                            ReminderNotificationScheduler.cancel(this@CalendarActivity, eventId)
                        } else {
                            ReminderNotificationScheduler.schedule(this@CalendarActivity, updatedEvent)
                        }
                        showLoading(false)
                        val statusText = if (updatedEvent.completed) "completado" else "pendiente"
                        Toast.makeText(this@CalendarActivity, 
                            "Evento marcado como $statusText", 
                            Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Error al actualizar estado: ${error.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@CalendarActivity, 
                    "Error: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteEvent(event: CalendarEvent) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar evento")
            .setMessage("¿Estás seguro de que quieres eliminar '${event.title}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteEvent(event: CalendarEvent) {
        val eventId = event.id ?: return
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = calendarRepository.deleteReminder(eventId)
                result.fold(
                    onSuccess = {
                        // Remover el evento de la lista
                        eventsList.removeAll { it.id == eventId }
                        ReminderNotificationScheduler.cancel(this@CalendarActivity, eventId)
                        updateEventsForDate(selectedDate)
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Evento eliminado", 
                            Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Error al eliminar evento: ${error.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@CalendarActivity, 
                    "Error: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Implementación de EventDialogListener
    override fun onEventSaved(event: CalendarEvent) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = calendarRepository.createReminder(event)
                result.fold(
                    onSuccess = { createdEvent ->
                        eventsList.add(createdEvent)
                        ReminderNotificationScheduler.schedule(this@CalendarActivity, createdEvent)
                        updateEventsForDate(selectedDate)
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Evento creado exitosamente", 
                            Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Error al crear evento: ${error.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@CalendarActivity, 
                    "Error: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEventUpdated(event: CalendarEvent) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = calendarRepository.updateReminder(event)
                result.fold(
                    onSuccess = { updatedEvent ->
                        // Actualizar el evento en la lista
                        val index = eventsList.indexOfFirst { it.id == event.id }
                        if (index != -1) {
                            eventsList[index] = updatedEvent
                            updateEventsForDate(selectedDate)
                        }
                        ReminderNotificationScheduler.schedule(this@CalendarActivity, updatedEvent)
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Evento actualizado exitosamente", 
                            Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        showLoading(false)
                        Toast.makeText(this@CalendarActivity, 
                            "Error al actualizar evento: ${error.message}", 
                            Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@CalendarActivity, 
                    "Error: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEventDeleted(eventId: Int) {
        // Este método se implementa a través del confirmDeleteEvent
        eventsList.removeAll { it.id == eventId }
        updateEventsForDate(selectedDate)
    }

    // Métodos de importación/exportación
    private fun showImportOptions() {
        val options = arrayOf(
            "Importar desde archivo .ics",
            "Agregar calendario desde URL",
            "Administrar calendarios por URL"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Importar calendario")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> importFromFile()
                    1 -> importFromUrl()
                    2 -> syncWithGoogleCalendar()
                }
            }
            .show()
    }

    private fun importFromFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/calendar"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE)
    }

    private fun importFromUrl() {
        showCalendarSyncForm()
    }

    private fun syncWithGoogleCalendar() {
        Toast.makeText(
            this,
            "Para Google Calendar, publica o copia su URL iCalendar y agrégala como calendario por URL.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showCalendarSyncForm(calendarSync: CalendarSync? = null) {
        val nameInput = EditText(this).apply {
            hint = "Nombre del calendario"
            setSingleLine(true)
            setText(calendarSync?.nombre.orEmpty())
        }
        val urlInput = EditText(this).apply {
            hint = "https://ejemplo.com/calendario.ics"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setText(calendarSync?.urlCalendario.orEmpty())
        }
        val intervalInput = EditText(this).apply {
            hint = "Intervalo (15 a 1440 minutos)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText((calendarSync?.intervaloSincronizacion ?: 60).toString())
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(nameInput)
            addView(urlInput)
            addView(intervalInput)
        }

        AlertDialog.Builder(this)
            .setTitle(if (calendarSync == null) "Agregar calendario por URL" else "Editar calendario")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(if (calendarSync == null) "Agregar y sincronizar" else "Guardar") { _, _ ->
                val name = nameInput.text?.toString()?.trim().orEmpty()
                val url = urlInput.text?.toString()?.trim().orEmpty()
                val interval = intervalInput.text?.toString()?.toIntOrNull()
                val parsedUri = runCatching { Uri.parse(url) }.getOrNull()

                when {
                    name.isBlank() -> showError("Escribe un nombre para el calendario")
                    name.length > 100 -> showError("El nombre no puede superar 100 caracteres")
                    parsedUri?.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http") ||
                        parsedUri?.host.isNullOrBlank() ->
                        showError("Ingresa una URL válida que comience por https:// o http://")
                    interval == null || interval !in 15..1440 ->
                        showError("El intervalo debe estar entre 15 y 1440 minutos")
                    else -> saveCalendarSync(calendarSync, name, url, interval)
                }
            }
            .show()
    }

    private fun saveCalendarSync(
        existingSync: CalendarSync?,
        name: String,
        url: String,
        intervalMinutes: Int
    ) {
        showLoading(true)
        lifecycleScope.launch {
            val result = if (existingSync == null) {
                calendarSyncRepository.createCalendarSync(name, url, intervalMinutes)
            } else {
                calendarSyncRepository.updateCalendarSync(
                    existingSync.id,
                    name,
                    url,
                    intervalMinutes
                )
            }

            result.fold(
                onSuccess = { savedSync ->
                    if (existingSync == null) {
                        runCalendarSync(savedSync)
                    } else {
                        showLoading(false)
                        runCalendarSync(savedSync)
                    }
                },
                onFailure = { error ->
                    showLoading(false)
                    showError("No se pudo guardar el calendario: ${error.message}")
                }
            )
        }
    }

    private fun showCalendarSyncs() {
        showLoading(true)
        lifecycleScope.launch {
            val result = calendarSyncRepository.getCalendarSyncs()
            showLoading(false)
            result.fold(
                onSuccess = { syncs ->
                    if (syncs.isEmpty()) {
                        AlertDialog.Builder(this@CalendarActivity)
                            .setTitle("Calendarios por URL")
                            .setMessage("Aún no has agregado calendarios iCalendar.")
                            .setPositiveButton("Cerrar", null)
                            .setNeutralButton("Agregar") { _, _ -> showCalendarSyncForm() }
                            .show()
                    } else {
                        val labels = syncs.map { sync ->
                            val status = when (sync.estado) {
                                "activo" -> "Activo"
                                "error" -> "Error"
                                else -> "Inactivo"
                            }
                            "${sync.nombre} · $status"
                        }.toTypedArray()
                        AlertDialog.Builder(this@CalendarActivity)
                            .setTitle("Calendarios por URL")
                            .setItems(labels) { _, index ->
                                showCalendarSyncOptions(syncs[index])
                            }
                            .setPositiveButton("Agregar") { _, _ -> showCalendarSyncForm() }
                            .setNegativeButton("Cerrar", null)
                            .show()
                    }
                },
                onFailure = { error ->
                    showError("No se pudieron cargar los calendarios: ${error.message}")
                }
            )
        }
    }

    private fun showCalendarSyncOptions(sync: CalendarSync) {
        val toggleLabel = if (sync.estado == "activo") "Desactivar" else "Activar"
        val options = arrayOf("Sincronizar ahora", toggleLabel, "Editar", "Eliminar")
        AlertDialog.Builder(this)
            .setTitle(sync.nombre)
            .setItems(options) { _, index ->
                when (index) {
                    0 -> runCalendarSync(sync)
                    1 -> toggleCalendarSync(sync)
                    2 -> showCalendarSyncForm(sync)
                    3 -> confirmDeleteCalendarSync(sync)
                }
            }
            .show()
    }

    private fun runCalendarSync(sync: CalendarSync) {
        showLoading(true)
        lifecycleScope.launch {
            val result = calendarSyncRepository.syncCalendar(sync.id)
            result.fold(
                onSuccess = { importedCount ->
                    loadEventsFromApi()
                    Toast.makeText(
                        this@CalendarActivity,
                        "Sincronización completada: $importedCount eventos nuevos.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { error ->
                    showLoading(false)
                    showError("No se pudo sincronizar el calendario: ${error.message}")
                }
            )
        }
    }

    private fun toggleCalendarSync(sync: CalendarSync) {
        showLoading(true)
        lifecycleScope.launch {
            val result = calendarSyncRepository.toggleCalendarSync(sync.id)
            showLoading(false)
            result.fold(
                onSuccess = { updatedSync ->
                    Toast.makeText(
                        this@CalendarActivity,
                        if (updatedSync.estado == "activo") "Calendario activado" else "Calendario desactivado",
                        Toast.LENGTH_SHORT
                    ).show()
                    showCalendarSyncOptions(updatedSync)
                },
                onFailure = { error ->
                    showError("No se pudo cambiar el estado: ${error.message}")
                }
            )
        }
    }

    private fun confirmDeleteCalendarSync(sync: CalendarSync) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar calendario")
            .setMessage("¿Quieres eliminar la fuente “${sync.nombre}”? Los recordatorios importados se conservan.")
            .setPositiveButton("Eliminar fuente") { _, _ ->
                deleteCalendarSync(sync, deleteImportedReminders = false)
            }
            .setNeutralButton("Eliminar todo") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar recordatorios importados")
                    .setMessage("También se eliminarán todos los recordatorios sincronizados desde esta fuente.")
                    .setPositiveButton("Confirmar") { _, _ ->
                        deleteCalendarSync(sync, deleteImportedReminders = true)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCalendarSync(sync: CalendarSync, deleteImportedReminders: Boolean) {
        showLoading(true)
        lifecycleScope.launch {
            val result = calendarSyncRepository.deleteCalendarSync(
                sync.id,
                deleteImportedReminders
            )
            showLoading(false)
            result.fold(
                onSuccess = {
                    loadEventsFromApi()
                    showCalendarSyncs()
                },
                onFailure = { error ->
                    showError("No se pudo eliminar el calendario: ${error.message}")
                }
            )
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun exportCalendar() {
        if (eventsList.isEmpty()) {
            Toast.makeText(this, "No hay eventos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        eventsToExport = eventsList.toList()
        createCalendarDocument.launch("cellfree-calendario.ics")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_IMPORT_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                importCalendarFromFile(uri)
            }
        }
    }

    private fun importCalendarFromFile(uri: Uri) {
        Toast.makeText(this, "Importando calendario desde archivo...", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_SELECTED_DATE = "selected_date"
        private const val REQUEST_CODE_IMPORT_FILE = 1001
    }
}