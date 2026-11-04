package com.example.cellfreeapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.cellfreeapp.BuildConfig
import com.example.cellfreeapp.data.api.ApiClient

object ConfigHelper {
    
    private const val TAG = "CellFree_Config"
    
    /**
     * Show current API configuration (only in debug mode)
     */
    fun showApiConfiguration(context: Context) {
        if (BuildConfig.APP_DEBUG_MODE) {
            val config = ApiClient.getApiConfiguration()
            Log.d(TAG, config)
            
            Toast.makeText(
                context,
                "API: ${BuildConfig.API_BASE_URL}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Log configuration details
     */
    fun logConfiguration() {
        if (BuildConfig.APP_DEBUG_MODE) {
            Log.d(TAG, "=== CellFree App Configuration ===")
            Log.d(TAG, "Base URL: ${BuildConfig.API_BASE_URL}")
            Log.d(TAG, "Timeout: ${BuildConfig.API_TIMEOUT_SECONDS}s")
            Log.d(TAG, "Logging: ${BuildConfig.API_ENABLE_LOGGING}")
            Log.d(TAG, "Debug Mode: ${BuildConfig.APP_DEBUG_MODE}")
            Log.d(TAG, "App Version: ${BuildConfig.VERSION_NAME}")
            Log.d(TAG, "===================================")
        }
    }
    
    /**
     * Validate configuration
     */
    fun validateConfiguration(): Boolean {
        val baseUrl = BuildConfig.API_BASE_URL
        
        if (baseUrl.isBlank()) {
            Log.e(TAG, "ERROR: API_BASE_URL is not configured in local.properties")
            return false
        }
        
        if (!baseUrl.startsWith("http")) {
            Log.e(TAG, "ERROR: API_BASE_URL must start with http:// or https://")
            return false
        }
        
        if (!baseUrl.endsWith("/")) {
            Log.w(TAG, "WARNING: API_BASE_URL should end with /")
        }
        
        return true
    }
    
    /**
     * Get environment-specific configurations
     */
    fun getEnvironmentInfo(): String {
        return when {
            BuildConfig.API_BASE_URL.contains("10.0.2.2") -> "Android Emulator"
            BuildConfig.API_BASE_URL.contains("localhost") -> "Localhost"
            BuildConfig.API_BASE_URL.contains("192.168") -> "Local Network"
            BuildConfig.API_BASE_URL.contains("https") -> "Production/HTTPS"
            else -> "Custom Environment"
        }
    }
}