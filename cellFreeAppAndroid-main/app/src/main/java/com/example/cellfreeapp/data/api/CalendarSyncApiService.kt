package com.example.cellfreeapp.data.api

import com.example.cellfreeapp.data.model.CalendarSyncRequest
import com.example.cellfreeapp.data.model.CalendarSyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarSyncApiService {

    @GET("calendar-sync")
    suspend fun getCalendarSyncs(
        @Header("Authorization") authorization: String
    ): Response<CalendarSyncResponse>

    @POST("calendar-sync")
    suspend fun createCalendarSync(
        @Body request: CalendarSyncRequest,
        @Header("Authorization") authorization: String
    ): Response<CalendarSyncResponse>

    @PUT("calendar-sync/{id}")
    suspend fun updateCalendarSync(
        @Path("id") id: Int,
        @Body request: CalendarSyncRequest,
        @Header("Authorization") authorization: String
    ): Response<CalendarSyncResponse>

    @DELETE("calendar-sync/{id}")
    suspend fun deleteCalendarSync(
        @Path("id") id: Int,
        @Query("eliminar_recordatorios") deleteImportedReminders: Boolean,
        @Header("Authorization") authorization: String
    ): Response<CalendarSyncResponse>

    @POST("calendar-sync/{id}/sync")
    suspend fun syncCalendar(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<CalendarSyncResponse>

    @PATCH("calendar-sync/{id}/toggle")
    suspend fun toggleCalendarSync(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<CalendarSyncResponse>
}
