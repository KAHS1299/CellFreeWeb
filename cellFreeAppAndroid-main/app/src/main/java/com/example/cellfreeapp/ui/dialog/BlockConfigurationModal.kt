package com.example.cellfreeapp.ui.dialog

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import com.example.cellfreeapp.R
import com.example.cellfreeapp.data.model.AppCategory
import com.example.cellfreeapp.data.model.BlockConfiguration
import com.example.cellfreeapp.databinding.ModalBlockConfigurationBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class BlockConfigurationModal(
    private val context: Context,
    private val appName: String,
    private val isAllApps: Boolean,
    private val onSave: (BlockConfiguration) -> Unit,
    private val onCancel: (() -> Unit)? = null,
    private val initialConfiguration: BlockConfiguration? = null
) {
    
    private lateinit var binding: ModalBlockConfigurationBinding
    private lateinit var dialog: BottomSheetDialog
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null
    
    fun show() {
        setupDialog()
        setupViews()
        setupClickListeners()
        dialog.setOnDismissListener {
            onCancel?.invoke()
        }
        
        dialog.setOnCancelListener {
            onCancel?.invoke()
        }
        
        dialog.show()
    }
    
    private fun setupDialog() {
        binding = ModalBlockConfigurationBinding.inflate(LayoutInflater.from(context))
        dialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
    }
    
    private fun setupViews() {
        // Configurar título
        binding.tvModalTitle.text = if (isAllApps) "Todas" else appName
        
        // Configurar categoría (solo visible para apps individuales)
        if (!isAllApps) {
            binding.tilCategory.visibility = android.view.View.VISIBLE
            setupCategorySpinner(initialConfiguration?.category)
        } else {
            binding.tilCategory.visibility = android.view.View.GONE
        }
        
        val today = Calendar.getInstance()
        selectedStartDate = initialConfiguration?.startDate
            ?.let(::parseDateCalendar)
            ?: (today.clone() as Calendar)
        binding.etStartDate.setText(displayDateFormat.format(selectedStartDate!!.time))
        
        val nextWeek = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 7)
        }
        selectedEndDate = initialConfiguration?.endDate
            ?.let(::parseDateCalendar)
            ?: nextWeek
        binding.etEndDate.setText(displayDateFormat.format(selectedEndDate!!.time))
        binding.etTimeUsage.setText(
            (initialConfiguration?.timeInMinutes ?: 60).toString()
        )
    }
    
    private fun setupCategorySpinner(initialCategory: String?) {
        val categories = AppCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
        
        val selectedCategory = AppCategory.values()
            .find { it.value == initialCategory }
            ?.displayName
            ?: categories.firstOrNull()
        if (selectedCategory != null) {
            binding.actvCategory.setText(selectedCategory, false)
        }
    }

    private fun parseDateCalendar(value: String): Calendar? {
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd"
        )
        val date = patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }.parse(value)
            }.getOrNull()
        } ?: value.takeIf { it.length >= 10 }?.let { datePrefix ->
            runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    isLenient = false
                }.parse(datePrefix.take(10))
            }.getOrNull()
        } ?: return null

        return Calendar.getInstance().apply { time = date }
    }
    
    private fun setupClickListeners() {
        // Botón cerrar
        binding.btnCloseModal.setOnClickListener {
            dialog.dismiss()
        }
        
        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Botón guardar
        binding.btnSave.setOnClickListener {
            saveConfiguration()
        }
        
        // Campo fecha de inicio
        binding.etStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        // Campo fecha de fin
        binding.etEndDate.setOnClickListener {
            showDatePicker(false)
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) selectedStartDate ?: Calendar.getInstance() else selectedEndDate ?: Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                
                if (isStartDate) {
                    selectedStartDate = selectedCalendar
                    binding.etStartDate.setText(displayDateFormat.format(selectedCalendar.time))
                    
                    // Si la fecha de inicio es después de la fecha de fin, actualizar fecha de fin
                    if (selectedEndDate != null && selectedCalendar.after(selectedEndDate)) {
                        val newEndDate = selectedCalendar.clone() as Calendar
                        newEndDate.add(Calendar.DAY_OF_MONTH, 1)
                        selectedEndDate = newEndDate
                        binding.etEndDate.setText(displayDateFormat.format(newEndDate.time))
                    }
                } else {
                    // Validar que la fecha de fin no sea anterior a la fecha de inicio
                    if (selectedStartDate != null && selectedCalendar.before(selectedStartDate)) {
                        // Mostrar error
                        binding.etEndDate.error = "La fecha de fin debe ser posterior a la fecha de inicio"
                        return@DatePickerDialog
                    }
                    
                    selectedEndDate = selectedCalendar
                    binding.etEndDate.setText(displayDateFormat.format(selectedCalendar.time))
                    binding.etEndDate.error = null
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Para fecha de inicio, no permitir fechas anteriores a hoy
        if (isStartDate) {
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        } else {
            // Para fecha de fin, no permitir fechas anteriores a la fecha de inicio
            selectedStartDate?.let { startDate ->
                datePickerDialog.datePicker.minDate = startDate.timeInMillis
            }
        }
        
        datePickerDialog.show()
    }
    
    private fun saveConfiguration() {
        // Validar campos
        if (!validateFields()) {
            return
        }
        
        val timeText = binding.etTimeUsage.text.toString()
        val timeInMinutes = timeText.toIntOrNull() ?: 60
        
        val category = if (!isAllApps) {
            val selectedCategoryName = binding.actvCategory.text.toString()
            AppCategory.values().find { it.displayName == selectedCategoryName }?.value
        } else {
            null
        }
        
        val configuration = BlockConfiguration(
            appName = appName,
            isAllApps = isAllApps,
            startDate = selectedStartDate?.let { 
                // Configurar hora de inicio a 00:00:00
                val startCalendar = it.clone() as Calendar
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                dateFormat.format(startCalendar.time) 
            },
            endDate = selectedEndDate?.let { 
                // Configurar hora de fin a 23:59:59
                val endCalendar = it.clone() as Calendar
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                dateFormat.format(endCalendar.time) 
            },
            timeInMinutes = timeInMinutes,
            category = category
        )
        
        onSave(configuration)
        dialog.dismiss()
    }
    
    private fun validateFields(): Boolean {
        var isValid = true
        
        // Validar tiempo de uso
        val timeText = binding.etTimeUsage.text.toString()
        if (timeText.isBlank()) {
            binding.tilTimeUsage.error = "Ingrese el tiempo de uso"
            isValid = false
        } else {
            val time = timeText.toIntOrNull()
            if (time == null || time <= 0 || time > 1440) { // Máximo 24 horas
                binding.tilTimeUsage.error = "Ingrese un tiempo válido (1-1440 minutos)"
                isValid = false
            } else {
                binding.tilTimeUsage.error = null
            }
        }
        
        // Validar fechas
        if (selectedStartDate == null) {
            binding.tilStartDate.error = "Seleccione fecha de inicio"
            isValid = false
        } else {
            binding.tilStartDate.error = null
        }
        
        if (selectedEndDate == null) {
            binding.tilEndDate.error = "Seleccione fecha de fin"
            isValid = false
        } else {
            binding.tilEndDate.error = null
        }
        
        // Validar que fecha de fin sea posterior a fecha de inicio
        if (selectedStartDate != null && selectedEndDate != null) {
            if (selectedEndDate!!.before(selectedStartDate) || selectedEndDate!!.equals(selectedStartDate)) {
                binding.tilEndDate.error = "La fecha de fin debe ser posterior a la fecha de inicio"
                isValid = false
            }
        }
        
        // Validar categoría para apps individuales
        if (!isAllApps && binding.actvCategory.text.isBlank()) {
            binding.tilCategory.error = "Seleccione una categoría"
            isValid = false
        } else {
            binding.tilCategory.error = null
        }
        
        return isValid
    }
}