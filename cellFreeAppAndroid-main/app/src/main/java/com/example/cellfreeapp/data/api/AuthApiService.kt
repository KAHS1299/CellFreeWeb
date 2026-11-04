package com.example.cellfreeapp.data.api

import com.example.cellfreeapp.data.models.AuthResponse
import com.example.cellfreeapp.data.models.LoginRequest
import com.example.cellfreeapp.data.models.RegisterRequest
import com.example.cellfreeapp.data.models.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @GET("auth/user")
    suspend fun getUser(@Header("Authorization") token: String): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<AuthResponse>
    
    @PUT("auth/update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
        @Header("Authorization") token: String
    ): Response<AuthResponse>
    
    @DELETE("auth/delete-account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): Response<AuthResponse>
}