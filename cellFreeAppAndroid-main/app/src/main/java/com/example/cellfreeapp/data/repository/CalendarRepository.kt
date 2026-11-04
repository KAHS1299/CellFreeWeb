package com.example.cellfreeapp.data.repository

import android.content.SharedPreferences
import com.example.cellfreeapp.data.api.RemindersApiService
import com.example.cellfreeapp.data.model.Reminder
import com.example.cellfreeapp.data.model.ReminderRequest
import com.example.cellfreeapp.models.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

class CalendarRepository(
    private val apiService: RemindersApiService,
    private val sharedPreferences: SharedPreferences
) {

    private fun legacyAuthHeader(): String {
        val token = sharedPreferences.getString("access_token", "") ?: ""
        android.util.Log.d("CalendarRepository", "Retrieved token: ${if (token.isEmpty()) "EMPTY" else "TOKEN_PRESENT (length=${token.length})"}")
        return "Bearer $token"
    }

    private fun getAuthToken(): String {
        val token = sharedPreferences.getString("access_token", null)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Sesión expirada. Inicia sesión nuevamente.")
        return "Bearer $token"
    }

    suspend fun getReminders(
        tipo: String? = null,
        completado: Boolean? = null,
        fechaDesde: String? = null,
        fechaHasta: String? = null
    ): Result<List<CalendarEvent>> {
        return try {
            val response = apiService.getReminders(
                tipo = tipo,
                completado = completado,
                fechaDesde = fechaDesde,
                fechaHasta = fechaHasta,
                authorization = getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status != "success") {
                    Result.failure(Exception(body?.message ?: "La API no devolvió los recordatorios"))
                } else {
                    val reminders = body.data.recordatorios ?: emptyList()
                    Result.success(reminders.map { reminder -> reminder.toCalendarEvent() })
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReminder(event: CalendarEvent): Result<CalendarEvent> {
        return try {
            val request = ReminderRequest(
                titulo = event.title,
                descripcion = event.description,
                fechaHora = event.dateTime,
                tipo = event.type,
                completado = event.completed
            )

            val response = apiService.createReminder(
                request = request,
                authorization = getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                val createdReminder = body?.data?.recordatorio
                if (body?.status == "success" && createdReminder != null) {
                    Result.success(createdReminder.toCalendarEvent())
                } else {
                    Result.failure(Exception(body?.message ?: "No se pudo crear el recordatorio"))
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReminder(event: CalendarEvent): Result<CalendarEvent> {
        return try {
            val request = ReminderRequest(
                titulo = event.title,
                descripcion = event.description,
                fechaHora = event.dateTime,
                tipo = event.type,
                completado = event.completed
            )

            val eventId = event.id
                ?: return Result.failure(Exception("El recordatorio no tiene un identificador válido"))
            val response = apiService.updateReminder(
                id = eventId,
                request = request,
                authorization = getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                val updatedReminder = body?.data?.recordatorio
                if (body?.status == "success" && updatedReminder != null) {
                    Result.success(updatedReminder.toCalendarEvent())
                } else {
                    Result.failure(Exception(body?.message ?: "No se pudo actualizar el recordatorio"))
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(eventId: Int): Result<Boolean> {
        return try {
            val response = apiService.deleteReminder(
                id = eventId,
                authorization = getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "success") {
                    Result.success(true)
                } else {
                    Result.failure(Exception(body?.message ?: "No se pudo eliminar el recordatorio"))
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReminderComplete(eventId: Int): Result<CalendarEvent> {
        return try {
            val response = apiService.toggleReminderComplete(
                id = eventId,
                authorization = getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                val toggledReminder = body?.data?.recordatorio
                if (body?.status == "success" && toggledReminder != null) {
                    Result.success(toggledReminder.toCalendarEvent())
                } else {
                    Result.failure(Exception(body?.message ?: "No se pudo cambiar el estado"))
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthCalendar(year: Int, month: Int): Result<List<CalendarEvent>> {
        return try {
            val response = apiService.getMonthCalendar(
                year = year,
                month = month,
                authorization = getAuthToken()
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status != "success") {
                    Result.failure(Exception(body?.message ?: "La API no devolvió los recordatorios del mes"))
                } else {
                    val reminders = body.data.recordatorios ?: emptyList()
                    Result.success(reminders.map { reminder -> reminder.toCalendarEvent() })
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTodayReminders(): Result<List<CalendarEvent>> {
        return try {
            val response = apiService.getTodayReminders(authorization = getAuthToken())

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status != "success") {
                    Result.failure(Exception(body?.message ?: "La API no devolvió los recordatorios de hoy"))
                } else {
                    val reminders = body.data.recordatorios ?: emptyList()
                    Result.success(reminders.map { reminder -> reminder.toCalendarEvent() })
                }
            } else {
                Result.failure(responseError(response.code(), response.message(), response.errorBody()?.string()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Función de extensión para convertir Reminder a CalendarEvent
    private fun Reminder.toCalendarEvent(): CalendarEvent {
        val event = CalendarEvent(
            id = this.id,
            title = this.titulo,
            description = this.descripcion ?: "",
            dateTime = this.fechaHora,
            type = this.tipo,
            completed = this.completado,
            color = CalendarEvent.getTypeColor(this.tipo),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
        event.date
        return event
    }

    private fun responseError(code: Int, message: String, details: String?): Exception {
        val description = details?.takeIf { it.isNotBlank() } ?: message
        return Exception("Error $code: $description")
    }
}