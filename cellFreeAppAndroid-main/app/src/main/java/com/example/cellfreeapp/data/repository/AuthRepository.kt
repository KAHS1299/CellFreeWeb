package com.example.cellfreeapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.cellfreeapp.BuildConfig
import com.example.cellfreeapp.data.api.ApiClient
import com.example.cellfreeapp.data.models.LoginRequest
import com.example.cellfreeapp.data.models.RegisterRequest
import com.example.cellfreeapp.data.models.User
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("CellFreeAuth", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(email, password)
                val response = ApiClient.authApiService.login(request)
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.status == "success" && authResponse.data != null) {
                        // Guardar token y datos de usuario
                        saveAuthData(authResponse.data.access_token, authResponse.data.user)
                        Result.success(authResponse.data.user)
                    } else {
                        Result.failure(Exception(authResponse?.message ?: "Error desconocido"))
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Credenciales incorrectas"
                        422 -> "Datos inválidos"
                        500 -> "Error del servidor"
                        else -> "Error de conexión (${response.code()})"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor en configuración."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta o el servidor no responde."
                    else -> "Error de conexión: ${e.message}"
                }
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(name, email, password, passwordConfirmation)
                val response = ApiClient.authApiService.register(request)
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.status == "success" && authResponse.data != null) {
                        // Guardar token y datos de usuario
                        saveAuthData(authResponse.data.access_token, authResponse.data.user)
                        Result.success(authResponse.data.user)
                    } else {
                        Result.failure(Exception(authResponse?.message ?: "Error desconocido"))
                    }
                } else {
                    Result.failure(Exception("Error de registro: ${response.code()}"))
                }
            } catch (e: Exception) {
                val friendlyMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> 
                        "Error de conexión. Verifica la URL del servidor en configuración."
                    e.message?.contains("timeout") == true -> 
                        "Tiempo de espera agotado (${BuildConfig.API_TIMEOUT_SECONDS}s). La conexión es muy lenta o el servidor no responde."
                    else -> "Error de conexión: ${e.message}"
                }
                Result.failure(Exception(friendlyMessage))
            }
        }
    }
    
    suspend fun logout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token != null) {
                    ApiClient.authApiService.logout("Bearer $token")
                    clearAuthData()
                    Result.success(true)
                } else {
                    clearAuthData()
                    Result.success(true)
                }
            } catch (e: Exception) {
                clearAuthData() // Limpiar datos locales aunque falle la llamada
                Result.success(true)
            }
        }
    }
    
    private fun saveAuthData(token: String, user: User) {
        android.util.Log.d("AuthRepository", "Saving token: ${if (token.isEmpty()) "EMPTY" else "TOKEN_PRESENT (length=${token.length})"}")
        with(sharedPreferences.edit()) {
            putString(KEY_ACCESS_TOKEN, token)
            putString(KEY_USER_DATA, gson.toJson(user))
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    private fun clearAuthData() {
        with(sharedPreferences.edit()) {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_DATA)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun getCurrentUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && 
               getAccessToken() != null
    }
}