package com.example.cellfreeapp.data.model

import com.google.gson.annotations.SerializedName

data class BlockedApp(
    @SerializedName("id")
    val id: Int? = null,
    
    @SerializedName("nombre_app")
    val nombreApp: String,
    
    @SerializedName("categoria")
    val categoria: String? = null,
    
    @SerializedName("tiempo_bloqueo")
    val tiempoBloqueo: Int? = null,

    @SerializedName("nombre_paquete")
    val nombrePaquete: String? = null,

    @SerializedName("fecha_inicio")
    val fechaInicio: String? = null,

    @SerializedName("fecha_fin")
    val fechaFin: String? = null,
    
    @SerializedName("estado")
    val estado: Boolean = true,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class BlockedAppsResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: BlockedAppsData,
    
    @SerializedName("message")
    val message: String? = null
)

data class BlockedAppsData(
    @SerializedName(
        value = "aplicaciones_bloqueadas",
        alternate = ["aplicaciones_activas"]
    )
    val aplicacionesBloqueadas: List<BlockedApp>? = null,
    
    @SerializedName("aplicacion_bloqueada")
    val aplicacionBloqueada: BlockedApp? = null
)

data class BlockedAppRequest(
    @SerializedName("nombre_app")
    val nombreApp: String,
    
    @SerializedName("categoria")
    val categoria: String? = null,
    
    @SerializedName("tiempo_bloqueo")
    val tiempoBloqueo: Int? = null,
    
    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("fecha_inicio")
    val fechaInicio: String? = null,

    @SerializedName("fecha_fin")
    val fechaFin: String? = null,

    @SerializedName("nombre_paquete")
    val nombrePaquete: String? = null
)

data class BlockStats(
    @SerializedName("total_aplicaciones")
    val totalAplicaciones: Int,
    
    @SerializedName("aplicaciones_activas")
    val aplicacionesActivas: Int,
    
    @SerializedName("aplicaciones_inactivas")
    val aplicacionesInactivas: Int,
    
    @SerializedName("categorias")
    val categorias: Map<String, Int>
)

data class BlockStatsResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("data")
    val data: BlockStatsData
)

data class BlockStatsData(
    @SerializedName("estadisticas")
    val estadisticas: BlockStats
)

// Para el modal de configuración
data class BlockConfiguration(
    val appName: String,
    val isAllApps: Boolean,
    val startDate: String? = null,
    val endDate: String? = null,
    val timeInMinutes: Int = 60,
    val category: String? = null
)

enum class AppCategory(val value: String, val displayName: String) {
    SOCIAL("social", "Social"),
    JUEGOS("juegos", "Juegos"),
    ENTRETENIMIENTO("entretenimiento", "Entretenimiento"),
    PRODUCTIVIDAD("productividad", "Productividad"),
    OTROS("otros", "Otros")
}