package com.example.cellfreeapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cellfreeapp.data.api.ApiClient
import com.example.cellfreeapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("CellFreeAuth", Context.MODE_PRIVATE)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val TAG = "ReportsRepository"
    }
    
    private fun getAccessToken(): String? {
        return sharedPreferences.getString("access_token", null)
    }
    
    private fun getAuthToken(): String? {
        val token = getAccessToken()
        Log.d(TAG, "Retrieved token: ${if (token.isNullOrEmpty()) "EMPTY" else "TOKEN_PRESENT (${token.length} chars)"}")
        return if (token != null) "Bearer $token" else null
    }
    
    // Registrar uso de aplicación
    suspend fun registerUsage(
        nombreApp: String,
        categoria: String,
        tiempoUsoMinutos: Int,
        numeroAperturas: Int
    ): Result<UsageRegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val request = UsageRegistrationRequest(
                    nombreApp = nombreApp,
                    categoria = categoria,
                    tiempoUsoMinutos = tiempoUsoMinutos,
                    numeroAperturas = numeroAperturas
                )
                
                val response = ApiClient.reportsApiService.registerUsage(
                    authorization = token,
                    request = request
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Result.success(body)
                    } else {
                        Result.failure(Exception(body?.message ?: "Error al registrar uso"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al registrar uso: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Obtener reporte diario
    suspend fun getDailyReport(fecha: String? = null): Result<DailyReportData> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.reportsApiService.getDailyReport(
                    authorization = token,
                    fecha = fecha
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "Error al obtener reporte diario"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener reporte diario: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Obtener reporte semanal
    suspend fun getWeeklyReport(fechaInicio: String? = null): Result<AggregatedReportData> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.reportsApiService.getWeeklyReport(
                    authorization = token,
                    fechaInicio = fechaInicio
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "Error al obtener reporte semanal"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener reporte semanal: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Obtener reporte mensual
    suspend fun getMonthlyReport(fechaInicio: String? = null): Result<AggregatedReportData> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.reportsApiService.getMonthlyReport(
                    authorization = token,
                    fechaInicio = fechaInicio
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "Error al obtener reporte mensual"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener reporte mensual: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Obtener reporte personalizado
    suspend fun getCustomReport(fechaInicio: String, fechaFin: String): Result<AggregatedReportData> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.reportsApiService.getCustomReport(
                    authorization = token,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        Result.success(body.data)
                    } else {
                        Result.failure(Exception(body?.message ?: "Error al obtener reporte personalizado"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener reporte personalizado: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Descargar reporte CSV
    suspend fun downloadReport(
        tipo: String,
        fechaInicio: String? = null,
        fechaFin: String? = null,
        destinationFile: File
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.reportsApiService.downloadReport(
                    authorization = token,
                    tipo = tipo,
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Guardar el archivo CSV
                        val inputStream = body.byteStream()
                        val outputStream = FileOutputStream(destinationFile)
                        
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        Log.d(TAG, "Reporte descargado en: ${destinationFile.absolutePath}")
                        Result.success(destinationFile)
                    } else {
                        Result.failure(Exception("Respuesta vacía"))
                    }
                } else {
                    Result.failure(Exception("Error HTTP: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al descargar reporte: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Función auxiliar para obtener datos combinados (diario, semanal, mensual)
    suspend fun getCombinedReports(): Result<List<ReportTableItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val today = dateFormat.format(Date())
                
                // Obtener reportes en paralelo
                val dailyResult = getDailyReport(today)
                val weeklyResult = getWeeklyReport()
                val monthlyResult = getMonthlyReport()
                
                // Procesar resultados
                val dailyData = dailyResult.getOrNull()
                val weeklyData = weeklyResult.getOrNull()
                val monthlyData = monthlyResult.getOrNull()
                
                // Crear mapa de aplicaciones
                val appsMap = mutableMapOf<String, ReportTableItem>()
                
                // Procesar datos diarios
                dailyData?.aplicaciones?.forEach { app ->
                    appsMap[app.nombreApp] = ReportTableItem(
                        nombreApp = app.nombreApp,
                        categoria = app.categoria,
                        diario = formatTime(app.tiempoUsoMinutos),
                        semanal = "0",
                        mensual = "0"
                    )
                }
                
                // Procesar datos semanales
                weeklyData?.aplicaciones?.forEach { app ->
                    val existing = appsMap[app.nombreApp]
                    if (existing != null) {
                        appsMap[app.nombreApp] = existing.copy(
                            semanal = formatTime(app.tiempoTotalMinutos)
                        )
                    } else {
                        appsMap[app.nombreApp] = ReportTableItem(
                            nombreApp = app.nombreApp,
                            categoria = app.categoria,
                            diario = "0",
                            semanal = formatTime(app.tiempoTotalMinutos),
                            mensual = "0"
                        )
                    }
                }
                
                // Procesar datos mensuales
                monthlyData?.aplicaciones?.forEach { app ->
                    val existing = appsMap[app.nombreApp]
                    if (existing != null) {
                        appsMap[app.nombreApp] = existing.copy(
                            mensual = formatTime(app.tiempoTotalMinutos)
                        )
                    } else {
                        appsMap[app.nombreApp] = ReportTableItem(
                            nombreApp = app.nombreApp,
                            categoria = app.categoria,
                            diario = "0",
                            semanal = "0",
                            mensual = formatTime(app.tiempoTotalMinutos)
                        )
                    }
                }
                
                Result.success(appsMap.values.toList().sortedByDescending { 
                    parseTimeToMinutes(it.diario)
                })
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener reportes combinados: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    // Función auxiliar para formatear tiempo
    private fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins}m"
            else -> "0m"
        }
    }
    
    // Función auxiliar para parsear tiempo a minutos
    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val regex = """(\d+)h\s*(\d+)m|(\d+)m""".toRegex()
            val match = regex.find(timeStr)
            when {
                match?.groups?.get(1) != null -> {
                    val hours = match.groups[1]?.value?.toInt() ?: 0
                    val minutes = match.groups[2]?.value?.toInt() ?: 0
                    hours * 60 + minutes
                }
                match?.groups?.get(3) != null -> {
                    match.groups[3]?.value?.toInt() ?: 0
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}