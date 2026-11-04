package com.example.cellfreeapp.ui.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import com.example.cellfreeapp.R
import com.example.cellfreeapp.models.CalendarEvent
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EventDialogFragment : DialogFragment() {

    interface EventDialogListener {
        fun onEventSaved(event: CalendarEvent)
        fun onEventUpdated(event: CalendarEvent)
        fun onEventDeleted(eventId: Int)
    }

    private var listener: EventDialogListener? = null
    private var existingEvent: CalendarEvent? = null
    private var selectedDate: Date = Date()
    
    // Views
    private lateinit var tvModalTitle: TextView
    private lateinit var etEventTitle: TextInputEditText
    private lateinit var etEventDescription: TextInputEditText
    private lateinit var chipTarea: Chip
    private lateinit var chipExamen: Chip
    private lateinit var chipEstudio: Chip
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    companion object {
        private const val KEY_SELECTED_DATE = "selected_date"
        private const val KEY_HAS_EVENT = "has_event"
        private const val KEY_EVENT_ID = "event_id"
        private const val KEY_EVENT_TITLE = "event_title"
        private const val KEY_EVENT_DESCRIPTION = "event_description"
        private const val KEY_EVENT_DATE_TIME = "event_date_time"
        private const val KEY_EVENT_TYPE = "event_type"
        private const val KEY_EVENT_COMPLETED = "event_completed"

        fun newInstance(date: Date): EventDialogFragment {
            return EventDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(KEY_SELECTED_DATE, date.time)
                }
            }
        }

        fun newInstance(event: CalendarEvent): EventDialogFragment {
            return EventDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(KEY_SELECTED_DATE, event.date.time)
                    putBoolean(KEY_HAS_EVENT, true)
                    event.id?.let { putInt(KEY_EVENT_ID, it) }
                    putString(KEY_EVENT_TITLE, event.title)
                    putString(KEY_EVENT_DESCRIPTION, event.description)
                    putString(KEY_EVENT_DATE_TIME, event.dateTime)
                    putString(KEY_EVENT_TYPE, event.type)
                    putBoolean(KEY_EVENT_COMPLETED, event.completed)
                }
            }
        }
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        listener = context as? EventDialogListener
            ?: throw IllegalStateException(
                "La actividad debe implementar EventDialogListener"
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val savedDate = savedInstanceState?.getLong(KEY_SELECTED_DATE)
            ?: arguments?.getLong(KEY_SELECTED_DATE)
            ?: System.currentTimeMillis()
        selectedDate = Date(savedDate)

        if (arguments?.getBoolean(KEY_HAS_EVENT) == true) {
            existingEvent = CalendarEvent(
                id = arguments?.takeIf { it.containsKey(KEY_EVENT_ID) }?.getInt(KEY_EVENT_ID),
                title = arguments?.getString(KEY_EVENT_TITLE).orEmpty(),
                description = arguments?.getString(KEY_EVENT_DESCRIPTION).orEmpty(),
                dateTime = arguments?.getString(KEY_EVENT_DATE_TIME)
                    ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(selectedDate),
                type = arguments?.getString(KEY_EVENT_TYPE).orEmpty(),
                completed = arguments?.getBoolean(KEY_EVENT_COMPLETED) ?: false,
                color = CalendarEvent.getTypeColor(arguments?.getString(KEY_EVENT_TYPE).orEmpty())
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupInitialData()
        setupClickListeners()
        updateDateTimeButtons()
    }

    private fun initViews(view: View) {
        tvModalTitle = view.findViewById(R.id.tv_modal_title)
        etEventTitle = view.findViewById(R.id.et_event_title)
        etEventDescription = view.findViewById(R.id.et_event_description)
        chipTarea = view.findViewById(R.id.chip_tarea)
        chipExamen = view.findViewById(R.id.chip_examen)
        chipEstudio = view.findViewById(R.id.chip_estudio)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        btnSelectTime = view.findViewById(R.id.btn_select_time)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSave = view.findViewById(R.id.btn_save)
    }

    private fun setupInitialData() {
        if (existingEvent != null) {
            // Modo edición
            tvModalTitle.text = "Editar Evento"
            btnSave.text = "Actualizar"
            
            etEventTitle.setText(existingEvent!!.title)
            etEventDescription.setText(existingEvent!!.description)
            
            // Seleccionar el tipo correcto
            when (existingEvent!!.type) {
                "tarea" -> chipTarea.isChecked = true
                "examen" -> chipExamen.isChecked = true
                "estudio" -> chipEstudio.isChecked = true
            }
        } else {
            // Modo creación
            tvModalTitle.text = "Agregar Evento"
            btnSave.text = "Guardar"
            
            // Seleccionar tarea por defecto
            chipTarea.isChecked = true
        }
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            saveEvent()
        }

        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        // Configurar chips para que solo uno pueda estar seleccionado
        chipTarea.setOnClickListener { selectChip(chipTarea) }
        chipExamen.setOnClickListener { selectChip(chipExamen) }
        chipEstudio.setOnClickListener { selectChip(chipEstudio) }
    }

    private fun selectChip(selectedChip: Chip) {
        chipTarea.isChecked = selectedChip == chipTarea
        chipExamen.isChecked = selectedChip == chipExamen
        chipEstudio.isChecked = selectedChip == chipEstudio
        
        // Actualizar colores
        updateChipColors()
    }

    private fun updateChipColors() {
        updateChipColor(chipTarea, R.color.calendar_event_type_task)
        updateChipColor(chipExamen, R.color.calendar_event_type_exam)
        updateChipColor(chipEstudio, R.color.calendar_event_type_study)
    }

    private fun updateChipColor(chip: Chip, @androidx.annotation.ColorRes colorRes: Int) {
        val color = if (chip.isChecked) colorRes else R.color.gray
        chip.chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), color)
        )
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                
                // Mantener la hora actual
                val currentCalendar = Calendar.getInstance()
                currentCalendar.time = selectedDate
                newCalendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                newCalendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                newCalendar.set(Calendar.SECOND, 0)
                newCalendar.set(Calendar.MILLISECOND, 0)
                
                selectedDate = newCalendar.time
                updateDateTimeButtons()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.time = selectedDate
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                newCalendar.set(Calendar.SECOND, 0)
                newCalendar.set(Calendar.MILLISECOND, 0)
                
                selectedDate = newCalendar.time
                updateDateTimeButtons()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24 horas
        ).show()
    }

    private fun updateDateTimeButtons() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        btnSelectDate.text = dateFormat.format(selectedDate)
        btnSelectTime.text = timeFormat.format(selectedDate)
    }

    private fun saveEvent() {
        val title = etEventTitle.text?.toString()?.trim() ?: ""
        val description = etEventDescription.text?.toString()?.trim() ?: ""
        
        if (title.isEmpty()) {
            etEventTitle.error = "El título es requerido"
            return
        }
        if (title.length > 100) {
            etEventTitle.error = "El título no puede superar 100 caracteres"
            return
        }

        val type = when {
            chipTarea.isChecked -> "tarea"
            chipExamen.isChecked -> "examen"
            chipEstudio.isChecked -> "estudio"
            else -> "estudio"
        }

        // Formatear fecha y hora para la API
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateTimeString = dateTimeFormat.format(selectedDate)

        if (existingEvent != null) {
            // Actualizar evento existente
            val updatedEvent = existingEvent!!.copy(
                title = title,
                description = description,
                dateTime = dateTimeString,
                type = type,
                color = CalendarEvent.getTypeColor(type)
            )
            listener?.onEventUpdated(updatedEvent)
        } else {
            // Crear nuevo evento
            val newEvent = CalendarEvent(
                title = title,
                description = description,
                dateTime = dateTimeString,
                type = type,
                color = CalendarEvent.getTypeColor(type)
            )
            listener?.onEventSaved(newEvent)
        }

        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val density = resources.displayMetrics.density
            val horizontalMargin = (32 * density).toInt()
            val maxWidth = (480 * density).toInt()
            val width = (resources.displayMetrics.widthPixels - horizontalMargin)
                .coerceAtMost(maxWidth)
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(KEY_SELECTED_DATE, selectedDate.time)
        super.onSaveInstanceState(outState)
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }
}