package com.example.cellfreeapp.data.repository

import android.content.Context
import com.example.cellfreeapp.data.api.ApiClient
import com.example.cellfreeapp.data.model.CalendarSync
import com.example.cellfreeapp.data.model.CalendarSyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarSyncRepository(context: Context) {

    private val preferences =
        context.getSharedPreferences("CellFreeAuth", Context.MODE_PRIVATE)

    suspend fun getCalendarSyncs(): Result<List<CalendarSync>> = withContext(Dispatchers.IO) {
        request { token ->
            val response = ApiClient.calendarSyncApiService.getCalendarSyncs(token)
            if (!response.isSuccessful) {
                failure(response.code(), response.message(), response.errorBody()?.string())
            } else {
                val body = response.body()
                if (body?.status != "success") {
                    Result.failure(Exception(body?.message ?: "No se pudieron cargar los calendarios"))
                } else {
                    Result.success(body.data?.sincronizaciones.orEmpty())
                }
            }
        }
    }

    suspend fun createCalendarSync(
        name: String,
        url: String,
        intervalMinutes: Int
    ): Result<CalendarSync> = withContext(Dispatchers.IO) {
        mutateCalendarSync {
            ApiClient.calendarSyncApiService.createCalendarSync(
                CalendarSyncRequest(name, url, intervalMinutes),
                it
            )
        }
    }

    suspend fun updateCalendarSync(
        id: Int,
        name: String,
        url: String,
        intervalMinutes: Int
    ): Result<CalendarSync> = withContext(Dispatchers.IO) {
        mutateCalendarSync {
            ApiClient.calendarSyncApiService.updateCalendarSync(
                id,
                CalendarSyncRequest(name, url, intervalMinutes),
                it
            )
        }
    }

    suspend fun syncCalendar(id: Int): Result<Int> = withContext(Dispatchers.IO) {
        request { token ->
            val response = ApiClient.calendarSyncApiService.syncCalendar(id, token)
            if (!response.isSuccessful) {
                failure(response.code(), response.message(), response.errorBody()?.string())
            } else {
                val body = response.body()
                if (body?.status == "success") {
                    Result.success(body.data?.eventosImportados ?: 0)
                } else {
                    Result.failure(Exception(body?.message ?: "Falló la sincronización del calendario"))
                }
            }
        }
    }

    suspend fun toggleCalendarSync(id: Int): Result<CalendarSync> = withContext(Dispatchers.IO) {
        request { token ->
            val response = ApiClient.calendarSyncApiService.toggleCalendarSync(id, token)
            if (!response.isSuccessful) {
                failure(response.code(), response.message(), response.errorBody()?.string())
            } else {
                val body = response.body()
                val sync = body?.data?.sincronizacion
                if (body?.status == "success" && sync != null) {
                    Result.success(sync)
                } else {
                    Result.failure(Exception(body?.message ?: "No se pudo cambiar el estado"))
                }
            }
        }
    }

    suspend fun deleteCalendarSync(id: Int, deleteImportedReminders: Boolean): Result<Boolean> =
        withContext(Dispatchers.IO) {
            request { token ->
                val response = ApiClient.calendarSyncApiService.deleteCalendarSync(
                    id,
                    deleteImportedReminders,
                    token
                )
                if (!response.isSuccessful) {
                    failure(response.code(), response.message(), response.errorBody()?.string())
                } else {
                    val body = response.body()
                    if (body?.status == "success") {
                        Result.success(true)
                    } else {
                        Result.failure(Exception(body?.message ?: "No se pudo eliminar el calendario"))
                    }
                }
            }
        }

    private suspend fun mutateCalendarSync(
        call: suspend (String) -> retrofit2.Response<com.example.cellfreeapp.data.model.CalendarSyncResponse>
    ): Result<CalendarSync> = request { token ->
        val response = call(token)
        if (!response.isSuccessful) {
            failure(response.code(), response.message(), response.errorBody()?.string())
        } else {
            val body = response.body()
            val sync = body?.data?.sincronizacion
            if (body?.status == "success" && sync != null) {
                Result.success(sync)
            } else {
                Result.failure(Exception(body?.message ?: "No se pudo guardar la sincronización"))
            }
        }
    }

    private suspend fun <T> request(
        call: suspend (String) -> Result<T>
    ): Result<T> {
        val accessToken = preferences.getString("access_token", null)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure(Exception("Sesión expirada. Inicia sesión nuevamente."))

        return try {
            call("Bearer $accessToken")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun <T> failure(code: Int, message: String, details: String?): Result<T> {
        val responseDetails = details?.takeIf { it.isNotBlank() } ?: message
        return Result.failure(Exception("Error HTTP $code: $responseDetails"))
    }
}
