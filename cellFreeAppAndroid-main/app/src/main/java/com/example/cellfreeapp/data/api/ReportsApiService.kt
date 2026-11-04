package com.example.cellfreeapp.data.api

import com.example.cellfreeapp.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ReportsApiService {
    
    // Gestión de Uso
    @POST("/api/reportes/registrar-uso")
    suspend fun registerUsage(
        @Header("Authorization") authorization: String,
        @Body request: UsageRegistrationRequest
    ): Response<UsageRegistrationResponse>
    
    // Consulta de Reportes
    @GET("/api/reportes/diario")
    suspend fun getDailyReport(
        @Header("Authorization") authorization: String,
        @Query("fecha") fecha: String? = null
    ): Response<DailyReportResponse>
    
    @GET("/api/reportes/semanal")
    suspend fun getWeeklyReport(
        @Header("Authorization") authorization: String,
        @Query("fecha_inicio") fechaInicio: String? = null
    ): Response<AggregatedReportResponse>
    
    @GET("/api/reportes/mensual")
    suspend fun getMonthlyReport(
        @Header("Authorization") authorization: String,
        @Query("fecha_inicio") fechaInicio: String? = null
    ): Response<AggregatedReportResponse>
    
    @GET("/api/reportes/personalizado")
    suspend fun getCustomReport(
        @Header("Authorization") authorization: String,
        @Query("fecha_inicio") fechaInicio: String,
        @Query("fecha_fin") fechaFin: String
    ): Response<AggregatedReportResponse>
    
    // Exportación
    @GET("/api/reportes/descargar")
    suspend fun downloadReport(
        @Header("Authorization") authorization: String,
        @Query("tipo") tipo: String,
        @Query("fecha_inicio") fechaInicio: String? = null,
        @Query("fecha_fin") fechaFin: String? = null
    ): Response<ResponseBody>
}