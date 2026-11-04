package com.example.cellfreeapp.data.model

import com.google.gson.annotations.SerializedName

data class Reminder(
    @SerializedName("id")
    val id: Int? = null,
    
    @SerializedName("titulo")
    val titulo: String,
    
    @SerializedName("descripcion")
    val descripcion: String? = null,
    
    @SerializedName("fecha_hora")
    val fechaHora: String,
    
    @SerializedName("tipo")
    val tipo: String, // tarea, examen, estudio
    
    @SerializedName("completado")
    val completado: Boolean = false,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class ReminderRequest(
    @SerializedName("titulo")
    val titulo: String,
    
    @SerializedName("descripcion")
    val descripcion: String? = null,
    
    @SerializedName("fecha_hora")
    val fechaHora: String,
    
    @SerializedName("tipo")
    val tipo: String,
    
    @SerializedName("completado")
    val completado: Boolean = false
)

data class RemindersResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: RemindersData,
    
    @SerializedName("message")
    val message: String? = null
)

data class RemindersData(
    @SerializedName("recordatorios")
    val recordatorios: List<Reminder>? = null,
    
    @SerializedName("recordatorio")
    val recordatorio: Reminder? = null,
    
    @SerializedName("year")
    val year: Int? = null,
    
    @SerializedName("month")
    val month: Int? = null,
    
    @SerializedName("recordatorios_por_fecha")
    val recordatoriosPorFecha: Map<String, List<Reminder>>? = null
)

data class ReminderStats(
    @SerializedName("total_recordatorios")
    val totalRecordatorios: Int,
    
    @SerializedName("recordatorios_completados")
    val recordatoriosCompletados: Int,
    
    @SerializedName("recordatorios_pendientes")
    val recordatoriosPendientes: Int,
    
    @SerializedName("recordatorios_vencidos")
    val recordatoriosVencidos: Int,
    
    @SerializedName("recordatorios_hoy")
    val recordatoriosHoy: Int,
    
    @SerializedName("por_tipo")
    val porTipo: Map<String, Int>
)

data class ReminderStatsResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: ReminderStatsData
)

data class ReminderStatsData(
    @SerializedName("estadisticas")
    val estadisticas: ReminderStats
)

enum class ReminderType(val value: String, val displayName: String) {
    TAREA("tarea", "Tarea"),
    EXAMEN("examen", "Examen"),
    ESTUDIO("estudio", "Estudio")
}