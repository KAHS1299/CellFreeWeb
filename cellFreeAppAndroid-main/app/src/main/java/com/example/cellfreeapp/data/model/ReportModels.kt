package com.example.cellfreeapp.data.model

import com.google.gson.annotations.SerializedName

// Modelo para registrar uso de aplicaciones
data class UsageRegistrationRequest(
    @SerializedName("nombre_app")
    val nombreApp: String,
    
    @SerializedName("categoria")
    val categoria: String,
    
    @SerializedName("tiempo_uso_minutos")
    val tiempoUsoMinutos: Int,
    
    @SerializedName("numero_aperturas")
    val numeroAperturas: Int
)

// Modelo para aplicación en reporte diario
data class DailyAppUsage(
    @SerializedName("nombre_app")
    val nombreApp: String,
    
    @SerializedName("categoria")
    val categoria: String,
    
    @SerializedName("tiempo_uso_minutos")
    val tiempoUsoMinutos: Int,
    
    @SerializedName("tiempo_uso_horas")
    val tiempoUsoHoras: Double,
    
    @SerializedName("numero_aperturas")
    val numeroAperturas: Int
)

// Modelo para aplicación en reportes agregados (semanal/mensual/personalizado)
data class AggregatedAppUsage(
    @SerializedName("nombre_app")
    val nombreApp: String,
    
    @SerializedName("categoria")
    val categoria: String,
    
    @SerializedName("tiempo_total_minutos")
    val tiempoTotalMinutos: Int,
    
    @SerializedName("tiempo_total_horas")
    val tiempoTotalHoras: Double,
    
    @SerializedName("total_aperturas")
    val totalAperturas: Int,
    
    @SerializedName("dias_uso")
    val diasUso: Int,
    
    @SerializedName("promedio_diario_minutos")
    val promedioDiarioMinutos: Double
)

// Reporte diario
data class DailyReportData(
    @SerializedName("fecha")
    val fecha: String,
    
    @SerializedName("tipo_reporte")
    val tipoReporte: String,
    
    @SerializedName("aplicaciones")
    val aplicaciones: List<DailyAppUsage>,
    
    @SerializedName("total_tiempo_minutos")
    val totalTiempoMinutos: Int,
    
    @SerializedName("total_tiempo_horas")
    val totalTiempoHoras: Double
)

// Reporte agregado (semanal/mensual/personalizado)
data class AggregatedReportData(
    @SerializedName("fecha_inicio")
    val fechaInicio: String,
    
    @SerializedName("fecha_fin")
    val fechaFin: String,
    
    @SerializedName("tipo_reporte")
    val tipoReporte: String,
    
    @SerializedName("aplicaciones")
    val aplicaciones: List<AggregatedAppUsage>,
    
    @SerializedName("total_tiempo_minutos")
    val totalTiempoMinutos: Int,
    
    @SerializedName("total_tiempo_horas")
    val totalTiempoHoras: Double
)

// Respuesta de reporte diario
data class DailyReportResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: DailyReportData,
    
    @SerializedName("message")
    val message: String? = null
)

// Respuesta de reporte agregado
data class AggregatedReportResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: AggregatedReportData,
    
    @SerializedName("message")
    val message: String? = null
)

// Respuesta de registro de uso
data class UsageRegistrationResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("message")
    val message: String? = null
)

// Modelo unificado para mostrar en la tabla
data class ReportTableItem(
    val nombreApp: String,
    val categoria: String,
    val diario: String,      // Tiempo diario formateado
    val semanal: String,     // Tiempo semanal formateado
    val mensual: String,     // Tiempo mensual formateado
    val personalizado: String? = null // Para filtros por fecha
)

// Enum para tipos de reporte
enum class ReportType(val value: String, val displayName: String) {
    DIARIO("diario", "Diario"),
    SEMANAL("semanal", "Semanal"),
    MENSUAL("mensual", "Mensual"),
    PERSONALIZADO("personalizado", "Personalizado")
}

// Resumen de tiempo para mostrar totales
data class TimeUsageSummary(
    val totalMinutos: Int,
    val totalHoras: Double,
    val totalAperturas: Int,
    val diasAnalizados: Int
)