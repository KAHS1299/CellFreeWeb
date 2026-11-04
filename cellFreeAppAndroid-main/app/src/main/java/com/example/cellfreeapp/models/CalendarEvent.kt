package com.example.cellfreeapp.models

import androidx.annotation.ColorRes
import com.example.cellfreeapp.R
import com.google.gson.annotations.SerializedName
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalendarEvent(
    val id: Int? = null,
    @SerializedName("titulo")
    val title: String,
    @SerializedName("descripcion")
    val description: String,
    @SerializedName("fecha_hora")
    val dateTime: String, // Formato: "YYYY-MM-DD HH:MM:SS"
    @SerializedName("tipo")
    val type: String, // tarea, examen, estudio
    @SerializedName("completado")
    val completed: Boolean = false,
    @SerializedName("es_sincronizado")
    val isSynchronized: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    // Campo de color de UI: referencia a un recurso de color.
    @ColorRes
    val color: Int = R.color.calendar_event_type_study
) {
    // Propiedades calculadas para compatibilidad con la UI existente
    val date: Date
        get() = parseDateTime(dateTime)
    
    val startTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    
    val endTime: String
        get() = startTime // Por ahora usar la misma hora, se puede calcular después

    @ColorRes
    fun getStatusColor(now: Date = Date()): Int = when {
        completed -> R.color.calendar_event_completed
        date.before(now) -> R.color.calendar_event_overdue
        else -> R.color.calendar_event_pending
    }

    companion object {
        private fun parseDateTime(dateTimeStr: String): Date {
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX"
            )
            for (pattern in formats) {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }
                val position = ParsePosition(0)
                val parsed = formatter.parse(dateTimeStr, position)
                if (parsed != null && position.index == dateTimeStr.length) {
                    return parsed
                }
            }
            throw IllegalArgumentException("Formato de fecha del recordatorio inválido: $dateTimeStr")
        }
        
        @ColorRes
        fun getTypeColor(type: String): Int {
            return when (type) {
                "examen" -> R.color.calendar_event_type_exam
                "tarea" -> R.color.calendar_event_type_task
                "estudio" -> R.color.calendar_event_type_study
                else -> R.color.calendar_event_type_other
            }
        }
    }
}