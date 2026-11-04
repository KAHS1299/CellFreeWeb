package com.example.cellfreeapp.data.api

import com.example.cellfreeapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface RemindersApiService {
    
    @GET("reminders")
    suspend fun getReminders(
        @Query("tipo") tipo: String? = null,
        @Query("completado") completado: Boolean? = null,
        @Query("fecha_desde") fechaDesde: String? = null,
        @Query("fecha_hasta") fechaHasta: String? = null,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @POST("reminders")
    suspend fun createReminder(
        @Body request: ReminderRequest,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @GET("reminders/{id}")
    suspend fun getReminder(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @PUT("reminders/{id}")
    suspend fun updateReminder(
        @Path("id") id: Int,
        @Body request: ReminderRequest,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @DELETE("reminders/{id}")
    suspend fun deleteReminder(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @PATCH("reminders/{id}/toggle-complete")
    suspend fun toggleReminderComplete(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @GET("reminders/today/list")
    suspend fun getTodayReminders(
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @GET("reminders/upcoming/list")
    suspend fun getUpcomingReminders(
        @Query("days") days: Int? = 7,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @GET("reminders/overdue/list")
    suspend fun getOverdueReminders(
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @GET("reminders/calendar/month")
    suspend fun getMonthCalendar(
        @Query("year") year: Int,
        @Query("month") month: Int,
        @Header("Authorization") authorization: String
    ): Response<RemindersResponse>
    
    @GET("reminders/stats/summary")
    suspend fun getReminderStats(
        @Header("Authorization") authorization: String
    ): Response<ReminderStatsResponse>
}