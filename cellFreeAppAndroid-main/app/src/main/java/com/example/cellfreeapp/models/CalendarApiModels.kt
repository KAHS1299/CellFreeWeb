package com.example.cellfreeapp.models

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val status: String,
    val message: String? = null,
    val data: T? = null
)

data class RemindersResponse(
    @SerializedName("recordatorios")
    val reminders: List<CalendarEvent>? = null
)

data class ReminderResponse(
    @SerializedName("recordatorio")
    val reminder: CalendarEvent
)

data class CreateReminderRequest(
    val titulo: String,
    val descripcion: String,
    @SerializedName("fecha_hora")
    val dateTime: String,
    val tipo: String,
    val completado: Boolean = false
)

data class UpdateReminderRequest(
    val titulo: String?,
    val descripcion: String?,
    @SerializedName("fecha_hora")
    val dateTime: String?,
    val tipo: String?,
    val completado: Boolean?
)