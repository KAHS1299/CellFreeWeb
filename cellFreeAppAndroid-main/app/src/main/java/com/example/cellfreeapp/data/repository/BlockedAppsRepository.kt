package com.example.cellfreeapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cellfreeapp.BuildConfig
import com.example.cellfreeapp.data.api.ApiClient
import com.example.cellfreeapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlockedAppsRepository(context: Context) {
    
    companion object {
        private const val TAG = "BlockedAppsRepository"
        private const val SHARED_PREFS_NAME = "CellFreeAuth"
        private const val ACCESS_TOKEN_KEY = "access_token"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    
    private fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }
    
    suspend fun getBlockedApps(categoria: String? = null, estado: Boolean? = null): Result<List<BlockedApp>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.blockedAppsApiService.getBlockedApps(
                    categoria = categoria,
                    estado = estado,
                    authorization = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val blockedAppsResponse = response.body()
                    if (blockedAppsResponse?.status == "success") {
                        val apps = blockedAppsResponse.data.aplicacionesBloqueadas ?: emptyList()
                        Log.d(TAG, "Apps bloqueadas obtenidas: ${apps.size}")
                        Result.success(apps)
                    } else {
                        Result.failure(Exception(blockedAppsResponse?.message ?: "Error desconocido"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al obtener apps bloqueadas: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun createBlockedApp(request: BlockedAppRequest): Result<BlockedApp> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                Log.d(TAG, "Token obtenido: ${if (token != null) "presente" else "no encontrado"}")
                
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                Log.d(TAG, "Creando app bloqueada: ${request.nombreApp}")
                
                val response = ApiClient.blockedAppsApiService.createBlockedApp(
                    request = request,
                    authorization = "Bearer $token"
                )
                
                Log.d(TAG, "Respuesta del servidor - Código: ${response.code()}")
                
                if (response.isSuccessful) {
                    val blockedAppResponse = response.body()
                    if (blockedAppResponse?.status == "success" && blockedAppResponse.data.aplicacionBloqueada != null) {
                        Log.d(TAG, "App bloqueada creada: ${blockedAppResponse.data.aplicacionBloqueada.nombreApp}")
                        Result.success(blockedAppResponse.data.aplicacionBloqueada)
                    } else {
                        Log.e(TAG, "Error en respuesta: ${blockedAppResponse?.message}")
                        Result.failure(Exception(blockedAppResponse?.message ?: "Error al crear aplicación bloqueada"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error del servidor: ${response.code()} - $errorBody")
                    Result.failure(Exception("Error del servidor: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al crear app bloqueada: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun updateBlockedApp(id: Int, request: BlockedAppRequest): Result<BlockedApp> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.blockedAppsApiService.updateBlockedApp(
                    id = id,
                    request = request,
                    authorization = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val blockedAppResponse = response.body()
                    if (blockedAppResponse?.status == "success" && blockedAppResponse.data.aplicacionBloqueada != null) {
                        Log.d(TAG, "App bloqueada actualizada: ${blockedAppResponse.data.aplicacionBloqueada.nombreApp}")
                        Result.success(blockedAppResponse.data.aplicacionBloqueada)
                    } else {
                        Result.failure(Exception(blockedAppResponse?.message ?: "Error al actualizar aplicación bloqueada"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al actualizar app bloqueada: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun deleteBlockedApp(id: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.blockedAppsApiService.deleteBlockedApp(
                    id = id,
                    authorization = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    Log.d(TAG, "App bloqueada eliminada con ID: $id")
                    Result.success(true)
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al eliminar app bloqueada: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun toggleAppStatus(id: Int): Result<BlockedApp> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.blockedAppsApiService.toggleAppStatus(
                    id = id,
                    authorization = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val blockedAppResponse = response.body()
                    if (blockedAppResponse?.status == "success" && blockedAppResponse.data.aplicacionBloqueada != null) {
                        Log.d(TAG, "Estado de app cambiado: ${blockedAppResponse.data.aplicacionBloqueada.nombreApp}")
                        Result.success(blockedAppResponse.data.aplicacionBloqueada)
                    } else {
                        Result.failure(Exception(blockedAppResponse?.message ?: "Error al cambiar estado"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al cambiar estado: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun getEnabledBlockedApps(): Result<List<BlockedApp>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.blockedAppsApiService.getBlockedApps(
                    estado = true,
                    authorization = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val blockedAppsResponse = response.body()
                    if (blockedAppsResponse?.status == "success") {
                        val apps = blockedAppsResponse.data.aplicacionesBloqueadas ?: emptyList()
                        Log.d(TAG, "Configuraciones habilitadas obtenidas: ${apps.size}")
                        Result.success(apps)
                    } else {
                        Result.failure(Exception(blockedAppsResponse?.message ?: "Error desconocido"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al obtener configuraciones habilitadas: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun getBlockStats(): Result<BlockStats> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(Exception("Token de acceso no encontrado"))
                }
                
                val response = ApiClient.blockedAppsApiService.getBlockStats(
                    authorization = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val statsResponse = response.body()
                    if (statsResponse?.status == "success") {
                        Log.d(TAG, "Estadísticas obtenidas")
                        Result.success(statsResponse.data.estadisticas)
                    } else {
                        Result.failure(Exception("Error al obtener estadísticas"))
                    }
                } else {
                    Result.failure(Exception("Error del servidor: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta."
                    else -> "Error de conexión: ${e.message}"
                }
                Log.e(TAG, "Error al obtener estadísticas: ${e.message}")
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
}