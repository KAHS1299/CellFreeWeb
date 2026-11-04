package com.example.cellfreeapp.data.model

import com.google.gson.annotations.SerializedName

data class CalendarSync(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("url_calendario")
    val urlCalendario: String,
    @SerializedName("estado")
    val estado: String,
    @SerializedName("intervalo_sincronizacion")
    val intervaloSincronizacion: Int,
    @SerializedName("ultima_sincronizacion")
    val ultimaSincronizacion: String? = null,
    @SerializedName("ultimo_error")
    val ultimoError: String? = null
)

data class CalendarSyncRequest(
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("url_calendario")
    val urlCalendario: String,
    @SerializedName("intervalo_sincronizacion")
    val intervaloSincronizacion: Int
)

data class CalendarSyncResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: CalendarSyncData? = null
)

data class CalendarSyncData(
    @SerializedName("sincronizaciones")
    val sincronizaciones: List<CalendarSync>? = null,
    @SerializedName("sincronizacion")
    val sincronizacion: CalendarSync? = null,
    @SerializedName("eventos_importados")
    val eventosImportados: Int? = null,
    @SerializedName("recordatorios_eliminados")
    val recordatoriosEliminados: Int? = null
)
