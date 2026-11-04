package com.example.cellfreeapp.data.api

import com.example.cellfreeapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // Get configuration from BuildConfig (loaded from local.properties)
    private val BASE_URL = BuildConfig.API_BASE_URL
    private val TIMEOUT_SECONDS = BuildConfig.API_TIMEOUT_SECONDS.toLong()
    private val ENABLE_LOGGING = BuildConfig.API_ENABLE_LOGGING
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (ENABLE_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .apply {
            if (ENABLE_LOGGING) {
                addInterceptor(loggingInterceptor)
            }
        }
        // Connection timeout: tiempo para establecer conexión
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Read timeout: tiempo para recibir datos del servidor
        .readTimeout(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS) // Más tiempo para leer respuestas
        // Write timeout: tiempo para enviar datos al servidor
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Call timeout: tiempo total para toda la llamada
        .callTimeout(TIMEOUT_SECONDS * 3, TimeUnit.SECONDS) // Tiempo total generoso
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val blockedAppsApiService: BlockedAppsApiService = retrofit.create(BlockedAppsApiService::class.java)
    val remindersApiService: RemindersApiService = retrofit.create(RemindersApiService::class.java)
    val calendarSyncApiService: CalendarSyncApiService = retrofit.create(CalendarSyncApiService::class.java)
    val reportsApiService: ReportsApiService = retrofit.create(ReportsApiService::class.java)
    
    // Utility method to get current configuration (for debugging)
    fun getApiConfiguration(): String {
        return """
            API Configuration:
            - Base URL: $BASE_URL
            - Connect Timeout: ${TIMEOUT_SECONDS}s
            - Read Timeout: ${TIMEOUT_SECONDS * 2}s
            - Write Timeout: ${TIMEOUT_SECONDS}s
            - Call Timeout: ${TIMEOUT_SECONDS * 3}s
            - Logging: $ENABLE_LOGGING
            - Debug Mode: ${BuildConfig.APP_DEBUG_MODE}
        """.trimIndent()
    }
}